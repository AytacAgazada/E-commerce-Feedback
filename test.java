//package com.example.feedback.service.parse;
//
//
//
//import com.example.feedback.entity.Product;
//
//import com.example.feedback.entity.Review;
//
//import com.example.feedback.exception.ScraperException;
//
//import com.fasterxml.jackson.databind.JsonNode;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import lombok.extern.slf4j.Slf4j;
//
//import org.jsoup.Jsoup;
//
//import org.jsoup.nodes.Document;
//
//import org.jsoup.nodes.Element;
//
//import org.jsoup.select.Elements;
//
//import org.springframework.stereotype.Component;
//
//import org.springframework.util.StringUtils;
//
//
//
//import java.util.ArrayList;
//
//import java.util.List;
//
//import java.util.regex.Matcher;
//
//import java.util.regex.Pattern;
//
//
//
//@Slf4j
//
//@Component
//
//public class Birmarket implements ProductParser {
//
//
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//
//
//    private static final int Limit = 1000000000;
//
//
//
//    @Override
//
//    public boolean supports(String url) {
//
//        return StringUtils.hasText(url) && (url.contains("umico.az") || url.contains("birmarket.az"));
//
//    }
//
//
//
//    @Override
//
//    public Product parse(String url) {
//
//        try {
//
//            Document doc = Jsoup.connect(url)
//
//                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
//
//                    .timeout(30000)
//
//                    .get();
//
//
//
//            Product product = new Product();
//
//            product.setUrl(url);
//
//            product.setSourceSite("BIRMARKET");
//
//
//
//            String productIdStr = extractProductId(url);
//
//            if (productIdStr != null) {
//
//                product.setId(Long.parseLong(productIdStr));
//
//            } else {
//
//                throw new ScraperException("Product ID could not be extracted from URL");
//
//            }
//
//
//
//            product.setName(doc.select("h1").text().trim());
//
//
//
//            if (productIdStr != null) {
//
//                product.setReviews(fetchAllReviewsFromApi(productIdStr, product));
//
//            }
//
//
//
//            String priceRaw = doc.select("span[data-info=item-desc-price-new]").text();
//
//            String cleanedPrice = priceRaw.replace(",", ".").replaceAll("[^0-9.]", "").trim();
//
//            product.setPrice(cleanedPrice.isEmpty() ? 0.0 : Double.parseDouble(cleanedPrice));
//
//            product.setCurrency("AZN");
//
//
//
//            String description = doc.select("div.MPProductDescriptionContentInfo").text();
//
//            if (description.isEmpty()) {
//
//                description = doc.select("div[data-info='product-info-description']").text();
//
//            }
//
//            description = description.replaceFirst("(?i)^Təsvir\\s*", "").trim();
//
//            product.setDescription(description.isEmpty() ? "No description available" : description);
//
//
//
//            String productId = extractProductId(url);
//
//            if (productId != null) {
//
//                product.setReviews(fetchAllReviewsFromApi(productId, product));
//
//            }
//
//
//
//            if (product.getReviews() == null || product.getReviews().isEmpty()) {
//
//                product.setReviews(fetchReviewsFromHtml(doc, product));
//
//            }
//
//
//
//            return product;
//
//        } catch (Exception e) {
//
//            log.error("Birmarket parsing error for URL {}: {}", url, e.getMessage());
//
//            throw new ScraperException("Could not extract product data: " + e.getMessage());
//
//        }
//
//    }
//
//
//
//    private String extractProductId(String url) {
//
//        Pattern pattern = Pattern.compile("/product/(\\d+)");
//
//        Matcher matcher = pattern.matcher(url);
//
//        return matcher.find() ? matcher.group(1) : null;
//
//    }
//
//
//
//    private List<Review> fetchAllReviewsFromApi(String productId, Product product) {
//
//        List<Review> allReviews = new ArrayList<>();
//
//        int page = 1;
//
//
//
//        while (true) {
//
//            List<Review> pageReviews = fetchReviewsPage(productId, product, page);
//
//
//
//            if (pageReviews.isEmpty()) {
//
//                break;
//
//            }
//
//
//
//            allReviews.addAll(pageReviews);
//
//            log.info("Fetched page {}: {} reviews (total so far: {})", page, pageReviews.size(), allReviews.size());
//
//
//
//            if (pageReviews.size() < PAGE_SIZE) {
//
//                break;
//
//            }
//
//
//
//            page++;
//
//
//
//            try {
//
//                Thread.sleep(300);
//
//            } catch (InterruptedException e) {
//
//                Thread.currentThread().interrupt();
//
//                break;
//
//            }
//
//        }
//
//
//
//        log.info("Total reviews fetched from API for product {}: {}", productId, allReviews.size());
//
//        return allReviews;
//
//    }
//
//
//
//    private List<Review> fetchReviewsPage(String productId, Product product, int page) {
//
//        List<Review> reviews = new ArrayList<>();
//
//        try {
//
//            String apiUrl = "https://api.umico.az/api/v1/products/" + productId
//
//                    + "/reviews?page=" + page
//
//                    + "&per_page=" + PAGE_SIZE
//
//                    + "&sort=created_at&order=desc";
//
//
//
//            log.debug("Fetching reviews page {} from: {}", page, apiUrl);
//
//
//
//            String jsonResponse = Jsoup.connect(apiUrl)
//
//                    .ignoreContentType(true)
//
//                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
//
//                    .header("Accept", "application/json")
//
//                    .header("Referer", "https://birmarket.az/")
//
//                    .timeout(15000)
//
//                    .execute()
//
//                    .body();
//
//
//
//            JsonNode root = objectMapper.readTree(jsonResponse);
//
//
//
//            JsonNode reviewsNode = root.path("reviews");
//
//            if (reviewsNode.isMissingNode() || !reviewsNode.isArray()) {
//
//                reviewsNode = root.path("data");
//
//            }
//
//            if (reviewsNode.isMissingNode() || !reviewsNode.isArray()) {
//
//                reviewsNode = root;
//
//            }
//
//
//
//            if (reviewsNode.isArray()) {
//
//                for (JsonNode node : reviewsNode) {
//
//                    String content = node.path("comment").asText();
//
//                    if (content.isEmpty()) {
//
//                        content = node.path("body").asText();
//
//                    }
//
//                    if (content.isEmpty()) {
//
//                        content = node.path("text").asText();
//
//                    }
//
//
//
//                    if (StringUtils.hasText(content)) {
//
//                        Review review = new Review();
//
//
//
//                        String author = node.path("user").path("first_name").asText("");
//
//                        String lastName = node.path("user").path("last_name").asText("");
//
//                        if (!lastName.isEmpty()) {
//
//                            author = author + " " + lastName;
//
//                        }
//
//                        review.setAuthor(author.trim().isEmpty() ? "Anonymous" : author.trim());
//
//
//
//                        review.setContent(content);
//
//                        review.setRating(node.path("rating").asInt(5));
//
//                        review.setReviewDate(node.path("created_at").asText("Date unknown"));
//
//                        review.setProduct(product);
//
//                        reviews.add(review);
//
//                    }
//
//                }
//
//            }
//
//
//
//        } catch (Exception e) {
//
//            log.warn("API page {} fetch failed for product {}: {}", page, productId, e.getMessage());
//
//        }
//
//        return reviews;
//
//    }
//
//
//
//    private List<Review> fetchReviewsFromHtml(Document doc, Product product) {
//
//        List<Review> reviews = new ArrayList<>();
//
//
//
//        Elements reviewElements = doc.select("div[data-info='review-item']");
//
//
//
//        if (reviewElements.isEmpty()) {
//
//            reviewElements = doc.select("div.MPProductReview > div");
//
//        }
//
//
//
//        for (Element el : reviewElements) {
//
//            String author = el.select("div[data-info='review-item-user'], div[class*='Author']").text().trim();
//
//            String content = el.select("div[data-info='review-item-message']").text().trim();
//
//            String date = el.select("div[data-info='review-item-date']").text().trim();
//
//            int rating = extractRatingFromHtml(el);
//
//
//
//            if (StringUtils.hasText(content)) {
//
//                Review review = new Review();
//
//                review.setAuthor(author.isEmpty() ? "Anonymous" : author);
//
//                review.setContent(content);
//
//                review.setReviewDate(date.isEmpty() ? "Date unknown" : date);
//
//                review.setRating(rating);
//
//                review.setProduct(product);
//
//                reviews.add(review);
//
//            }
//
//        }
//
//
//
//        return reviews;
//
//    }
//
//
//
//
//
//    private int extractRatingFromHtml(Element reviewElement) {
//
//        Elements starSvgs = reviewElement.select("svg.vue-star-rating-star");
//
//
//
//        if (starSvgs.isEmpty()) {
//
//            return 5;
//
//        }
//
//
//
//        int total = Math.min(starSvgs.size(), 5);
//
//        int filledCount = 0;
//
//
//
//        for (int i = 0; i < total; i++) {
//
//            Element firstStop = starSvgs.get(i).selectFirst("linearGradient stop:first-child");
//
//            if (firstStop != null && "100%".equals(firstStop.attr("offset"))) {
//
//                filledCount++;
//
//            }
//
//        }
//
//
//
//        return filledCount > 0 ? filledCount : 5;
//
//    }