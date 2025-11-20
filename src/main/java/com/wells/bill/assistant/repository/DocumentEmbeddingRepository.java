package com.wells.bill.assistant.repository;

import com.wells.bill.assistant.entity.DocumentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentEmbeddingRepository extends JpaRepository<DocumentEmbedding, Long> {
    @Query(value = "SELECT id, doc_id, source, chunk_text, metadata, embedding, created_at, "
            + "1 - (embedding <=> :queryEmbedding) as score "
            + "FROM document_embeddings "
            + "ORDER BY embedding <=> :queryEmbedding LIMIT :k", nativeQuery = true)
    List<Object[]> findNearest(@Param("queryEmbedding") float[] queryEmbedding, @Param("k") int k);

}