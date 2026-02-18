package com.example.feedback.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name="Product")
public class Product {

    @Id
    private Long id;

    private String url;
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Double price;
    private String currency;
    private String sourceSite;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews;
}
