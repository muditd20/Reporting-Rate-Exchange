package com.wex.purchasetransaction.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.LocalDate;
import java.util.Optional;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Component
public class TreasuryExchangeClient {
    
    private final RestClient restClient;

    public TreasuryExchangeClient(@Value("${treasury.api.url}") String apiUrl) {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            HttpClient httpClient = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .build();

            this.restClient = RestClient.builder()
                    .baseUrl(apiUrl)
                    .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SSL context", e);
        }
    }

    public Optional<ExchangeRateData> getLatestExchangeRate(String targetCountry, LocalDate purchaseDate) {
        String filterParam = String.format("country:eq:%s,record_date:lte:%s", targetCountry, purchaseDate.toString());
        
        String uri = UriComponentsBuilder.fromPath("")
                .queryParam("filter", filterParam)
                .queryParam("sort", "-record_date")
                .queryParam("page[size]", "1")
                .build().toUriString();

        ExchangeRateResponse response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(ExchangeRateResponse.class);

        if (response != null && response.data() != null && !response.data().isEmpty()) {
            return Optional.of(response.data().get(0));
        }

        return Optional.empty();
    }
}
