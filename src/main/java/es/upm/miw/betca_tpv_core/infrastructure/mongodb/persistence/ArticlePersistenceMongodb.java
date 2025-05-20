package es.upm.miw.betca_tpv_core.infrastructure.mongodb.persistence;

import es.upm.miw.betca_tpv_core.domain.exceptions.ConflictException;
import es.upm.miw.betca_tpv_core.domain.exceptions.NotFoundException;
import es.upm.miw.betca_tpv_core.domain.model.Article;
import es.upm.miw.betca_tpv_core.domain.persistence.ArticlePersistence;
import es.upm.miw.betca_tpv_core.infrastructure.mongodb.daos.ArticleReactive;
import es.upm.miw.betca_tpv_core.infrastructure.mongodb.daos.ProviderReactive;
import es.upm.miw.betca_tpv_core.infrastructure.mongodb.daos.TagReactive;
import es.upm.miw.betca_tpv_core.infrastructure.mongodb.entities.ArticleEntity;
import es.upm.miw.betca_tpv_core.infrastructure.mongodb.entities.TagEntity;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public class ArticlePersistenceMongodb implements ArticlePersistence {

    private final ProviderReactive providerReactive;
    private final ArticleReactive articleReactive;
    private final TagReactive tagReactive;

    @Autowired
    public ArticlePersistenceMongodb(ProviderReactive providerReactive, ArticleReactive articleReactive, TagReactive tagReactive) {
        this.providerReactive = providerReactive;
        this.articleReactive = articleReactive;
        this.tagReactive = tagReactive;
    }

    @Override
    public Mono<Article> create(Article article) {
        return this.assertBarcodeNotExist(article.getBarcode())
                .then(Mono.justOrEmpty(article.getProviderCompany()))
                .flatMap(providerCompany -> this.providerReactive.findByCompany(article.getProviderCompany())
                        .switchIfEmpty(Mono.error(
                                new NotFoundException("Non existent company: " + article.getProviderCompany())
                        ))
                )
                .map(providerEntity -> new ArticleEntity(article, providerEntity))
                .switchIfEmpty(Mono.just(new ArticleEntity(article, null)))
                .flatMap(articleEntity -> {
                    if (article.getTag() != null) {
                        return this.tagReactive.findByName(article.getTag().getName())
                                .next()
                                .switchIfEmpty(this.tagReactive.save(new TagEntity(article.getTag())))
                                .map(tagEntity -> {
                                    articleEntity.setTag(tagEntity);
                                    return articleEntity;
                                });
                    } else {
                        return Mono.just(articleEntity);
                    }
                })
                .flatMap(this.articleReactive::save)
                .map(ArticleEntity::toArticle);
    }

    @Override
    public Mono<Article> readByBarcode(String barcode) {
        return this.articleReactive.findByBarcode(barcode)
                .switchIfEmpty(Mono.error(new NotFoundException("Non existent article barcode: " + barcode)))
                .map(ArticleEntity::toArticle);
    }

    @Override
    public Mono<Article> update(String barcode, Article article) {
        Mono<ArticleEntity> articleEntityMono;
        if (!barcode.equals(article.getBarcode())) {
            articleEntityMono = this.assertBarcodeNotExist(article.getBarcode())
                    .then(this.articleReactive.findByBarcode(barcode));
        } else {
            articleEntityMono = this.articleReactive.findByBarcode(barcode);
        }
        return articleEntityMono
                .switchIfEmpty(Mono
                        .error(new NotFoundException("Non existent article barcode: " + barcode)))
                .flatMap(articleEntity -> {
                    BeanUtils.copyProperties(article, articleEntity);
                    return this.providerReactive.findByCompany(article.getProviderCompany())
                            .switchIfEmpty(Mono.error(
                                    new NotFoundException("Non existent company: " + article.getProviderCompany()))
                            )
                            .map(providerEntity -> {
                                articleEntity.setProviderEntity(providerEntity);
                                return articleEntity;
                            });
                })
                .flatMap(articleEntity -> {
                    if (article.getTag() != null) {
                        return this.tagReactive.findByName(article.getTag().getName())
                                .next()
                                .switchIfEmpty(this.tagReactive.save(new TagEntity(article.getTag())))
                                .map(tagEntity -> {
                                    articleEntity.setTag(tagEntity);
                                    return articleEntity;
                                });
                    } else {
                        articleEntity.setTag(null);
                        return Mono.just(articleEntity);
                    }
                })
                .flatMap(this.articleReactive::save)
                .map(ArticleEntity::toArticle);
    }

    @Override
    public Flux<Article> findByAnyNullField() {
        return this.articleReactive.findByProviderEntityIsNull()
                .map(ArticleEntity::toArticle);
    }

    @Override
    public Flux<Article> findByBarcodeAndDescriptionAndReferenceAndStockLessThanAndDiscontinuedNullSafe(
            String barcode, String description, String reference, Integer stock, Boolean discontinued) {
        return this.articleReactive.findByBarcodeAndDescriptionAndReferenceAndStockLessThanAndDiscontinuedNullSafe(
                        barcode, description, reference, stock, discontinued)
                .map(ArticleEntity::toArticle);
    }

    @Override
    public Flux<Article> findByBarcodeAndDescriptionAndReferenceAndStockLessThanAndDiscontinuedAndTagNullSafe(
            String barcode, String description, String reference, Integer stock, Boolean discontinued, String tagId) {
        return this.articleReactive.findByBarcodeAndDescriptionAndReferenceAndStockLessThanAndDiscontinuedAndTagNullSafe(
                        barcode, description, reference, stock, discontinued, tagId)
                .map(ArticleEntity::toArticle);
    }

    @Override
    public Mono<Article> readAndWriteStockByBarcodeAssured(String barcode, Integer stockIncrement) {
        return this.articleReactive.findByBarcode(barcode)
                .switchIfEmpty(Mono.error(new NotFoundException("Article: " + barcode)))
                .flatMap(article -> {
                    article.setStock(article.getStock() + stockIncrement);
                    return this.articleReactive.save(article);
                }).map(ArticleEntity::toArticle);
    }

    @Override
    public Flux<String> findByBarcodeAndNotDiscontinuedNullField(String barcode) {
        return this.articleReactive.findByBarcodeLikeAndNotDiscontinuedNullSafe(barcode)
                .map(ArticleEntity::getBarcode);
    }

    private Mono<Void> assertBarcodeNotExist(String barcode) {
        return this.articleReactive.findByBarcode(barcode)
                .flatMap(articleEntity -> Mono.error(
                        new ConflictException("Article Barcode already exists : " + barcode)
                ));
    }

    @Override
    public Flux<Article> findByProviderCompany(String providerCompany) {
        return this.providerReactive.findByCompany(providerCompany)
                .switchIfEmpty(Mono.error(new NotFoundException("Non existent company: " + providerCompany)))
                .flatMapMany(providerEntity -> this.articleReactive.findByProviderEntityId(providerEntity.getId()))
                .map(ArticleEntity::toArticle);
    }

    @Override
    public Flux<Article> findByDiscontinuedIsFalse() {
        return this.articleReactive.findByDiscontinuedIsFalse()
                .map(ArticleEntity::toArticle);
    }

    @Override
    public Flux<Article> findByTag(String tagId) {
        return this.articleReactive.findByTag_IdAndDiscontinuedIsFalse(tagId)
                .map(ArticleEntity::toArticle);
    }

    @Override
    public Flux<Article> findAll() {
        return this.articleReactive.findAll()
                .map(ArticleEntity::toArticle);
    }
}
