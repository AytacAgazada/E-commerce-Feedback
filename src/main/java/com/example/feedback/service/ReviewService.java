package com.example.feedback.service;

import com.example.feedback.dto.ProductResponse;
import com.example.feedback.dto.ReviewResponse;
import com.example.feedback.entity.Review;
import com.example.feedback.mapper.ProductMapper;
import com.example.feedback.repository.ProductRepository;
import com.example.feedback.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ResponseEntity<List<ReviewResponse>> getAllReviews(Long productId) {
        if (productId == null) {
            return ResponseEntity.badRequest().build();
        }

        List<ReviewResponse> reviews = reviewRepository.findByProduct_Id(productId);
        return ResponseEntity.ok(reviews);
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toDto)
                .toList();
    }

    public List<Map<String, Object>> findAllMinimal() {
        return productRepository.findAll()
                .stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", p.getId());
                    map.put("name", p.getName());
                    return map;
                })
                .collect(Collectors.toList());
    }
}
