package com.flowmind.agent.extension;

import com.flowmind.agent.dto.AgentRequest;
import com.flowmind.agent.service.LarkCliToolService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LarkCliMcpExtension implements McpToolProvider {
    @Value("${flowmind.tools.lark-cli.enabled:true}")
    private boolean enabled;

    @Value("${flowmind.tools.lark-cli.command:lark-cli}")
    private String command;

    @Value("${flowmind.feishu.knowledge-base.name:保研知识库}")
    private String knowledgeBaseName;

    @Value("${flowmind.feishu.knowledge-base.folder-token:}")
    private String knowledgeBaseFolderToken;

    @Value("${flowmind.feishu.knowledge-base.url:}")
    private String knowledgeBaseUrl;

    private final LarkCliToolService toolService;

    public LarkCliMcpExtension(LarkCliToolService toolService) {
        this.toolService = toolService;
    }

    @Override
    public String name() {
        return "lark-cli";
    }

    @Override
    public String description() {
        return "Feishu/Lark CLI bridge for whitelisted document and Drive folder operations.";
    }

    @Override
    public boolean supports(String agentType) {
        return enabled && ("feishu".equals(agentType)
                || "knowledge".equals(agentType)
                || "content".equals(agentType));
    }

    @Override
    public String runtimeContext(AgentRequest request) {
        if (!enabled) {
            return "Lark CLI bridge is disabled. Tool calling unavailable.";
        }

        String version = toolService.checkVersion();
        return """
                Lark CLI tool bridge is ACTIVE.
                - Command: %s
                - Status: %s
                - Default Feishu knowledge folder:
                  - name: %s
                  - folderToken: %s
                  - url: %s
                - Whitelisted backend tools:
                  - CREATE_DOC: create a Feishu document. If the user mentions the knowledge folder, use parentToken=%s.
                  - FETCH_DOC: fetch a Feishu doc by URL or token.
                  - LIST_KNOWLEDGE_FOLDER: list direct children of the configured knowledge folder.
                - Never invent Feishu URLs, tokens, or folder contents. Use backend tool results only.
                """.formatted(
                command,
                version,
                knowledgeBaseName,
                blank(knowledgeBaseFolderToken),
                blank(knowledgeBaseUrl),
                blank(knowledgeBaseFolderToken)
        );
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? "(not configured)" : value;
    }
}
