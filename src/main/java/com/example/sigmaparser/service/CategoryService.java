package com.example.sigmaparser.service;

import com.example.sigmaparser.model.Category;
import com.example.sigmaparser.model.Link;

import java.util.List;

public interface CategoryService {
    List<Category> getAllCategories();
    Category createCategory(Category category, Link link);
    Category updateCategory(long idCategory, Category category);
    void deleteCategory(long idCategory);
    Category findCategory(String url);
}
