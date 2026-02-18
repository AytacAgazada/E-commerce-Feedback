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

    private static final int API_LIMIT = 1000;

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

            String productIdStr = extractProductId(url);
            if (productIdStr != null) {
                product.setId(Long.parseLong(productIdStr));
            } else {
                throw new ScraperException("Product ID could not be extracted from URL");
            }

            product.setName(doc.select("h1").text().trim());

            product.setReviews(fetchAllReviewsFromApi(productIdStr, product));

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

            if (product.getReviews() == null || product.getReviews().isEmpty()) {
                product.setReviews(fetchReviewsFromHtml(doc, product));
            }

            return product;
        } catch (Exception e) {
            log.error("Birmarket parsing error for URL {}: {}", url, e.getMessage());
            throw new ScraperException("Could not extract product data: " + e.getMessage());
        }
    }

    private String extractProductId(String url) {
        Pattern pattern = Pattern.compile("/product/(\\d+)");
        Matcher matcher = pattern.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private List<Review> fetchAllReviewsFromApi(String productId, Product product) {
        List<Review> allReviews = new ArrayList<>();
        int offset = 0;
        int limit = 10;

        while(true){
            try {
                String apiUrl = "https://umico.az/assessment/api/v1/public/message"
                        + "?product_id=" + productId
                        + "&assessment_id=3"
                        + "&offset=" + offset
                        + "&limit=" + API_LIMIT
                        + "&sort_by=date"
                        + "&sort_type=desc";

                log.info("Fetching reviews from API: {}", apiUrl);

                String jsonResponse = Jsoup.connect(apiUrl)
                        .ignoreContentType(true)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .header("Accept", "application/json")
                        .timeout(15000)
                        .execute()
                        .body();

                JsonNode root = objectMapper.readTree(jsonResponse);

                JsonNode reviewsNode = root.has("messages") ? root.get("messages") : root;

                if (reviewsNode.isArray()) {
                    for (JsonNode node : reviewsNode) {
                        Review review = new Review();

                        String author = node.path("customer_name").asText("Anonymous");

                        String content = node.path("text").asText();
                        if (content.isEmpty()) content = node.path("comment").asText();

                        if (StringUtils.hasText(content)) {
                            review.setAuthor(author);
                            review.setContent(content);
                            review.setRating(node.path("score").asInt(5));
                            review.setReviewDate(node.path("created_at").asText("Date unknown"));
                            review.setProduct(product);
                            allReviews.add(review);
                        }
                    }
                    offset += reviewsNode.size();

                    if (reviewsNode.size() < limit) {
                        break;
                    }
                }else {break;}

                Thread.sleep(20);

            } catch (Exception e) {
                log.warn("API fetch failed for product {}: {}", productId, e.getMessage());
            }
        }

        log.info("Total reviews fetched from API for product {}: {}", productId, allReviews.size());
        return allReviews;
    }

    private List<Review> fetchReviewsFromHtml(Document doc, Product product) {
        List<Review> reviews = new ArrayList<>();
        Elements reviewElements = doc.select("div[data-info='review-item']");

        if (reviewElements.isEmpty()) {
            reviewElements = doc.select("div.MPProductReview > div");
        }

        for (Element el : reviewElements) {
            String author = el.select("div[data-info='review-item-user'], div[class*='Author']").text().trim();
            String content = el.select("div[data-info='review-item-message']").text().trim();
            String date = el.select("div[data-info='review-item-date']").text().trim();
            int rating = extractRatingFromHtml(el);

            if (StringUtils.hasText(content)) {
                Review review = new Review();
                review.setAuthor(author.isEmpty() ? "Anonymous" : author);
                review.setContent(content);
                review.setReviewDate(date.isEmpty() ? "Date unknown" : date);
                review.setRating(rating);
                review.setProduct(product);
                reviews.add(review);
            }
        }
        return reviews;
    }

    private int extractRatingFromHtml(Element reviewElement) {
        Elements starSvgs = reviewElement.select("svg.vue-star-rating-star");
        if (starSvgs.isEmpty()) return 5;

        int filledCount = 0;
        for (Element star : starSvgs) {
            Element firstStop = star.selectFirst("linearGradient stop:first-child");
            if (firstStop != null && "100%".equals(firstStop.attr("offset"))) {
                filledCount++;
            }
        }
        return filledCount > 0 ? filledCount : 5;
    }
}