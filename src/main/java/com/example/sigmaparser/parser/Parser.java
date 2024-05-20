package com.example.sigmaparser.parser;

import com.example.sigmaparser.async.ProductParsingAction;
import com.example.sigmaparser.model.*;
import com.example.sigmaparser.service.CategoryService;
import com.example.sigmaparser.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import static com.example.sigmaparser.utils.ParserUtils.*;


@Slf4j
@Component
@RequiredArgsConstructor
public class Parser {
    private static final String SIGMA_URL = "https://sigma.ua/";
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final CategoryService categoryService;

    private final ProductService productService;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool(3);

    public void parse() {
        parseRootCategories();

        List<Category> categories = categoryService.getAllCategories().stream()
                .filter(category -> category.getParentCategory() != null).collect(Collectors.toList());

        ProductParsingAction mainAction = new ProductParsingAction(categories, this);
        forkJoinPool.invoke(mainAction);

        forkJoinPool.shutdown();
    }

    private void parseRootCategories() {
        try {
            Document doc = Jsoup.connect(SIGMA_URL).get();
            Elements rootCategories = doc.select("ul.nav.navbar-nav.category-nav.fl_left>li");

            if (ObjectUtils.isNotEmpty(rootCategories)) {
                execFor(rootCategories, this::parseCategory);
            }
        } catch (IOException e) {
            log.error("Error occurred while parsing categories URLs: {}", e.getMessage());
        }
    }

    private void parseCategory(Element category) {
        Link link = new Link();
        Element getParentUrlElement = category.selectFirst("a");
        link.setName(Objects.requireNonNull(getParentUrlElement).text());
        link.setUrl(getParentUrlElement.attr("abs:href"));
        link.setParent(null);
        link.setLinkType(LinkType.ROOT_CATEGORY);
        link.setChildLinks(new ArrayList<>());

        execFor(category.select("ul.nav.category-nav-open-drop>li"), (subcategory) -> {
            link.getChildLinks().add(parseSubcategory(subcategory, link));
        });
    }

    private Link parseSubcategory(Element subcategory, Link parentLink) {
        Link subLink = new Link();
        subLink.setParent(parentLink);
        Element getUrlElement = subcategory.selectFirst("a");
        subLink.setUrl(Objects.requireNonNull(getUrlElement).attr("abs:href"));
        subLink.setName(getUrlElement.text());
        subLink.setChildLinks(new ArrayList<>());

        Category parentCategory = categoryService.createCategory(null, subLink);

        Elements catalogs = subcategory.select("div.category-nav-open-drop-sub>a");
        if (!catalogs.isEmpty()) {
            subLink.setLinkType(LinkType.CATEGORY);
            execFor(catalogs, catalog -> {
                Link productListLink = parseProductList(catalog, subLink, parentCategory);
                subLink.getChildLinks().add(productListLink);
            });
        } else {
            subLink.setLinkType(LinkType.PRODUCT_LIST);
        }
        return subLink;
    }

    private Link parseProductList(Element catalog, Link parentLink, Category parentCategory) {
        Link productListLink = new Link();
        productListLink.setParent(parentLink);
        productListLink.setUrl(catalog.attr("abs:href"));
        productListLink.setName(catalog.text());
        productListLink.setLinkType(LinkType.PRODUCT_LIST);
        productListLink.setChildLinks(new ArrayList<>());

        categoryService.createCategory(parentCategory, productListLink);

        return productListLink;
    }

    public void parseProductCards(List<Category> categories) {
        Set<String> visitedUrls = new HashSet<>();

        execFor(categories, category -> {
            Set<String> unparsedUrls = new HashSet<>();

            String categoryUrl = category.getUrl();
            Set<String> productCardUrls = extractProductListUrls(categoryUrl, visitedUrls);

            Set<Product> products = new HashSet<>();
            execFor(productCardUrls, productCardUrl -> {
                Product product = extractProductData(productCardUrl, unparsedUrls);
                if (isNotEmpty(product)) {
                    products.add(product);
                }
            });

            Set<Product> uniqueProducts = productService.existAll(products);
            productService.saveAll(uniqueProducts);

            if (!unparsedUrls.isEmpty()) {
                saveUnparsedProducts(unparsedUrls);
            }
        });
    }

