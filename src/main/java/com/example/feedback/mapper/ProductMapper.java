package com.example.feedback.mapper;

import com.example.feedback.dto.ProductResponse;
import com.example.feedback.dto.ReviewResponse;
import com.example.feedback.entity.Product;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ProductMapper {
    public ProductResponse toDto(Product entity) {
        return ProductResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .price(entity.getPrice())
                .currency(entity.getCurrency())
                .description(entity.getDescription())
                .sourceSite(entity.getSourceSite())
                .reviews(entity.getReviews().stream()
                        .map(r -> new ReviewResponse(r.getAuthor(), r.getContent(), r.getRating(),r.getReviewDate()))
                        .collect(Collectors.toList()))
                .build();
    }
}
