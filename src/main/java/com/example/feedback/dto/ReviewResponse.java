package com.example.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewResponse {
    private String author;
    private String content;
    private Integer rating;
    private String reviewDate;
}
