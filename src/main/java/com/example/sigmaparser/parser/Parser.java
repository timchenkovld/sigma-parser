package com.example.sigmaparser.parser;

import com.example.sigmaparser.model.*;
import com.example.sigmaparser.service.CategoryService;
import com.example.sigmaparser.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.example.sigmaparser.utils.ParserUtils.*;


@Component
@RequiredArgsConstructor
public class Parser {
    private static final String SIGMA_URL = "https://sigma.ua/";

    private final CategoryService categoryService;

    private final ProductService productService;

    private final DataSource dataSource;
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    public void parse() {
        List<Product> existingProducts = productService.getAllProducts();

        parseCategories();

        List<Category> categories = categoryService.getAllCategories().stream()
                .filter(category -> category.getParentCategory() != null).collect(Collectors.toList());

        List<List<Category>> dividedCategories = divideList(categories, 3);

        getFor(dividedCategories, categoryChunk -> {
            executor.submit(() -> {
                try {
                    Thread.sleep(getRandomTimeout());
                    parseProductCards(categoryChunk, existingProducts);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        });
        executor.shutdown();
    }

    private Set<Link> parseCategories() {
        Set<Link> links = new HashSet<>();
        try {
            Document doc = Jsoup.connect(SIGMA_URL).get();
            Elements rootCategories = doc.select("ul.nav.navbar-nav.category-nav.fl_left>li");

            if (ObjectUtils.isNotEmpty(rootCategories)) {
                getFor(rootCategories, category -> {
                    Link link = parseCategory(category);
                    links.add(link);
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return links;
    }

    private Link parseCategory(Element category) {
        Link link = new Link();
        Element getParentUrlElement = category.selectFirst("a");
        link.setName(getParentUrlElement.text());
        link.setUrl(getParentUrlElement.attr("abs:href"));
        link.setParent(null);
        link.setLinkType(LinkType.ROOT_CATEGORY);
        link.setChildLinks(new ArrayList<>());

        getFor(category.select("ul.nav.category-nav-open-drop>li"), (subcategory) -> {
            link.getChildLinks().add(parseSubcategory(subcategory, link));
        });
        return link;
    }

    private Link parseSubcategory(Element subcategory, Link parentLink) {
        Link subLink = new Link();
        subLink.setParent(parentLink);
        Element getUrlElement = subcategory.selectFirst("a");
        subLink.setUrl(getUrlElement.attr("abs:href"));
        subLink.setName(getUrlElement.text());
        subLink.setChildLinks(new ArrayList<>());

        Category parentCategory = categoryService.createCategory(null, subLink);

        Elements catalogs = subcategory.select("div.category-nav-open-drop-sub>a");
        if (!catalogs.isEmpty()) {
            subLink.setLinkType(LinkType.CATEGORY);
            getFor(catalogs, catalog -> {
                Link productListLink = parseCatalog(catalog, subLink, parentCategory);
                subLink.getChildLinks().add(productListLink);
            });
        } else {
            subLink.setLinkType(LinkType.PRODUCT_LIST);
        }
        return subLink;
    }

    private Link parseCatalog(Element catalog, Link parentLink, Category parentCategory) {
        Link productListLink = new Link();
        productListLink.setParent(parentLink);
        productListLink.setUrl(catalog.attr("abs:href"));
        productListLink.setName(catalog.text());
        productListLink.setLinkType(LinkType.PRODUCT_LIST);
        productListLink.setChildLinks(new ArrayList<>());

        categoryService.createCategory(parentCategory, productListLink);

        return productListLink;
    }


    private void parseProductCards(List<Category> categories, List<Product> existingProducts) {
        Set<String> visitedUrls = new HashSet<>();
        getFor(categories, category -> {
            String categoryUrl = category.getUrl();
            try {
                List<String> productCardUrls = extractProductCardUrls(categoryUrl, visitedUrls);
                List<Product> products = new ArrayList<>();
                getFor(productCardUrls, productCardUrl -> {
                    try {
                        Product product = extractProductData(productCardUrl);

                        boolean isNewProduct = isNewProduct(product, existingProducts);
                        if (isNewProduct) {
                            products.add(product);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
//                productService.saveAll(products);
                batchInsertProducts(products);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private boolean isNewProduct(Product product, List<Product> existingProducts) {
        return !existingProducts.contains(product);
    }

    private void batchInsertProducts(List<Product> products) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement productStatement = connection.prepareStatement(
                    "INSERT INTO product (name, price, description, url, available) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                int batchSize = 40;
                int count = 0;

                for (Product product : products) {
                    productStatement.setString(1, product.getName());
                    productStatement.setString(2, product.getPrice());
                    productStatement.setString(3, product.getDescription());
                    productStatement.setString(4, product.getUrl());
                    productStatement.setBoolean(5, product.isAvailable());

                    productStatement.addBatch();

                    count++;
                    if (count % batchSize == 0 || count == products.size()) {
                        productStatement.executeBatch();

                        try (ResultSet generatedKeys = productStatement.getGeneratedKeys()) {
                            int index = 0;
                            while (generatedKeys.next()) {
                                long productId = generatedKeys.getLong(1);
                                List<ProductImage> images = products.get(index++).getImages();

                                try (PreparedStatement imageStatement = connection.prepareStatement(
                                        "INSERT INTO product_image (product_id, image_url) VALUES (?, ?)")) {
                                    for (ProductImage image : images) {
                                        imageStatement.setLong(1, productId);
                                        imageStatement.setString(2, image.getImageUrl());
                                        imageStatement.addBatch();
                                    }
                                    imageStatement.executeBatch();
                                }
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private List<String> extractProductCardUrls(String categoryUrl, Set<String> visitedUrls) throws IOException {
        List<String> productCardUrls = new ArrayList<>();
        Document doc = Jsoup.connect(categoryUrl).get();
        Elements productCardsLinks = doc.select("div.caption>div.name>a");
        getFor(productCardsLinks, element -> productCardUrls.add(element.attr("abs:href")));

        visitedUrls.add(categoryUrl);

        Element pagination = doc.selectFirst("div.pagination");
        if (pagination != null) {
            Element nextLink = pagination.selectFirst("li.right>a");
            if (nextLink != null) {
                String nextPageUrl = nextLink.attr("abs:href");

                if (!visitedUrls.contains(nextPageUrl) && !nextPageUrl.equals(categoryUrl)) {
                    productCardUrls.addAll(extractProductCardUrls(nextPageUrl, visitedUrls));
                }
            }
        }

        return productCardUrls;
    }

    private Product extractProductData(String productCardUrl) throws IOException {
        Document docProductCardPage = Jsoup.connect(productCardUrl).get();
        Product product = new Product();
        Element nameElement = docProductCardPage.selectFirst("h1.detail-goods-title");
        String name = getElementText(nameElement);
        product.setName(name);

        Element priceElement = docProductCardPage.selectFirst("div.price-box.price_block>span.price");
        String price = getElementText(priceElement);
        product.setPrice(price);

        StringBuilder description = new StringBuilder();
        Elements descriptionElements = docProductCardPage.select("div.wr-detail-table-tab>div.detail-table-box");
        getFor(descriptionElements, element -> description.append(element.text()).append("\n"));
        product.setDescription(description.toString().trim());

        List<String> imageUrls = new ArrayList<>();
        Elements imageElements = docProductCardPage.select("div.swiper-wrapper.detail-goods-min-img-wrapper img");
        getFor(imageElements, element -> imageUrls.add(element.attr("abs:src")));

        List<ProductImage> productImages = new ArrayList<>();
        getFor(imageUrls, imageUrl -> {
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

        return product;
    }

    private String getElementText(Element element) {
        return !Objects.isNull(element) && StringUtils.isNotBlank(element.text()) ? element.text() : "";
    }
}

