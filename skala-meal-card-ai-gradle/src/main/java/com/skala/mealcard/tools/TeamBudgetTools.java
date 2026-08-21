package com.skala.mealcard.tools;

import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.skala.mealcard.domain.Team;
import com.skala.mealcard.service.MealRequestService;

@Component
public class TeamBudgetTools {

    private final MealRequestService service;

    public TeamBudgetTools(MealRequestService service) {
        this.service = service;
    }

    @Tool(description = "현재 사용자의 팀 회식비 분기 예산, 사용액, 잔여 예산을 조회한다. 다른 팀 예산 조회에는 사용하지 않는다.")
    public Map<String, Object> getTeamBudget(ToolContext context) {
        ToolUsageContext.markUsed(context);

        String userId = String.valueOf(context.getContext().get("userId"));
        Team team = service.requireUserTeam(userId);

        return Map.of(
                "teamId", team.teamId(),
                "teamName", team.teamName(),
                "memberCount", team.memberCount(),
                "quarterlyBudget", team.quarterlyBudget(),
                "usedBudget", team.usedBudget(),
                "remainingBudget", team.remainingBudget()
        );
    }
}
