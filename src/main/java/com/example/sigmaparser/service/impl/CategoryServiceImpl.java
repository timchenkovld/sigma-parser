package com.example.sigmaparser.service.impl;

import com.example.sigmaparser.model.Category;
import com.example.sigmaparser.model.Link;
import com.example.sigmaparser.repository.CategoryRepository;
import com.example.sigmaparser.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    public Category createCategory(Category category, Link link) {
        List<Category> existingCategories = categoryRepository
                //
                .findCategoryByName(link.getName());

        if (existingCategories.isEmpty()) {
            Category newCategory = new Category();
            newCategory.setName(link.getName());
            newCategory.setUrl(link.getUrl());
            newCategory.setParentCategory(category);

            categoryRepository.save(newCategory);

            return newCategory;
        } else {
            return existingCategories.get(0);
        }
    }

    @Override
    public Category updateCategory(long idCategory, Category updatedCategory) {
        Optional<Category> optionalCategory = categoryRepository.findById(idCategory);
        if (optionalCategory.isPresent()) {
            Category category = optionalCategory.get();
            category.setName(updatedCategory.getName());
            category.setUrl(updatedCategory.getUrl());
            category.setParentCategory(updatedCategory.getParentCategory());

            return categoryRepository.save(category);
        } else {
            return null;
        }
    }

    @Override
    public void deleteCategory(long idCategory) {
        categoryRepository.deleteById(idCategory);
    }

    @Override
    public Category findCategory(String name) {
//        List<Category> categories = categoryRepository.findCategoryByLink(name);
        List<Category> categories = categoryRepository.findCategoryByName(name);
        if (!categories.isEmpty()) {
            return categories.get(0);
        } else {
            return null;
        }
    }

    @Override
    public List<Category> getCategory() {
        return categoryRepository.findAll();
    }
}

