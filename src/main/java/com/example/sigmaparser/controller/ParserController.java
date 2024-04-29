package com.example.sigmaparser.controller;

import com.example.sigmaparser.parser.Parser;
import com.example.sigmaparser.service.ConvertService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ParserController {
    private final Parser parser;
    private final ConvertService convertService;

    @ApiOperation(value = "parse", notes = "Initiates the parsing process")
    @GetMapping("/parse")
    public String parse() {
        parser.parse();
        return "Parsing initiated successfully!";
    }

    @ApiOperation(value = "convert", notes = "Initiates the conversion of data to exel")
    @GetMapping("/convert")
    public String convert() {
        convertService.convertToXLSX("products.xlsx");
        return "Conversion to exel has been successfully completed";
    }
}
