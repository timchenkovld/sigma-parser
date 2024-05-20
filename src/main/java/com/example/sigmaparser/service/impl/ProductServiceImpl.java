package com.example.sigmaparser.service.impl;

import com.example.sigmaparser.model.Product;
import com.example.sigmaparser.repository.ProductRepository;
import com.example.sigmaparser.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final DataSource dataSource;

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

    @Override
    public Set<Product> saveAll(Set<Product> products) {
        productRepository.saveAllAndFlush(products);
        return products;
    }

    @Override
    public Set<Product> existAll(Set<Product> products) {
        Set<Product> uniqueProducts = new HashSet<>();
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("select * from product p " +
                    "where p.url = ? and p.name = ?");

            for (Product product : products) {
                preparedStatement.setString(1, product.getUrl());
                preparedStatement.setString(2, product.getName());

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        uniqueProducts.add(product);
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return uniqueProducts;
    }
}
