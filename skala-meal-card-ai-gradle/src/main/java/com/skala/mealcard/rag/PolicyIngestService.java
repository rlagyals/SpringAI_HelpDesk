package com.skala.mealcard.rag;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.skala.mealcard.config.AppProperties;

@Service
public class PolicyIngestService {

    private final VectorStore vectorStore;
    private final AppProperties props;

    public PolicyIngestService(VectorStore vectorStore, AppProperties props) {
        this.vectorStore = vectorStore;
        this.props = props;
    }

    public int ingest(Resource resource, String version) {
        try {
            String source = resource.getFilename() == null ? "unknown" : resource.getFilename();
            String text = resource.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

            List<String> chunks = chunkMarkdown(
                    text,
                    props.chunk().maxChars(),
                    props.chunk().minChars()
            );

            List<Document> documents = chunks.stream()
                    .map(chunk -> new Document(
                            chunk,
                            Map.of(
                                    "source", source,
                                    "version", version,
                                    "department", "HR-FINANCE",
                                    "ingestedAt", LocalDate.now().toString()
                            )))
                    .toList();

            vectorStore.add(documents);
            return documents.size();
        }
        catch (Exception e) {
            throw new IllegalStateException("정책 인제스트 실패: " + resource.getDescription(), e);
        }
    }

    private List<String> chunkMarkdown(String text, int maxChars, int minChars) {
        String normalized = text.replace("\r\n", "\n").trim();
        String[] sections = normalized.split("(?=\\n##\\s)");

        List<String> result = new ArrayList<>();
        String title = "";

        for (String section : sections) {
            String s = section.trim();
            if (s.isBlank()) continue;

            if (title.isBlank() && s.startsWith("# ")) {
                int nl = s.indexOf('\n');
                if (nl > 0) {
                    title = s.substring(0, nl).trim();
                }
            }

            String withTitle = title.isBlank() || s.startsWith("# ")
                    ? s
                    : title + "\n\n" + s;

            if (withTitle.length() <= maxChars) {
                result.add(withTitle);
                continue;
            }

            for (int start = 0; start < withTitle.length(); start += maxChars) {
                int end = Math.min(start + maxChars, withTitle.length());
                String part = withTitle.substring(start, end).trim();
                if (!part.isBlank()) result.add(part);
            }
        }

        if (result.size() > 1 && result.get(result.size() - 1).length() < minChars) {
            String last = result.remove(result.size() - 1);
            int i = result.size() - 1;
            result.set(i, result.get(i) + "\n" + last);
        }

        return result;
    }
}
