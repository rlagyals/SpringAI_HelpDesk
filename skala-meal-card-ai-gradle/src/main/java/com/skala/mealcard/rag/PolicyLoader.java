package com.skala.mealcard.rag;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class PolicyLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PolicyLoader.class);

    private final ResourceLoader loader;
    private final PolicyIngestService ingestService;

    public PolicyLoader(ResourceLoader loader, PolicyIngestService ingestService) {
        this.loader = loader;
        this.ingestService = ingestService;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> paths = List.of(
                "classpath:policies/corporate-card-policy.md",
                "classpath:policies/meal-expense-policy.md",
                "classpath:policies/meal-request-approval-policy.md"
        );

        int chunks = paths.stream()
                .map(loader::getResource)
                .mapToInt(r -> ingestService.ingest(r, "2026-08"))
                .sum();

        log.info("정책 문서 인제스트 완료. chunks={}", chunks);
    }
}
