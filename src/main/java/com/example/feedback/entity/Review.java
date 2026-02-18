package com.example.feedback.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
@Table(name="Review")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String author;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Integer rating;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private String reviewDate;

}
