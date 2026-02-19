package com.example.feedback.controller;

import com.example.feedback.dto.ProductResponse;
import com.example.feedback.dto.ReviewResponse;
import com.example.feedback.dto.ScrapeRequest;
import com.example.feedback.entity.Product;
import com.example.feedback.mapper.ProductMapper;
import com.example.feedback.service.ReviewService;
import com.example.feedback.service.ScraperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/scrapers")
@RequiredArgsConstructor
public class ScraperController {
    private final ScraperService scraperService;
    private final ProductMapper mapper;
    private final ReviewService reviewService;

    @PostMapping("/scrape_Product")
    public ResponseEntity<ProductResponse> sscrapeProduct(@Valid @RequestBody ScrapeRequest request) {

        Product product = scraperService.scrapeAndSave(request.getUrl());

        ProductResponse response = mapper.toDto(product);

        return ResponseEntity.ok(response);

    }


    @GetMapping("/product-reviews/{id}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByProductId(@PathVariable Long id) {
        return reviewService.getAllReviews(id);
    }


    @GetMapping("/product/all")
    public List<ProductResponse> getAllProducts(){
        return reviewService.findAll();
    }


    @GetMapping("/product/all-minimal")
    public List<Map<String, Object>> getAllProductsMinimal(){
        return reviewService.findAllMinimal();
    }
}
