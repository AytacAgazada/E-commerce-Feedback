package com.example.feedback.service.parse;

import com.example.feedback.entity.Product;
import com.example.feedback.entity.Review;
import com.example.feedback.exception.ScraperException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class Birbank implements ProductParser {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/121.0.0.0 Safari/537.36";

    @Override
    public boolean supports(String url) {
        return url != null &&
                (url.contains("umico.az") || url.contains("birmarket.az"));
    }

    @Override
    public Product parse(String url) {

        try {
            Document doc = getDocument(url);

            Product product = new Product();
            product.setUrl(url);
            product.setSourceSite("BIRMARKET");

            String name = doc.select("h1").first() != null
                    ? doc.select("h1").first().text()
                    : "Unknown Product";

            String price = doc.select(".product-price").text();
            String description = doc.select(".product-description-content").text();

            product.setName(name);
            product.setPrice(price.isEmpty() ? "N/A" : price);
            product.setDescription(description.isEmpty() ? "No description available" : description);

            List<Review> reviews = new ArrayList<>();
            Elements reviewElements = doc.select(".review-item");

            for (Element el : reviewElements) {

                String author = el.select(".author-name").text();
                String content = el.select(".comment-text").text();

                if (!content.isEmpty()) {
                    Review review = new Review();
                    review.setAuthor(author.isEmpty() ? "Anonymous" : author);
                    review.setContent(content);
                    review.setProduct(product);
                    reviews.add(review);
                }
            }

            product.setReviews(reviews);

            return product;

        } catch (IOException e) {
            throw new ScraperException("Failed to retrieve Birbank data: " + e.getMessage());
        } catch (Exception e) {
            throw new ScraperException("Unexpected error while parsing Birbank page: " + e.getMessage());
        }
    }

    private Document getDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept-Language", "az-AZ,az;q=0.9,en-US;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .timeout(10000)
                .followRedirects(true)
                .get();
    }
}
