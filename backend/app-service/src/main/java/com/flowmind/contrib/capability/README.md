# Team Capability Folder

This folder is reserved for team-contributed backend capabilities.

Recommended package:

```java
package com.flowmind.contrib.capability.xxx;
```

Why this folder:

- `app-service` depends on all backend modules.
- Classes under `com.flowmind` are scanned by `FlowMindApplication`.
- Team members can add files here without editing existing Agent, Router, or business module code.

For each capability, create a subfolder:

```text
com/flowmind/contrib/capability/
  vectorsearch/
    VectorSearchToolService.java
    VectorSearchController.java
    VectorSearchExtension.java
  websearch/
    WebSearchToolService.java
    WebSearchController.java
    WebSearchExtension.java
```

Minimal file set:

1. `XxxToolService.java`
   - Real capability logic.
   - Can call existing services by constructor injection.

2. `XxxController.java`
   - REST API for testing and frontend usage.

3. `XxxExtension.java`, optional
   - Describes the capability to the agent system.
   - Implement `McpToolProvider` or `SkillProvider`.

Do not modify:

- `AgentRouter.java`
- Existing `*Agent.java`
- Existing service module controllers
- Existing database initialization code

Only change those files after the capability is stable and the maintainer decides to integrate it into the main agent flow.
