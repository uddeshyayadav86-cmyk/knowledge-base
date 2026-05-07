package com.kb.controller;

import com.kb.model.KnowledgeEntry;
import com.kb.service.KnowledgeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/entries/note")
    public ResponseEntity<KnowledgeEntry> addNote(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        List<String> tags = body.containsKey("tags") ? (List<String>) body.get("tags") : List.of();
        return ResponseEntity.ok(knowledgeService.saveNote(title, content, tags));
    }

    @PostMapping("/entries/url")
    public ResponseEntity<KnowledgeEntry> addUrl(@RequestBody Map<String, Object> body) throws IOException {
        String url = (String) body.get("url");
        List<String> tags = body.containsKey("tags") ? (List<String>) body.get("tags") : List.of();
        return ResponseEntity.ok(knowledgeService.saveFromUrl(url, tags));
    }

    @PostMapping("/entries/pdf")
    public ResponseEntity<KnowledgeEntry> addPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "tags", required = false) String tagsStr) throws IOException {
        List<String> tags = tagsStr != null ? Arrays.asList(tagsStr.split(",")) : List.of();
        return ResponseEntity.ok(knowledgeService.saveFromPdf(file, title, tags));
    }

    @PostMapping("/query")
    public ResponseEntity<Map<String, String>> query(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        String answer = knowledgeService.query(question);
        return ResponseEntity.ok(Map.of("answer", answer));
    }

    @GetMapping("/reflect/weekly")
    public ResponseEntity<Map<String, String>> weeklyReflection() {
        String reflection = knowledgeService.weeklyReflection();
        return ResponseEntity.ok(Map.of("reflection", reflection));
    }

    @GetMapping("/entries")
    public ResponseEntity<List<KnowledgeEntry>> getAll() {
        return ResponseEntity.ok(knowledgeService.getAll());
    }

    @GetMapping("/entries/{id}")
    public ResponseEntity<KnowledgeEntry> getById(@PathVariable Long id) {
        return ResponseEntity.ok(knowledgeService.getById(id));
    }

    @DeleteMapping("/entries/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/entries/tag/{tag}")
    public ResponseEntity<List<KnowledgeEntry>> getByTag(@PathVariable String tag) {
        return ResponseEntity.ok(knowledgeService.getByTag(tag));
    }
}