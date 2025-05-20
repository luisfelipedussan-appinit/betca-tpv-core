package es.upm.miw.betca_tpv_core.infrastructure.api.resources;


import es.upm.miw.betca_tpv_core.domain.model.Article;
import es.upm.miw.betca_tpv_core.domain.services.ArticleService;
import es.upm.miw.betca_tpv_core.infrastructure.api.Rest;
import es.upm.miw.betca_tpv_core.infrastructure.api.dtos.ArticleBarcodesDto;
import es.upm.miw.betca_tpv_core.infrastructure.api.dtos.ArticleCreationResponseDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Rest
@RequestMapping(ArticleResource.ARTICLES)
public class ArticleResource {
    public static final String ARTICLES = "/articles";

    public static final String PURCHASED = "/barcodes/purchased/without-complaints";
    public static final String BARCODE_ID = "/{barcode}";
    public static final String SEARCH = "/search";
    public static final String UNFINISHED = "/unfinished";
    public static final String BARCODE = "/barcode";
    public static final String SEARCH_PROVIDER_COMPANY = "/search-by-provider";
    public static final String ALL = "/all";

    private final ArticleService articleService;

    @Autowired
    public ArticleResource(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping(produces = {"application/json"})
    public Mono<ArticleCreationResponseDto> create(@Valid @RequestBody Article article) {
        article.doDefault();
        return this.articleService.create(article)
                .map(ArticleCreationResponseDto::fromArticle);
    }

    @PreAuthorize("permitAll()")
    @GetMapping(BARCODE_ID)
    public Mono<Article> read(@PathVariable String barcode) {
        return this.articleService.read(barcode);
    }

    @PutMapping(BARCODE_ID)
    public Mono<Article> update(@PathVariable String barcode, @Valid @RequestBody Article article) {
        article.doDefault();
        return this.articleService.update(barcode, article);
    }

    @GetMapping(SEARCH)
    public Flux<Article> findByBarcodeAndDescriptionAndReferenceAndStockLessThanAndDiscontinuedNullSafe(
            @RequestParam(required = false) String barcode, @RequestParam(required = false) String description, @
                    RequestParam(required = false) String reference, @RequestParam(required = false) Integer stock,
            @RequestParam(required = false) Boolean discontinued, @RequestParam(required = false) String tagId,
            @RequestParam(required = false) String tagIds, @RequestParam(required = false) String tagName,
            @RequestParam(required = false) String tagNames) {
        if (tagNames != null && !tagNames.isEmpty()) {
            List<String> tagNameList = Arrays.asList(tagNames.split(","));
            return this.articleService.findByTagNames(tagNameList)
                    .map(Article::ofBarcodeDescriptionStock);
        } else if (tagName != null && !tagName.isEmpty()) {
            return this.articleService.findByTagName(tagName)
                    .map(Article::ofBarcodeDescriptionStock);
        } else if (tagIds != null && !tagIds.isEmpty()) {
            List<String> tagIdList = Arrays.asList(tagIds.split(","));
            return Flux.fromIterable(tagIdList)
                    .flatMap(this.articleService::findByTag)
                    .map(Article::ofBarcodeDescriptionStock);
        } else if (tagId != null) {
            // If tagId is provided along with other filters, use the combined method
            if (barcode != null || description != null || reference != null || stock != null || discontinued != null) {
                return this.articleService.findByBarcodeAndDescriptionAndReferenceAndStockLessThanAndDiscontinuedAndTagNullSafe(
                        barcode, description, reference, stock, discontinued, tagId)
                        .map(Article::ofBarcodeDescriptionStock);
            } else {
                // If only tagId is provided, use the existing method
                return this.articleService.findByTag(tagId)
                        .map(Article::ofBarcodeDescriptionStock);
            }
        }
        return this.articleService.findByBarcodeAndDescriptionAndReferenceAndStockLessThanAndDiscontinuedNullSafe(
                        barcode, description, reference, stock, discontinued)
                .map(Article::ofBarcodeDescriptionStock);
    }
    @PreAuthorize("permitAll()")
    @GetMapping(BARCODE)
    public Mono<ArticleBarcodesDto> findByBarcodeNullSafe(@RequestParam(required = false) String barcode) {
        return this.articleService.findByBarcodeAndNotDiscontinuedNullSafe(barcode)
                .collectList()
                .map(ArticleBarcodesDto::new);
    }

    @GetMapping(UNFINISHED)
    public Flux<Article> findByUnfinished() {
        return this.articleService.findByUnfinished();
    }

    @GetMapping(SEARCH_PROVIDER_COMPANY)
    public Flux<Article> findByProviderCompany(@RequestParam String providerCompany) {
        return this.articleService.findByProviderCompany(providerCompany);
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping(value = PURCHASED)
    public Mono<ArticleBarcodesDto> findByBarcodeAndUserLoggedPurchasedArticlesWithoutComplaintsOpen(@RequestParam(required = false) String barcode,Authentication authentication){
        return this.articleService.findByBarcodeAndUserLoggedPurchasedArticlesWithoutComplaintsOpen(barcode,authentication.getPrincipal().toString())
                .collectList()
                .map(ArticleBarcodesDto::new);
    }

    @GetMapping(ALL)
    public Flux<Article> findAll() {
        return this.articleService.findAll();
    }
}
