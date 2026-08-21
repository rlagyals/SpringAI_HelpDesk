package com.skala.mealcard.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.skala.mealcard.domain.MealRequest;

@Repository
public class MealRequestRepository {

    private final Map<String, MealRequest> requests = new ConcurrentHashMap<>();

    public MealRequest save(MealRequest request) {
        requests.put(request.requestId(), request);
        return request;
    }

    public Optional<MealRequest> findById(String requestId) {
        return Optional.ofNullable(requests.get(requestId));
    }

    public List<MealRequest> findAll() {
        return requests.values().stream()
                .sorted(Comparator.comparing(MealRequest::createdAt).reversed())
                .toList();
    }

    public List<MealRequest> findByTeamId(String teamId) {
        return requests.values().stream()
                .filter(r -> r.teamId().equals(teamId))
                .sorted(Comparator.comparing(MealRequest::createdAt).reversed())
                .toList();
    }

    public Optional<MealRequest> findLatestByApplicant(String userId) {
        return requests.values().stream()
                .filter(r -> r.applicantUserId().equals(userId))
                .max(Comparator.comparing(MealRequest::createdAt));
    }
}
