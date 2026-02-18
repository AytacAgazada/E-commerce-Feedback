package com.example.feedback.service.parse;

import com.example.feedback.entity.Product;
import com.example.feedback.entity.Review;
import com.example.feedback.exception.ScraperException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class Birmarket implements ProductParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(String url) {
        return StringUtils.hasText(url) && (url.contains("umico.az") || url.contains("birmarket.az"));
    }

    @Override
    public Product parse(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(30000)
                    .get();

            Product product = new Product();
            product.setUrl(url);
            product.setSourceSite("BIRMARKET");
            product.setName(doc.select("h1").text().trim());

            String priceRaw = doc.select("span[data-info=item-desc-price-new]").text();
            String cleanedPrice = priceRaw.replace(",", ".").replaceAll("[^0-9.]", "").trim();
            product.setPrice(cleanedPrice.isEmpty() ? 0.0 : Double.parseDouble(cleanedPrice));
            product.setCurrency("AZN");

            String description = doc.select("div.MPProductDescriptionContentInfo").text();
            if (description.isEmpty()) {
                description = doc.select("div[data-info='product-info-description']").text();
            }
            description = description.replaceFirst("(?i)^Təsvir\\s*", "").trim();
            product.setDescription(description.isEmpty() ? "No description available" : description);

            product.setReviews(fetchReviews(url, doc, product));

            return product;
        } catch (Exception e) {
            log.error("Birmarket parsing error for URL {}: {}", url, e.getMessage());
            throw new ScraperException("Could not extract product data: " + e.getMessage());
        }
    }

    private List<Review> fetchReviews(String url, Document doc, Product product) {
        List<Review> reviews = new ArrayList<>();

        Elements reviewElements = doc.select("div[data-info='review-item'], div.MPProductReview");
        for (Element el : reviewElements) {
            Review review = new Review();
            String author = el.select("div[data-info='review-item-user'], div[class*='Author']").text().trim();
            String content = el.select("div[data-info='review-item-message']").text().trim();
            String date = el.select("div[data-info='review-item-date']").text().trim();

            int rating = el.select("svg.text-yellow-400, svg[class*='yellow']").size();

            if (StringUtils.hasText(content)) {
                review.setAuthor(author.isEmpty() ? "Anonymous" : author);
                review.setContent(content);
                review.setReviewDate(date.isEmpty() ? "Date unknown" : date);
                review.setRating(rating > 0 ? rating : 5);
                review.setProduct(product);
                reviews.add(review);
            }
        }

        if (reviews.isEmpty()) {
            reviews = fetchReviewsFromApi(url, product);
        }

        return reviews;
    }

    private List<Review> fetchReviewsFromApi(String productUrl, Product product) {
        List<Review> reviews = new ArrayList<>();
        try {
            Pattern pattern = Pattern.compile("product/(\\d+)");
            Matcher matcher = pattern.matcher(productUrl);

            if (matcher.find()) {
                String productId = matcher.group(1);
                String apiUrl = "https://api.umico.az/api/v1/products/" + productId + "/reviews";

                String jsonResponse = Jsoup.connect(apiUrl)
                        .ignoreContentType(true)
                        .userAgent("Mozilla/5.0")
                        .execute()
                        .body();

                JsonNode root = objectMapper.readTree(jsonResponse);
                JsonNode reviewsNode = root.path("reviews");

                if (reviewsNode.isArray()) {
                    for (JsonNode node : reviewsNode) {
                        Review review = new Review();
                        review.setAuthor(node.path("user").path("first_name").asText("Anonymous"));
                        review.setContent(node.path("comment").asText());
                        review.setRating(node.path("rating").asInt(5));
                        review.setReviewDate(node.path("created_at").asText("Date unknown"));
                        review.setProduct(product);

                        if (StringUtils.hasText(review.getContent())) {
                            reviews.add(review);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("API review fetch failed: {}", e.getMessage());
        }
        return reviews;
    }
}