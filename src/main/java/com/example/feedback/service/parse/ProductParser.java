package com.example.feedback.service.parse;

import com.example.feedback.entity.Product;

public interface ProductParser {
    boolean supports( String url);
    Product parse(String url);
}
