package com.skala.mealcard.repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.skala.mealcard.domain.Team;

@Repository
public class TeamRepository {

    private final Map<String, Team> teams = new LinkedHashMap<>();

    public TeamRepository() {
        // 규정: 분기 기준액 = 1인당 90,000원.
        // TEAM-A: 6명 -> 540,000원, 사용액 180,000원
        // TEAM-B: 4명 -> 360,000원, 사용액 90,000원
        teams.put("TEAM-A", new Team("TEAM-A", "AI서비스팀", 6, 540_000, 180_000));
        teams.put("TEAM-B", new Team("TEAM-B", "플랫폼팀", 4, 360_000, 90_000));
    }

    public Optional<Team> findById(String teamId) {
        return Optional.ofNullable(teams.get(teamId));
    }
}
