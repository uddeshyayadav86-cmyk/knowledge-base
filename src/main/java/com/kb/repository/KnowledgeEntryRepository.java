package com.kb.repository;

import com.kb.model.KnowledgeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeEntryRepository extends JpaRepository<KnowledgeEntry, Long> {

    // Find by tags
    @Query("SELECT e FROM KnowledgeEntry e JOIN e.tags t WHERE t = :tag")
    List<KnowledgeEntry> findByTag(@Param("tag") String tag);

    // Full text search on title and content (fallback when no vector DB)
    @Query("SELECT e FROM KnowledgeEntry e WHERE " +
           "LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.content) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<KnowledgeEntry> fullTextSearch(@Param("query") String query);

    // Find all entries ordered by creation date
    List<KnowledgeEntry> findAllByOrderByCreatedAtDesc();

    // Find by source type
    List<KnowledgeEntry> findBySourceType(KnowledgeEntry.SourceType sourceType);

    // NOTE: If you add pgvector extension to PostgreSQL, replace fullTextSearch with:
    // @Query(value = "SELECT * FROM knowledge_entries ORDER BY embedding <-> CAST(:queryEmbedding AS vector) LIMIT :limit", nativeQuery = true)
    // List<KnowledgeEntry> findSimilarByEmbedding(@Param("queryEmbedding") String queryEmbedding, @Param("limit") int limit);
}
