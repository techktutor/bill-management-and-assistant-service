package com.wells.bill.assistant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Document embeddings backed by pgvector.
 * Column mapping for vector depends on your JDBC/pgvector integration.
 * Use columnDefinition = "vector(768)" or appropriate dimension.
 */
@Data
@Entity
@Table(name = "document_embeddings")
public class DocumentEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_id", nullable = false)
    private UUID docId;

    @Column(name = "source")
    private String source;

    @Column(name = "chunk_text", columnDefinition = "text")
    private String chunkText;

    /**
     * IMPORTANT: mapping float[] to pgvector depends on driver support.
     * If using the 'pgvector' JDBC support, columnDefinition = "vector(768)" works.
     */
    @Column(name = "embedding", columnDefinition = "vector(768)")
    private float[] embedding;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (docId == null) docId = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