    private boolean isNotEmpty(Product product) {
        return product.getName() == null || product.getName().isEmpty() || product.getUrl() == null || product.getUrl().isEmpty() ? false : true;
    }

    private void saveUnparsedProducts(Set<String> unparsedUrls) {
        Set<Product> unparsedProducts = new HashSet<>();
        execFor(unparsedUrls, url -> {
            Product product = extractProductData(url, new HashSet<>());
            if (product != null) {
                unparsedProducts.add(product);
            }
        });

        productService.saveAll(unparsedProducts);
    }

    private Set<String> extractProductListUrls(String categoryUrl, Set<String> visitedUrls) {
        Set<String> productCardUrls = new HashSet<>();
        int retryAttempts = 0;
        boolean success = false;

        while (!success && retryAttempts < MAX_RETRY_ATTEMPTS) {
            try {
                Document doc = Jsoup.connect(categoryUrl).get();
                Elements productCardsLinks = doc.select("div.caption>div.name>a");
                execFor(productCardsLinks, element -> productCardUrls.add(element.attr("abs:href")));

                visitedUrls.add(categoryUrl);

                Element pagination = doc.selectFirst("div.pagination");
                if (pagination != null) {
                    Element nextLink = pagination.selectFirst("li.right>a");
                    if (nextLink != null) {
                        String nextPageUrl = nextLink.attr("abs:href");

                        if (!visitedUrls.contains(nextPageUrl) && !nextPageUrl.equals(categoryUrl)) {
                            productCardUrls.addAll(extractProductListUrls(nextPageUrl, visitedUrls));
                        }
                    }
                }

                success = true;
            } catch (HttpStatusException e) {
                if (e.getStatusCode() == 500) {
                    log.warn("Got HTTP 500 error, retrying ({}/{} - {})", retryAttempts + 1, MAX_RETRY_ATTEMPTS, e.getUrl());
                    retryAttempts++;
                }
            } catch (IOException e) {
                log.error("Error occurred while extracting product card URLs: {}", e.getMessage());
            }
        }

        return productCardUrls;
    }

    private Product extractProductData(String productCardUrl, Set<String> unparsedUrls) {
        Product product = new Product();
        Document docProductCardPage = null;
        try {
            log.info("Parsing product details from URL: {}", productCardUrl);
            docProductCardPage = Jsoup.connect(productCardUrl).get();

            Element nameElement = docProductCardPage.selectFirst("h1.detail-goods-title");
            String name = getElementText(nameElement);
            product.setName(name);

            Element priceElement = docProductCardPage.selectFirst("div.price-box.price_block>span.price");
            String price = getElementText(priceElement);
            product.setPrice(price);

            StringBuilder description = new StringBuilder();
            Elements descriptionElements = docProductCardPage.select("div.wr-detail-table-tab>div.detail-table-box");
            execFor(descriptionElements, element -> description.append(element.text()).append("\n"));
            product.setDescription(description.toString().trim());

            List<String> imageUrls = new ArrayList<>();
            Elements imageElements = docProductCardPage.select("div.swiper-wrapper.detail-goods-min-img-wrapper img");
            execFor(imageElements, element -> imageUrls.add(element.attr("abs:src")));

            List<ProductImage> productImages = new ArrayList<>();
            execFor(imageUrls, imageUrl -> {
                ProductImage productImage = new ProductImage();
                productImage.setImageUrl(imageUrl);
                productImage.setProduct(product);
                productImages.add(productImage);
            });
            product.setImages(productImages);

            product.setUrl(productCardUrl);

            Element statusElement = docProductCardPage.selectFirst("div.detail-goods-status");
            String status = getElementText(statusElement);
            product.setAvailable("В наявності".equalsIgnoreCase(status));

        } catch (HttpStatusException e) {
            unparsedUrls.add(e.getUrl());
            log.error("Error occurred while parsing product details from URL: {}. Status code: {}", productCardUrl, e.getStatusCode());
        } catch (IOException e) {
            log.error("Error occurred while parsing product details from URL: {}", productCardUrl);
        }

        return product;
    }

    private String getElementText(Element element) {
        return !Objects.isNull(element) && StringUtils.isNotBlank(element.text()) ? element.text() : "";
    }
}