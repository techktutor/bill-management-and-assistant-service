package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.DocumentEmbedding;
import com.wells.bill.assistant.repository.DocumentEmbeddingRepository;
import jakarta.transaction.Transactional;
import org.apache.tika.Tika;
import org.springframework.ai.vertexai.embedding.text.VertexAiTextEmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ETL service:
 * - extract text (Tika)
 * - chunk text (overlapping windows)
 * - embed each chunk (EmbeddingClient)
 * - persist chunk + embedding into Postgres+pgvector
 */
@Service
public class RagEtlService {

    private final VertexAiTextEmbeddingModel embeddingModel;
    private final DocumentEmbeddingRepository embeddingRepository;

    public RagEtlService(VertexAiTextEmbeddingModel embeddingModel, DocumentEmbeddingRepository embeddingRepository) {
        this.embeddingModel = embeddingModel;
        this.embeddingRepository = embeddingRepository;
    }

    /**
     * Ingest an uploaded file and create embeddings for chunks.
     *
     * @param file   uploaded file (pdf, csv, txt, html, etc)
     * @param source identifier (file name, bill id, tenant id...)
     */
    @Transactional
    public void ingestFile(MultipartFile file, String source) throws Exception {
        Tika tika = new Tika();
        try (InputStream is = file.getInputStream()) {
            String text = tika.parseToString(is);
            ingestText(text, source);
        }
    }

    /**
     * Ingest raw text: chunk -> embed -> persist
     *
     * @param text   raw text to index
     * @param source doc identifier (file name, bill id)
     */
    @Transactional
    public void ingestText(String text, String source) {
        if (text == null || text.isBlank()) return;

        List<String> chunks = chunkText(text);
        // batch embeddings if possible. Here we call embedding per chunk to keep code simple.
        // If embeddingClient supports batch embeddings, prefer that for speed and cost.
        List<DocumentEmbedding> documentEmbeddings = new ArrayList<>();
        for (String s : chunks) {
            String chunk = s.trim();
            if (chunk.isEmpty()) continue;

            // call embedding model
            float[] vector = embeddingModel.embed(chunk);

            // many EmbeddingResponse implementations have getEmbedding() -> float[] or List<Double>
            DocumentEmbedding documentEmbedding = new DocumentEmbedding();
            documentEmbedding.setDocId(UUID.randomUUID());
            documentEmbedding.setSource(source);
            documentEmbedding.setChunkText(chunk);
            documentEmbedding.setEmbedding(vector);
            documentEmbedding.setMetadata("{}");
            documentEmbedding.setCreatedAt(Instant.now());
            documentEmbeddings.add(documentEmbedding);
        }
        embeddingRepository.saveAll(documentEmbeddings);
    }

    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        int len = text.length();
        int start = 0;
        while (start < len) {
            // chunk sizes (tune these to your model/tokenization)
            // approximate
            int CHUNK_SIZE_CHARS = 1500;
            int end = Math.min(len, start + CHUNK_SIZE_CHARS);
            String piece = text.substring(start, end);
            chunks.add(piece);
            if (end == len) break;
            int CHUNK_OVERLAP = 200;
            start = Math.max(0, end - CHUNK_OVERLAP);
        }
        return chunks;
    }
}
