package es.upm.miw.betca_tpv_core.domain.services;

import es.upm.miw.betca_tpv_core.TestConfig;
import es.upm.miw.betca_tpv_core.domain.model.Article;
import es.upm.miw.betca_tpv_core.domain.persistence.TagPersistence;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import java.util.Objects;

@TestConfig
public class ArticleServiceIT {
    @Autowired
    private ArticleService articleService;

    @Autowired
    private TagPersistence tagPersistence;

    @Test
    void testFindByBarcodeAndUserLoggedPurchasedBarcodesWithoutComplaints(){
        StepVerifier.create(this.articleService.findByBarcodeAndUserLoggedPurchasedArticlesWithoutComplaintsOpen(null,"666666004"))
                .expectNextCount(3)
                .thenConsumeWhile(Objects::nonNull)
                .verifyComplete();
    }

    @Test
    void testFindByBarcodeAndUserLoggedPurchasedBarcodesWithoutComplaints_withBarcodeFilter(){
        StepVerifier.create(this.articleService.findByBarcodeAndUserLoggedPurchasedArticlesWithoutComplaintsOpen("8400000000031","666666004"))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void testFindByBarcodeAndUserLoggedPurchasedBarcodesWithoutComplaints_UserWithAllComplaintsCreated(){
        StepVerifier.create(this.articleService.findByBarcodeAndUserLoggedPurchasedArticlesWithoutComplaintsOpen(null,"66"))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void testFindByBarcodeAndUserLoggedPurchasedBarcodesWithoutComplaints_WithOutPurchasedItems(){
        StepVerifier.create(this.articleService.findByBarcodeAndUserLoggedPurchasedArticlesWithoutComplaintsOpen(null,"6600001"))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void testFindByTag() {
        String tagId = "682e44d3b0318f1981d47c44";

        // First, check if the tag exists in the test database
        StepVerifier.create(this.tagPersistence.readById(tagId))
                .expectError(es.upm.miw.betca_tpv_core.domain.exceptions.NotFoundException.class)
                .verify();

        System.out.println("[DEBUG_LOG] Tag with ID " + tagId + " does not exist in the test database");

        // Then, check if there are any articles with this tag
        StepVerifier.create(this.articleService.findByTag(tagId))
                .expectNextCount(0)
                .verifyComplete();
    }
}
