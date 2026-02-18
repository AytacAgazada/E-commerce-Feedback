package com.example.feedback.service;

import com.example.feedback.dto.ReviewResponse;
import com.example.feedback.entity.Review;
import com.example.feedback.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    private final ReviewRepository reviewRepository;

    public ResponseEntity<List<ReviewResponse>> getAllReviews(Long productId) {
        if (productId == null) {
            return ResponseEntity.badRequest().build();
        }

        List<ReviewResponse> reviews = reviewRepository.findByProduct_Id(productId);
        return ResponseEntity.ok(reviews);
    }
}
