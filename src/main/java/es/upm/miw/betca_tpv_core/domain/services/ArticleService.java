package es.upm.miw.betca_tpv_core.domain.services;

import es.upm.miw.betca_tpv_core.domain.model.Article;
import es.upm.miw.betca_tpv_core.domain.model.Shopping;
import es.upm.miw.betca_tpv_core.domain.model.Tag;
import es.upm.miw.betca_tpv_core.domain.persistence.ArticlePersistence;
import es.upm.miw.betca_tpv_core.domain.persistence.ComplaintPersistence;
import es.upm.miw.betca_tpv_core.domain.persistence.TagPersistence;
import es.upm.miw.betca_tpv_core.domain.persistence.TicketPersistence;
import es.upm.miw.betca_tpv_core.domain.model.ComplaintState;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArticleService {

    private final ArticlePersistence articlePersistence;

    private final TicketPersistence ticketPersistence;

    private final ComplaintPersistence complaintPersistence;

    private final TagPersistence tagPersistence;

    @Autowired
    public ArticleService(ArticlePersistence articlePersistence, TicketPersistence ticketPersistence, 
                         ComplaintPersistence complaintPersistence, TagPersistence tagPersistence) {
        this.articlePersistence = articlePersistence;
        this.ticketPersistence = ticketPersistence;
        this.complaintPersistence = complaintPersistence;
        this.tagPersistence = tagPersistence;
    }

    public Mono<Article> create(Article article) {
        article.setRegistrationDate(LocalDateTime.now());
        return this.articlePersistence.create(article);
    }

    public Mono<Article> read(String barcode) {
        return this.articlePersistence.readByBarcode(barcode);
    }

    public Mono<Article> update(String barcode, Article article) {
        return this.articlePersistence.readByBarcode(barcode)
                .map(dataArticle -> {
                    BeanUtils.copyProperties(article, dataArticle, "registrationDate");
                    return dataArticle;
                }).flatMap(dataArticle -> this.articlePersistence.update(barcode, dataArticle));
    }

    public Flux<Article> findByBarcodeAndDescriptionAndReferenceAndStockLessThanAndDiscontinuedNullSafe(
            String barcode, String description, String reference, Integer stock, Boolean discontinued) {
        return this.articlePersistence.findByBarcodeAndDescriptionAndReferenceAndStockLessThanAndDiscontinuedNullSafe(
                barcode, description, reference, stock, discontinued);
    }

    public Flux<Article> findByBarcodeAndDescriptionAndReferenceAndStockLessThanAndDiscontinuedAndTagNullSafe(
            String barcode, String description, String reference, Integer stock, Boolean discontinued, String tagId) {
        return this.articlePersistence.findByBarcodeAndDescriptionAndReferenceAndStockLessThanAndDiscontinuedAndTagNullSafe(
                barcode, description, reference, stock, discontinued, tagId);
    }

    public Flux<Article> findByUnfinished() {
        return this.articlePersistence.findByAnyNullField();
    }

    public Flux<String> findByBarcodeAndNotDiscontinuedNullSafe(String barcode) {
        return this.articlePersistence.findByBarcodeAndNotDiscontinuedNullField(barcode);
    }

    public Flux<Article> findByProviderCompany(String company) {
        return this.articlePersistence.findByProviderCompany(company);
    }

    public Flux<String> findByBarcodeAndUserLoggedPurchasedArticlesWithoutComplaintsOpen(String barcode,String userMobile){
        return this.ticketPersistence.findByUserMobile(userMobile)
                .flatMap(ticket -> Flux.fromIterable(ticket.getShoppingList()))
                .map(Shopping::getBarcode)
                .distinct()
                .filterWhen(barcode1 -> this.complaintPersistence.findByUserMobileAndBarcodeAndState(userMobile,barcode1.toString(), ComplaintState.OPEN)
                        .hasElement()
                        .map(hasComplaint -> Boolean.FALSE.equals(hasComplaint))
                )
                .filter(barcode2 -> barcode == null || barcode2.contains(barcode));
    }

    public Flux<Article> findByTag(String tagId) {
        return this.articlePersistence.findByTag(tagId);
    }

    public Flux<Article> findByTagName(String tagName) {
        return this.tagPersistence.findByName(tagName)
                .flatMap(tag -> this.articlePersistence.findByTag(tag.getId()));
    }

    public Flux<Article> findByTagNames(List<String> tagNames) {
        return Flux.fromIterable(tagNames)
                .flatMap(this.tagPersistence::findByName)
                .flatMap(tag -> this.articlePersistence.findByTag(tag.getId()));
    }

    public Flux<Article> findByTagGroup(String tagGroup) {
        return this.tagPersistence.findByGroup(tagGroup)
                .flatMap(tag -> this.articlePersistence.findByTag(tag.getId()));
    }

    public Flux<Article> findAll() {
        return this.articlePersistence.findAll();
    }
}
