package es.upm.miw.betca_tpv_core.infrastructure.api.dtos;

import es.upm.miw.betca_tpv_core.domain.model.Article;
import es.upm.miw.betca_tpv_core.domain.model.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ArticleCreationResponseDto {
    private String barcode;
    private String description;
    private String registrationDate;
    private String providerCompany;
    private Tag tag;

    public static ArticleCreationResponseDto fromArticle(Article article) {
        return ArticleCreationResponseDto.builder()
                .barcode(article.getBarcode())
                .description(article.getDescription())
                .registrationDate(article.getRegistrationDate().toString())
                .providerCompany(article.getProviderCompany())
                .tag(article.getTag())
                .build();
    }
}