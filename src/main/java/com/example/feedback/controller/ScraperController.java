package com.example.feedback.controller;

import com.example.feedback.dto.ProductResponse;
import com.example.feedback.dto.ScrapeRequest;
import com.example.feedback.entity.Product;
import com.example.feedback.mapper.ProductMapper;
import com.example.feedback.service.ScraperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scrapers")
@RequiredArgsConstructor
public class ScraperController {
    private final ScraperService scraperService;
    private final ProductMapper mapper;

    @PostMapping("/scrape")
    public ResponseEntity<ProductResponse> sscrapeProduct(@Valid @RequestBody ScrapeRequest request) {

        Product product = scraperService.scrapeAndSave(request.getUrl());

        ProductResponse response = mapper.toDto(product);

        return ResponseEntity.ok(response);

    }
}
