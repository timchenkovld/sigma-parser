package com.example.sigmaparser.service.impl;

import com.example.sigmaparser.model.Product;
import com.example.sigmaparser.model.ProductImage;
import com.example.sigmaparser.service.ConvertService;
import com.example.sigmaparser.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Service
public class ConvertServiceImpl implements ConvertService {
    private final ProductService productService;

    @Override
    public void convertToXLSX(String filePath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Products");

            createHeaderRow(sheet);
            fillData(sheet);

            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");
        headerRow.createCell(1).setCellValue("Price");
        headerRow.createCell(2).setCellValue("Description");
        headerRow.createCell(3).setCellValue("Available");
        headerRow.createCell(4).setCellValue("URL");
        headerRow.createCell(5).setCellValue("Image URLs");

        int columnWidthChars = 20;
        sheet.setColumnWidth(0, columnWidthChars * 256);
        sheet.setColumnWidth(2, columnWidthChars * 512);
        sheet.setColumnWidth(4, columnWidthChars * 512);
    }

    private void fillData(Sheet sheet) {
        int rowNum = 1;
        int batchSize = 500;
        int parts = 0;

        while (true) {
            List<Product> products = productService.getProductsByParts(parts, batchSize);
            if (products.isEmpty()) {
                break;
            }

            System.out.println("Received " + products.size() + " products on part " + parts);

            for (Product product : products) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(product.getName());
                row.createCell(1).setCellValue(product.getPrice());
                row.createCell(2).setCellValue(product.getDescription());
                row.createCell(3).setCellValue(product.isAvailable() ? "Available" : "Not Available");
                row.createCell(4).setCellValue(product.getUrl());

                String imageUrls = product.getImages()
                        .stream().limit(15)
                        .map(ProductImage::getImageUrl)
                        .collect(Collectors.joining(";"));
                row.createCell(5).setCellValue(imageUrls);
            }
            parts++;
        }
    }
}
