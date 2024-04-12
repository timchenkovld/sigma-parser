package com.example.sigmaparser.parser;

import com.example.sigmaparser.model.Link;
import com.example.sigmaparser.model.LinkType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class Parser {
    private static final String SIGMA_URL = "https://sigma.ua/";

    public List<Link> parseLinks() {
        List<Link> links = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(SIGMA_URL).get();
            Elements rootCategories = doc.select("ul.nav.navbar-nav.category-nav.fl_left>li");

            for (Element category : rootCategories) {
                Link link = new Link();

                Element getParentUrlElement = category.selectFirst("a");
                link.setName(getParentUrlElement.text());
                link.setUrl(getParentUrlElement.attr("abs:href"));
                link.setParent(null);
                link.setLinkType(LinkType.ROOT_CATEGORY);
                link.setChildLinks(new ArrayList<>());

                links.add(link);

                Elements subcategories = category.select("ul.nav.category-nav-open-drop>li");
                for (Element subcategory : subcategories) {
                    Link subLink = new Link();

                    link.getChildLinks().add(subLink);
                    subLink.setParent(link);
                    Element getUrlElement = subcategory.selectFirst("a");
                    subLink.setUrl(getUrlElement.attr("abs:href"));
                    subLink.setName(getUrlElement.text());
                    subLink.setChildLinks(new ArrayList<>());

                    Elements catalogs = subcategory.select("div.category-nav-open-drop-sub>a");
                    if (!catalogs.isEmpty()) {
                        subLink.setLinkType(LinkType.CATEGORY);

                        for (Element catalog : catalogs) {
                            Link productListLink = new Link();

                            subLink.getChildLinks().add(productListLink);
                            productListLink.setParent(subLink);
                            productListLink.setUrl(catalog.attr("abs:href"));
                            productListLink.setName(catalog.text());
                            productListLink.setLinkType(LinkType.PRODUCT_LIST);
                            productListLink.setChildLinks(new ArrayList<>());

                            parseProductCards(productListLink);

                        }
                    } else {
                        subLink.setLinkType(LinkType.PRODUCT_LIST);

                        parseProductCards(subLink);
                    }

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return links;
    }

    private void parseProductCards(Link link) {
        try {
            Document doc = Jsoup.connect(link.getUrl()).get();
            Elements productElements = doc.select("div.showcase.clearfix#showcaseview>div");

            for (Element productElement : productElements) {

                Elements productCards = productElement.select("div.thumbnail>div.caption");
                for (Element productCard : productCards) {
                    Link productCardLink = new Link();

                    link.getChildLinks().add(productCardLink);
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

