package com.flowmind.agent.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@ConditionalOnExpression("'${flowmind.llm.provider:deepseek}' == 'mock' || !T(org.springframework.util.StringUtils).hasText('${flowmind.llm.api-key:}') || '${flowmind.llm.api-key:}'.contains('PUT_YOUR')")
public class MockLLMClient implements LLMClient {
    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return "已基于 MockLLM 生成结构化建议："
                + userPrompt
                + "\n\n下一步建议同步到内容日历、飞书文档和多维表格，并保留人工复核节点。";
    }

    @Override
    public void stream(String systemPrompt, String userPrompt, Consumer<String> onDelta) {
        String text = complete(systemPrompt, userPrompt);
        for (int i = 0; i < text.length(); i++) {
            onDelta.accept(String.valueOf(text.charAt(i)));
            try {
                Thread.sleep(12);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
