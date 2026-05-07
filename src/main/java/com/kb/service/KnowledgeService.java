package com.kb.service;

import com.kb.model.KnowledgeEntry;
import com.kb.repository.KnowledgeEntryRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

    private final KnowledgeEntryRepository repository;
    private final AnthropicService anthropicService;

    public KnowledgeService(KnowledgeEntryRepository repository, AnthropicService anthropicService) {
        this.repository = repository;
        this.anthropicService = anthropicService;
    }

    // ─── INGEST ───────────────────────────────────────────────────────────────

    public KnowledgeEntry saveNote(String title, String content, List<String> tags) {
        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setTitle(title);
        entry.setContent(content);
        entry.setSourceType(KnowledgeEntry.SourceType.NOTE);
        entry.setTags(tags);
        entry.setAiSummary(anthropicService.generateSummary(content));
        return repository.save(entry);
    }

    public KnowledgeEntry saveFromUrl(String url, List<String> tags) throws IOException {
        System.out.println("Scraping URL: " + url);
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(10_000)
                .get();

        String title = doc.title();
        String content = doc.select("article, main, .content, .post-content, p").text();

        if (content.isBlank()) {
            content = doc.body().text();
        }

        if (content.length() > 4000) {
            content = content.substring(0, 4000) + "... [truncated]";
        }

        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setTitle(title);
        entry.setContent(content);
        entry.setSourceType(KnowledgeEntry.SourceType.URL);
        entry.setSourceUrl(url);
        entry.setTags(tags);
        entry.setAiSummary(anthropicService.generateSummary(content));
        return repository.save(entry);
    }

    public KnowledgeEntry saveFromPdf(MultipartFile file, String title, List<String> tags) throws IOException {
        // PDFBox 3.x uses Loader.loadPDF() instead of PDDocument.load()
        PDDocument pdDocument = Loader.loadPDF(file.getBytes());
        PDFTextStripper stripper = new PDFTextStripper();
        String content = stripper.getText(pdDocument);
        pdDocument.close();

        if (content.length() > 4000) {
            content = content.substring(0, 4000) + "... [truncated]";
        }

        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setTitle(title != null && !title.isBlank() ? title : file.getOriginalFilename());
        entry.setContent(content);
        entry.setSourceType(KnowledgeEntry.SourceType.PDF);
        entry.setTags(tags);
        entry.setAiSummary(anthropicService.generateSummary(content));
        return repository.save(entry);
    }

    // ─── QUERY ────────────────────────────────────────────────────────────────

    public String query(String question) {
        List<KnowledgeEntry> relevant = repository.fullTextSearch(question);

        if (relevant.isEmpty()) {
            relevant = repository.findAllByOrderByCreatedAtDesc();
            relevant = relevant.stream().limit(5).collect(Collectors.toList());
        } else {
            relevant = relevant.stream().limit(5).collect(Collectors.toList());
        }

        if (relevant.isEmpty()) {
            return "Your knowledge base is empty! Start by adding some notes, URLs, or PDFs.";
        }

        List<String> contextChunks = relevant.stream()
                .map(e -> String.format("**%s** (added %s)\n%s",
                        e.getTitle(),
                        e.getCreatedAt().toLocalDate(),
                        e.getContent()))
                .collect(Collectors.toList());

        return anthropicService.askWithContext(question, contextChunks);
    }

    public String weeklyReflection() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<KnowledgeEntry> recentEntries = repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(e -> e.getCreatedAt().isAfter(weekAgo))
                .collect(Collectors.toList());

        if (recentEntries.isEmpty()) {
            return "You haven't added anything to your knowledge base this week. Try adding some notes or articles!";
        }

        List<String> entryTexts = recentEntries.stream()
                .map(e -> String.format("Title: %s\nContent: %s", e.getTitle(), e.getContent()))
                .collect(Collectors.toList());

        return anthropicService.generateWeeklyReflection(entryTexts);
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    public List<KnowledgeEntry> getAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public KnowledgeEntry getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entry not found: " + id));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<KnowledgeEntry> getByTag(String tag) {
        return repository.findByTag(tag);
    }
}