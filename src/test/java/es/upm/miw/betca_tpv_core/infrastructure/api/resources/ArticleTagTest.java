package es.upm.miw.betca_tpv_core.infrastructure.api.resources;

import es.upm.miw.betca_tpv_core.domain.model.Article;
import es.upm.miw.betca_tpv_core.domain.model.Provider;
import es.upm.miw.betca_tpv_core.domain.model.Tag;
import es.upm.miw.betca_tpv_core.infrastructure.api.RestClientTestService;
import es.upm.miw.betca_tpv_core.infrastructure.api.dtos.ArticleCreationResponseDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static es.upm.miw.betca_tpv_core.infrastructure.api.resources.ArticleResource.ARTICLES;
import static es.upm.miw.betca_tpv_core.infrastructure.api.resources.TagResource.TAGS;
import static org.junit.jupiter.api.Assertions.*;

@RestTestConfig
class ArticleTagTest {

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private RestClientTestService restClientTestService;

    @Test
    void testCreateArticleWithTag() {
        // First create a tag
        Tag tag = Tag.builder().name("TestTag").group("TestGroup").description("Test Description").build();
        Tag createdTag = this.restClientTestService.loginAdmin(webTestClient)
                .post()
                .uri(TAGS)
                .body(Mono.just(tag), Tag.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Tag.class)
                .value(Assertions::assertNotNull)
                .returnResult()
                .getResponseBody();
        
        assertNotNull(createdTag);
        assertNotNull(createdTag.getId());
        assertEquals("TestTag", createdTag.getName());
        
        // Create a provider if needed
        Provider provider = Provider.builder().company("tagTestProvider").nif("tagTestNif").phone("666666666").build();
        this.restClientTestService.loginAdmin(webTestClient)
                .post()
                .uri(ProviderResource.PROVIDERS)
                .body(Mono.just(provider), Provider.class)
                .exchange()
                .expectStatus().isOk();
        
        // Now create an article with the tag
        Article article = Article.builder()
                .barcode("tagTest12345")
                .description("Article with Tag")
                .retailPrice(BigDecimal.ONE)
                .providerCompany("tagTestProvider")
                .tag(createdTag)
                .build();
        
        // Test the response
        this.restClientTestService.loginAdmin(webTestClient)
                .post()
                .uri(ARTICLES)
                .body(Mono.just(article), Article.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ArticleCreationResponseDto.class)
                .value(Assertions::assertNotNull)
                .value(responseDto -> {
                    System.out.println("[DEBUG_LOG] Response DTO: " + responseDto);
                    assertEquals("tagTest12345", responseDto.getBarcode());
                    assertEquals("Article with Tag", responseDto.getDescription());
                    assertNotNull(responseDto.getRegistrationDate());
                    assertEquals("tagTestProvider", responseDto.getProviderCompany());
                    assertNotNull(responseDto.getTag());
                    assertEquals(createdTag.getId(), responseDto.getTag().getId());
                    assertEquals("TestTag", responseDto.getTag().getName());
                });
    }
}