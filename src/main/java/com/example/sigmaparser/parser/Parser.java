package com.example.sigmaparser.parser;

import com.example.sigmaparser.model.Category;
import com.example.sigmaparser.model.Link;
import com.example.sigmaparser.model.LinkType;
import com.example.sigmaparser.service.CategoryService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Component
@RequiredArgsConstructor
public class Parser {
    private static final String SIGMA_URL = "https://sigma.ua/";

    private final CategoryService categoryService;

    @PostConstruct
    public void init() {
        parseLinks();
    }

    public Set<Link> parseLinks() {
          Set<Link> links = new HashSet<>();
//        List<Link> links = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(SIGMA_URL).get();
            Elements rootCategories = doc.select("ul.nav.navbar-nav.category-nav.fl_left>li");

            for (Element category : rootCategories) {
                Link link = parseCategory(category);
                links.add(link);
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


        Elements subcategories = category.select("ul.nav.category-nav-open-drop>li");
        for (Element subcategory : subcategories) {
            Link subLink = parseSubcategory(subcategory, link);
            link.getChildLinks().add(subLink);
        }
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
            for (Element catalog : catalogs) {
                Link productListLink = parseCatalog(catalog, subLink, parentCategory);
                subLink.getChildLinks().add(productListLink);
            }
        } else {
            subLink.setLinkType(LinkType.PRODUCT_LIST);
            parseProductCards(subLink, parentCategory);
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

        parseProductCards(productListLink, parentCategory);

        categoryService.createCategory(parentCategory, productListLink);

        return productListLink;
    }

    private void parseProductCards(Link link, Category parentCategory) {
        try {
            Document doc = Jsoup.connect(link.getUrl()).get();
            Elements productElements = doc.select("div.showcase.clearfix#showcaseview>div");

            for (Element productElement : productElements) {
                Elements productCards = productElement.select("div.thumbnail>div.caption");
                for (Element productCard : productCards) {
                    Link productCardLink = new Link();
                    productCardLink.setParent(link);
                    Element getProductLinkElement = productCard.selectFirst("a");
                    productCardLink.setUrl(getProductLinkElement.attr("abs:href"));
                    productCardLink.setName(productCard.text());
                    productCardLink.setLinkType(LinkType.PRODUCT_CARD);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

