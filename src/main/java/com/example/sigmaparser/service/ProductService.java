package com.example.sigmaparser.service;

import com.example.sigmaparser.model.Product;

import java.util.List;

public interface ProductService {
    Product createProduct(Product product);
    Product findProductByUrlAndName(String url, String name);
    List<Product> getAllProducts();
}
