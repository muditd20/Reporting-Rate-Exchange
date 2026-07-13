package com.wex.purchasetransaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.wex.purchasetransaction.dto.PurchaseTransactionRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public class PurchaseControllerTest {

    @Container
    public static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private static WireMockServer wireMockServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("treasury.api.url", () -> wireMockServer.baseUrl());
    }

    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void teardownWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void testCreatePurchaseAndRetrieveConverted() throws Exception {
        // 1. Create Purchase
        PurchaseTransactionRequest request = new PurchaseTransactionRequest(
                "Test Item",
                LocalDate.of(2023, 10, 1),
                new BigDecimal("100.00")
        );

        String responseContent = mockMvc.perform(post("/api/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.description").value("Test Item"))
                .andReturn().getResponse().getContentAsString();

        // Extract ID
        Long id = objectMapper.readTree(responseContent).get("id").asLong();

        // 2. Setup WireMock for successful conversion
        String jsonResponse = """
                {
                    "data": [
                        {
                            "record_date": "2023-09-30",
                            "country": "Canada",
                            "currency": "Dollar",
                            "country_currency_desc": "Canada-Dollar",
                            "exchange_rate": "1.35",
                            "effective_date": "2023-09-30"
                        }
                    ]
                }
                """;

        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/"))
                .withQueryParam("filter", WireMock.equalTo("country:eq:Canada,record_date:lte:2023-10-01"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // 3. Retrieve Converted Purchase
        mockMvc.perform(get("/api/purchases/" + id)
                .param("targetCountry", "Canada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.originalUsdAmount").value(100.00))
                .andExpect(jsonPath("$.exchangeRate").value(1.35))
                .andExpect(jsonPath("$.convertedAmount").value(135.00));
    }

    @Test
    void testGetConvertedPurchaseRateTooOld() throws Exception {
        // 1. Create Purchase
        PurchaseTransactionRequest request = new PurchaseTransactionRequest(
                "Test Item Old Rate",
                LocalDate.of(2023, 10, 1),
                new BigDecimal("100.00")
        );

        String responseContent = mockMvc.perform(post("/api/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(responseContent).get("id").asLong();

        // 2. Setup WireMock for rate older than 6 months (e.g. 2023-03-01)
        String jsonResponse = """
                {
                    "data": [
                        {
                            "record_date": "2023-03-01",
                            "country": "Australia",
                            "currency": "Dollar",
                            "country_currency_desc": "Australia-Dollar",
                            "exchange_rate": "1.50",
                            "effective_date": "2023-03-01"
                        }
                    ]
                }
                """;

        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/"))
                .withQueryParam("filter", WireMock.equalTo("country:eq:Australia,record_date:lte:2023-10-01"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // 3. Retrieve Converted Purchase
        mockMvc.perform(get("/api/purchases/" + id)
                .param("targetCountry", "Australia"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
