package com.example.sigmaparser.parser;

import com.example.sigmaparser.model.*;
import com.example.sigmaparser.service.CategoryService;
import com.example.sigmaparser.service.ProductService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class Parser {
    private static final String SIGMA_URL = "https://sigma.ua/";

    private final CategoryService categoryService;

    private final ProductService productService;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    @PostConstruct
    public void init() {
        parseCategories();

        List<Category> categories = categoryService.getAllCategories().stream()
                .filter(category -> category.getParentCategory() != null).collect(Collectors.toList());

        List<List<Category>> dividedCategories = divideList(categories, 3);

        getFor(dividedCategories, categoryChunk -> {
            executor.submit(() -> {
                try {
                    Thread.sleep(1000);
                    parseProductCards(categoryChunk);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    private <T> List<List<T>> divideList(List<T> list, int parts) {
        List<List<T>> dividedList = new ArrayList<>();
        int chunkSize = (int) Math.ceil((double) list.size() / parts);
        for (int i = 0; i < list.size(); i += chunkSize) {
            dividedList.add(new ArrayList<>(list.subList(i, Math.min(i + chunkSize, list.size()))));
        }
        return dividedList;
    }

    public Set<Link> parseCategories() {
        Set<Link> links = new HashSet<>();
        try {
            Document doc = Jsoup.connect(SIGMA_URL).get();
            Elements rootCategories = doc.select("ul.nav.navbar-nav.category-nav.fl_left>li");

            getFor(rootCategories, category -> {
                Link link = parseCategory(category);
                links.add(link);
            });
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

    public void parseProductCards(List<Category> categories) {
        getFor(categories, category -> {
            String categoryUrl = category.getUrl();
            try {
                List<String> productCardUrls = extractProductCardUrls(categoryUrl);
                getFor(productCardUrls, productCardUrl -> {
                    try {
                        Product product = extractProductData(productCardUrl);
                        productService.createProduct(product);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private List<String> extractProductCardUrls(String categoryUrl) throws IOException {
        List<String> productCardUrls = new ArrayList<>();
        Document doc = Jsoup.connect(categoryUrl).get();
        Elements productCardsLinks = doc.select("div.caption>div.name>a");
        getFor(productCardsLinks, element -> productCardUrls.add(element.attr("abs:href")));
        return productCardUrls;
    }

    private Product extractProductData(String productCardUrl) throws IOException {
        Document docProductCardPage = Jsoup.connect(productCardUrl).get();
        Product product = new Product();
        Element nameElement = docProductCardPage.selectFirst("h1.detail-goods-title");
        String name = nameElement != null ? nameElement.text() : null;
        product.setName(name);

        Element priceElement = docProductCardPage.selectFirst("div.price-box.price_block>span.price");
        String price = priceElement != null ? priceElement.text() : null;
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
        String status = statusElement != null ? statusElement.text() : null;
        product.setAvailable(status != null && status.equalsIgnoreCase("В наявності"));

        return product;
    }

    private <T> void getFor(List<T> list, Consumer<T> processor) {
        for (T el : list) {
            processor.accept(el);
        }
    }
}

