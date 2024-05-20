package com.example.sigmaparser.async;

import com.example.sigmaparser.model.Category;
import com.example.sigmaparser.parser.Parser;

import java.util.List;
import java.util.concurrent.RecursiveAction;

public class ProductParsingAction extends RecursiveAction {
    private final List<Category> categories;
    private final Parser parser;

    public ProductParsingAction(List<Category> categories, Parser parser) {
        this.categories = categories;
        this.parser = parser;
    }

    @Override
    protected void compute() {
        if (categories.size() > 1) {
            processSubcategories(categories);
        } else {
            parser.parseProductCards(categories);
        }
    }

    private void processSubcategories(List<Category> categories) {
        int middle = categories.size() / 2;
        List<Category> leftPart = categories.subList(0, middle);
        List<Category> rightPart = categories.subList(middle, categories.size());

        ProductParsingAction leftAction = new ProductParsingAction(leftPart, parser);
        ProductParsingAction rightAction = new ProductParsingAction(rightPart, parser);

        invokeAll(leftAction, rightAction);
    }
}
