package com.skala.mealcard.repository;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.skala.mealcard.domain.UserAccount;
import com.skala.mealcard.domain.UserRole;

@Repository
public class UserRepository {

    private final Map<String, UserAccount> users = Map.of(
            "member-a1", new UserAccount("member-a1", "김민지", "TEAM-A", UserRole.MEMBER),
            "member-a2", new UserAccount("member-a2", "이준호", "TEAM-A", UserRole.MEMBER),
            "manager-a", new UserAccount("manager-a", "박서연", "TEAM-A", UserRole.MANAGER),
            "member-b1", new UserAccount("member-b1", "최유진", "TEAM-B", UserRole.MEMBER),
            "manager-b", new UserAccount("manager-b", "정도윤", "TEAM-B", UserRole.MANAGER)
    );

    public Optional<UserAccount> findById(String userId) {
        return Optional.ofNullable(users.get(userId));
    }
}
