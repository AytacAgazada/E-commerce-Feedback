package com.example.feedback.repository;

import com.example.feedback.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByUrl(String url);

    Optional<Product> findByUrl(String url);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.reviews WHERE p.id = :id")
    Optional<Product> findByIdWithReviews(@Param("id") Long id);
}