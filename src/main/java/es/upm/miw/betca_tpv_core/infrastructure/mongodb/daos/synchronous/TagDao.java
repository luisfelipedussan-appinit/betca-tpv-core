package es.upm.miw.betca_tpv_core.infrastructure.mongodb.daos.synchronous;

import es.upm.miw.betca_tpv_core.infrastructure.mongodb.entities.TagEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TagDao extends MongoRepository<TagEntity, String> {
    List<TagEntity> findByName(String name);
    List<TagEntity> findByGroup(String group);
}