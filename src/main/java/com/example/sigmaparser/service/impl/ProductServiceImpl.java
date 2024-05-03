package com.example.sigmaparser.service.impl;

import com.example.sigmaparser.model.Product;
import com.example.sigmaparser.repository.ProductRepository;
import com.example.sigmaparser.service.ProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    @Override
    public List<Product> getProductsByParts(int part, int batchSize) {
        Pageable pageable = PageRequest.of(part, batchSize);
        return productRepository.findAll(pageable).getContent();
    }

    @Transactional
    @Override
    public void saveAll(List<Product> products) {
        productRepository.saveAll(products);
    }
}
