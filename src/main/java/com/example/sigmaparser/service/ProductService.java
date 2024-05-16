package com.example.sigmaparser.service;

import com.example.sigmaparser.model.Product;

import java.util.List;
import java.util.Set;

public interface ProductService {
    Product createProduct(Product product);
    Product findProductByUrlAndName(String url, String name);
    List<Product> getAllProducts();
    List<Product> getProductsByParts(int page, int pageSize);
    void saveAll(Set<Product> products);
    Set<Product> existAll(Set<Product> products);
}
