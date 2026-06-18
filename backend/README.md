# FlowMind Agent Backend

Spring Boot 3.x multi-module Maven backend. The demo currently uses `app-service` as the single aggregated startup service, while keeping module boundaries such as `user-service`, `ai-agent-service`, `knowledge-service`, `content-service`, `student-service`, `school-service`, `analytics-service`, and `feishu-service`.

## 1. Start MySQL

If the container already exists:

```powershell
docker start mysql9
```

If it does not exist:

```powershell
docker run -d --name mysql9 -p 3306:3306 -v H:\Docker\mysql:/var/lib/mysql -e MYSQL_ROOT_PASSWORD=123456 mysql:9.7.0
```

The backend connects to:

```text
jdbc:mysql://localhost:3306/FlowMind
username: root
password: 123456
```

## 2. Start Backend

Default behavior:

- `flowmind.llm.provider` is `deepseek`.
- If `flowmind.llm.api-key` is empty, the backend automatically uses `MockLLMClient`.
- If `flowmind.llm.api-key` is set, the backend automatically uses `OpenAiCompatibleLLMClient` and calls DeepSeek.

Start command:

```powershell
cd "H:\Babycode\FlowMind Agent\flowmind-agent\backend"
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run
```

If port `8080` is occupied:

```powershell
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run "-Dspring-boot.run.arguments=--server.port=18080"
```

## 3. Start Backend With Real DeepSeek API

The backend does not use a DeepSeek SDK. It uses `OpenAiCompatibleLLMClient`, so DeepSeek, OpenAI-compatible gateways, and local OpenAI-compatible models can share the same client.

Because DeepSeek is now the default provider, you only need to provide an API key. The recommended local setup is `application-local.yml`, which is ignored by Git and is imported automatically by `application.yml`.

### Recommended: local private config

Create a local config file:

```powershell
Copy-Item .\app-service\src\main\resources\application-local.template.yml .\app-service\src\main\resources\application-local.yml
```

Edit:

```text
app-service/src/main/resources/application-local.yml
```

Set your private keys:

```yaml
flowmind:
  llm:
    api-key: your_deepseek_api_key
  embedding:
    api-key: your_embedding_api_key
```

Then keep using the normal startup command:

```powershell
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run
```

`application-local.yml` is listed in `.gitignore`, so it should not be committed.

### Option A: Direct command

This is the fastest way to test. Replace `sk-xxxx` with your DeepSeek API key:

```powershell
cd "H:\Babycode\FlowMind Agent\flowmind-agent\backend"
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run "-Dspring-boot.run.arguments=--flowmind.llm.api-key=sk-xxxx"
```

If port `8080` is occupied:

```powershell
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run "-Dspring-boot.run.arguments=--server.port=18080 --flowmind.llm.api-key=sk-xxxx"
```

Note: this puts the API key in terminal history. For longer-term use, prefer Option B.

### Option B: Use profile config

Create a local config file from the template:

```powershell
Copy-Item .\app-service\src\main\resources\application-deepseek.template.yml .\app-service\src\main\resources\application-deepseek.yml
```

Edit:

```text
app-service/src/main/resources/application-deepseek.yml
```

Set:

```yaml
flowmind:
  llm:
    provider: deepseek
    base-url: https://api.deepseek.com
    chat-path: /chat/completions
    model: deepseek-chat
    api-key: your_deepseek_api_key
```

Then start with:

```powershell
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=deepseek"
```

With another port:

```powershell
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=deepseek --server.port=18080"
```

## 3.1 If an API Key Was Accidentally Pushed

Do this immediately:

1. Revoke or rotate the leaked key in the provider console. Removing it from Git does not make the old key safe.
2. Remove the key from tracked files and put it in `application-local.yml`.
3. If the file is already tracked by Git, untrack only that file:

```powershell
git rm --cached app-service/src/main/resources/application-local.yml
```

4. Commit the cleanup:

```powershell
git add app-service/src/main/resources/application.yml app-service/src/main/resources/application-local.template.yml ..\.gitignore README.md
git commit -m "chore: move api keys to local config"
```

If the real key already exists in public GitHub history, rotate the key first. History rewriting can remove the text from old commits, but it cannot make the exposed key trustworthy again.

## 4. Verify LLM Mode

If the AI workbench replies like this:

```text
MockLLM ...
```

then the backend is still using MockLLM.

This means no valid DeepSeek API key was found, or `flowmind.llm.provider=mock` was explicitly set.

If DeepSeek is enabled correctly, responses should come from the real model through:

```text
ai-agent-service/src/main/java/com/flowmind/agent/llm/OpenAiCompatibleLLMClient.java
```

Streaming endpoint:

```text
POST /api/agents/chat/stream
Content-Type: application/json
Accept: text/event-stream
```

Example body:

```json
{
  "agentType": "auto",
  "message": "你好，帮我生成 5 个保研小红书选题",
  "context": {}
}
```

## 5. Access URLs

Default port:

```text
API: http://localhost:8080
Swagger: http://localhost:8080/swagger-ui.html
```

If using port `18080`:

```text
API: http://localhost:18080
Swagger: http://localhost:18080/swagger-ui.html
```

Demo accounts are stored in MySQL and are initialized automatically when the backend starts:

```text
admin   / 123456  团队管理员
content / 123456  内容运营人员
teacher / 123456  教育咨询老师
ip      / 123456  个人IP运营者
student / 123456  学员用户
```

RBAC tables:

```text
sys_user
sys_role
sys_user_role
sys_permission
sys_role_permission
```

Most APIs require a login token returned by `/api/auth/login`:

```text
Authorization: Bearer mock-jwt.xxxxxx
```

For backward-compatible demos, old tokens such as `Bearer mock-jwt.admin` or `Bearer mock-jwt.demo` are still accepted if the username exists.

Permission management APIs:

```http
GET /api/roles
GET /api/permissions
GET /api/roles/{roleCode}/permissions
PUT /api/roles/{roleCode}/permissions
```

## 6. Common Errors

### Unable to find a suitable main class

Do not run `spring-boot:run` from the parent module directly. Use:

```powershell
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run
```

### Port 8080 was already in use

Check the process:

```powershell
Get-NetTCPConnection -LocalPort 8080 | Select-Object LocalAddress,LocalPort,State,OwningProcess
```

Or use another port:

```powershell
.\mvnw.cmd -s maven-settings.xml -pl app-service -am spring-boot:run "-Dspring-boot.run.arguments=--server.port=18080"
```

### Maven repository permission issue

Keep `-s maven-settings.xml` in the command. The project uses a local Maven repository setting to avoid Windows user directory permission problems.

## 7. Feishu / Lark CLI

Check whether `lark-cli` is available:

```powershell
lark-cli --version
```

Config:

```yaml
flowmind:
  tools:
    lark-cli:
      enabled: true
      command: lark-cli
      timeout-seconds: 30
```

The Java bridge only exposes whitelisted methods through:

```text
ai-agent-service/src/main/java/com/flowmind/agent/service/LarkCliToolService.java
```

Do not let the LLM execute arbitrary shell commands directly.

## 8. Vector Search Endpoint

The decoupled vector retrieval example is available at:

```http
GET /api/knowledge/vector/search?q=保研简历&topK=5
```

Documentation:

```text
docs/README-vector-search-extension.md
```
