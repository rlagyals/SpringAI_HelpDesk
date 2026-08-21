package com.skala.mealcard.tools;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.ai.chat.model.ToolContext;

/**
 * ChatClient의 toolContext에 함께 담아 보낸 {@link AtomicBoolean} 플래그를 통해,
 * 이번 대화 턴에서 Tool이 실제로 호출됐는지를 호출 스레드/스트리밍 여부와 무관하게 추적한다.
 * ToolContext.getContext()는 불변 뷰지만, 그 안의 값 객체 자체는 그대로 공유되므로
 * Tool 쪽에서 flag.set(true)로 표시하면 호출자가 같은 참조로 다시 읽을 수 있다.
 */
public final class ToolUsageContext {

    public static final String FLAG_KEY = "toolUsedFlag";

    private ToolUsageContext() {
    }

    public static void markUsed(ToolContext context) {
        Object flag = context.getContext().get(FLAG_KEY);

        if (flag instanceof AtomicBoolean atomicBoolean) {
            atomicBoolean.set(true);
        }
    }
}
