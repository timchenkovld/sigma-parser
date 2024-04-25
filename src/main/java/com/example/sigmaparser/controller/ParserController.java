package com.example.sigmaparser.controller;

import com.example.sigmaparser.parser.Parser;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ParserController {
    private final Parser parser;

    @ApiOperation(value = "parse", notes = "Initiates the parsing process")
    @GetMapping("/parse")
    public String parse() {
        parser.parse();

        return "Parsing initiated successfully!";
    }
}
