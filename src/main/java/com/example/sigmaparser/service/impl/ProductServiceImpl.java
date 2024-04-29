package com.example.sigmaparser.service.impl;

import com.example.sigmaparser.model.Product;
import com.example.sigmaparser.repository.ProductRepository;
import com.example.sigmaparser.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;

    @Override
    public Product createProduct(Product product) {
        productRepository.save(product);
        return product;
    }

    @Override
    public Product findProductByUrlAndName(String url, String name) {
        return productRepository.findByUrlAndName(url, name);
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}
