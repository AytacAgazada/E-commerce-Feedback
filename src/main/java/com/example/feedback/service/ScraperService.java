package com.example.feedback.service;

import com.example.feedback.entity.Product;
import com.example.feedback.exception.ScraperException;
import com.example.feedback.repository.ProductRepository;
import com.example.feedback.service.parse.ProductParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScraperService {

    private final List<ProductParser> scrapers;
    private final ProductRepository productRepository;

    public Product scrapeAndSave(String url) {
        ProductParser selectedParser = scrapers.stream()
                .filter(s -> s.supports(url))
                .findFirst()
                .orElseThrow(() -> new ScraperException("This site is not supported yet!"));

        Product product = selectedParser.parse(url);

        return productRepository.save(product);
    }
}