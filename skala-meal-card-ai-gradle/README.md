# SKALA 법인카드 회식 매니저

이번 종합실습용 프로젝트는 **Gradle(build.gradle)** 기반이며, API 검증은 **Swagger UI**에서 할 수 있게 구성했다.
Docker는 사용하지 않는다. RAG는 `SimpleVectorStore`, 메모리/팀/신청 데이터는 인메모리 저장소를 사용한다.

## 실행

```bash
export OPENAI_API_KEY="본인_API_KEY"
./gradlew bootRun
```

> 이 프로젝트의 `gradlew`는 수업용 Mac 환경에 이미 설치된 `gradle` 명령을 호출한다.
> `gradle -v`가 정상 출력되는 환경에서 `./gradlew bootRun`으로 실행 가능하다.

## Swagger

앱 실행 후 브라우저에서:

```text
http://localhost:8080/swagger-ui.html
```

Swagger에서 **AI Chat → POST /api/chat → Try it out → Execute** 순서로 테스트한다.

기본 예시:

```json
{
  "question": "평일 일반 회식은 1인당 얼마까지 가능해?",
  "userId": "member-a1",
  "sessionId": "s1"
}
```

### 질문 예시

RAG:

```text
평일 일반 회식은 1인당 얼마까지 가능해?
주류 결제는 얼마까지 가능해?
회식 신청은 언제까지 해야 해?
```

Tool:

```text
우리 팀 회식비 예산 얼마나 남았어?
2026-08-25에 4명이 정기 회식할 건데 신청해줘. 술은 안 마시고 심야도 아니야.
아까 신청한 회식 승인 상태 알려줘.
```

Memory 테스트는 `userId`와 `sessionId`를 동일하게 유지한다.

## 실제 승인

AI는 승인하지 않는다. Swagger의 **Manager Approval** API에서 MANAGER가 별도로 처리한다.

```text
POST /api/approvals/{requestId}/approve
POST /api/approvals/{requestId}/reject
```

승인 body 예시:

```json
{
  "managerUserId": "manager-a"
}
```

## 데모 사용자

| userId | 역할 | 팀 |
|---|---|---|
| member-a1 | MEMBER | TEAM-A |
| member-a2 | MEMBER | TEAM-A |
| manager-a | MANAGER | TEAM-A |
| member-b1 | MEMBER | TEAM-B |
| manager-b | MANAGER | TEAM-B |

TEAM-A는 6명, 분기 예산 540,000원, 현재 사용액 180,000원이다.
TEAM-B는 4명, 분기 예산 360,000원, 현재 사용액 90,000원이다.

## 구현 범위

- Phase 1: ChatClient + Advisor 체인
- Phase 2: Markdown 규정 문서 chunk + source/version metadata + SimpleVectorStore
- Phase 2 심화: `GET /api/admin/chunks`
- Phase 3: RAG 답변 + 검색 문서 출처 반환
- Phase 4: `getTeamBudget`, `createMealRequest`, `getMealRequestStatus` Tool
- 권한: ToolContext의 userId 기준 자기 팀/자기 신청 검증
- 실제 승인: AI Tool이 아니라 MANAGER REST API
- Phase 5: `userId + sessionId` 기준 대화 메모리
- Phase 6: JSON API + SSE
- Swagger UI

## 규정 문서

```text
src/main/resources/policies/
├── corporate-card-policy.md
├── meal-expense-policy.md
└── meal-request-approval-policy.md
```

## 참고: 부서장 승인 규정

원 규정에는 팀 잔여 예산을 초과하면 부서장 승인이 추가로 필요하다.
이번 구현 범위는 MEMBER/MANAGER 두 역할만이므로 규정을 임의로 없애지 않고,
해당 건은 `EXTERNAL_APPROVAL_REQUIRED` 상태로 남긴다.
