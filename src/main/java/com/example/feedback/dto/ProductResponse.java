package com.example.feedback.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private Double price;
    private String currency;
    private String description;
    private String sourceSite;
    private List<ReviewResponse> reviews;
}
