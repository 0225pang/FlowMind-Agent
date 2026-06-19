package com.flowmind.mobile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Route and action guard definitions for the Android client.
 *
 * The web client already has many implicit checks in page components. The
 * mobile app needs the same decisions in one place because the bottom tab
 * navigation, modal sheets, offline mode and destructive operations are all
 * driven by compact touch interactions. This file is intentionally data-heavy:
 * a teammate can add a new scene by appending one route profile instead of
 * editing MainActivity.
 */
public final class MobileRouteGuards {
    private MobileRouteGuards() {
    }

    public static final String ROLE_CONTENT = "CONTENT_OPERATOR";
    public static final String ROLE_TEACHER = "EDU_CONSULTANT";
    public static final String ROLE_IP = "IP_OPERATOR";
    public static final String ROLE_ADMIN = "TEAM_ADMIN";
    public static final String ROLE_STUDENT = "STUDENT_USER";

    public static final String SCENE_AGENT = "agent";
    public static final String SCENE_KNOWLEDGE = "knowledge";
    public static final String SCENE_CONTENT = "content";
    public static final String SCENE_SCHOOL = "school";
    public static final String SCENE_SETTINGS = "settings";
    public static final String SCENE_STUDENT = "student";
    public static final String SCENE_FEISHU = "feishu";
    public static final String SCENE_ANALYTICS = "analytics";

    private static final Map<String, RouteProfile> ROUTES = buildRoutes();
    private static final Map<String, ActionPolicy> ACTIONS = buildActions();
    private static final Map<String, List<FieldRule>> FIELD_RULES = buildFieldRules();

    public static RouteDecision canEnter(RouteIntent intent) {
        RouteIntent safeIntent = intent == null ? RouteIntent.empty() : intent;
        RouteProfile profile = ROUTES.get(safeIntent.scene);
        if (profile == null) {
            return RouteDecision.blocked(
                    safeIntent.scene,
                    "Unknown scene",
                    "The mobile client does not know how to open this scene.",
                    Arrays.asList("Return to AI Workspace", "Check whether the feature is registered in MobileRouteGuards")
            );
        }
        if (profile.requiresLogin && !safeIntent.loggedIn) {
            return RouteDecision.blocked(
                    safeIntent.scene,
                    "Login required",
                    "Please sign in before opening " + profile.label + ".",
                    Arrays.asList("Use admin/123456 for demo", "Open Settings and confirm the backend address")
            );
        }
        if (!profile.allowedRoles.isEmpty() && !profile.allowedRoles.contains(safeIntent.role)) {
            return RouteDecision.blocked(
                    safeIntent.scene,
                    "No permission",
                    "Current role cannot access " + profile.label + ".",
                    Arrays.asList("Switch to a role with permission", "Ask the team admin to adjust role permissions")
            );
        }
        if (profile.requiresNetwork && !safeIntent.networkAvailable && !profile.offlineReadable) {
            return RouteDecision.blocked(
                    safeIntent.scene,
                    "Network unavailable",
                    "This scene requires backend data and has no offline fallback.",
                    Arrays.asList("Check Wi-Fi or mobile network", "Try again after the backend tunnel is reachable")
            );
        }
        if (profile.requiresNetwork && !safeIntent.networkAvailable && profile.offlineReadable) {
            return RouteDecision.warning(
                    safeIntent.scene,
                    "Offline fallback",
                    "Showing cached or mock data for " + profile.label + ".",
                    Arrays.asList("Reconnect network to refresh real data", "Avoid editing cached content until sync succeeds")
            );
        }
        if (profile.requiresFeishuAuth && !safeIntent.feishuAuthorized) {
            return RouteDecision.warning(
                    safeIntent.scene,
                    "Feishu authorization missing",
                    "You can browse existing local data, but Feishu sync actions will be disabled.",
                    Arrays.asList("Authorize lark-cli on backend", "Confirm the folder token and document scope")
            );
        }
        if (profile.requiresVectorReady && !safeIntent.vectorReady) {
            return RouteDecision.warning(
                    safeIntent.scene,
                    "Vector knowledge not ready",
                    "Knowledge search may fall back to normal backend answers.",
                    Arrays.asList("Start Weaviate", "Trigger knowledge sync before asking document-specific questions")
            );
        }
        return RouteDecision.allowed(
                safeIntent.scene,
                "Ready",
                profile.label + " is available.",
                profile.quickTips
        );
    }

    public static RouteDecision canRunAction(RouteIntent intent, String actionKey) {
        RouteDecision routeDecision = canEnter(intent);
        if (routeDecision.status == DecisionStatus.BLOCKED) {
            return routeDecision;
        }
        ActionPolicy action = ACTIONS.get(actionKey);
        if (action == null) {
            return RouteDecision.blocked(
                    actionKey,
                    "Unknown action",
                    "This operation has not been registered on mobile.",
                    Arrays.asList("Add it to MobileRouteGuards.buildActions", "Keep the backend endpoint documented")
            );
        }
        RouteIntent safeIntent = intent == null ? RouteIntent.empty() : intent;
        if (!action.allowedScenes.isEmpty() && !action.allowedScenes.contains(safeIntent.scene)) {
            return RouteDecision.blocked(
                    actionKey,
                    "Wrong scene",
                    "This action is not designed for the current screen.",
                    Arrays.asList("Open " + action.allowedScenes, "Use the scene-specific action button")
            );
        }
        if (!action.allowedRoles.isEmpty() && !action.allowedRoles.contains(safeIntent.role)) {
            return RouteDecision.blocked(
                    actionKey,
                    "No operation permission",
                    "Current role cannot run " + action.label + ".",
                    Arrays.asList("Switch role", "Ask admin to grant the action permission")
            );
        }
        if (action.requiresNetwork && !safeIntent.networkAvailable) {
            return RouteDecision.blocked(
                    actionKey,
                    "Network required",
                    action.label + " needs backend access.",
                    Arrays.asList("Reconnect network", "Retry after backend health check succeeds")
            );
        }
        if (action.requiresFeishuAuth && !safeIntent.feishuAuthorized) {
            return RouteDecision.blocked(
                    actionKey,
                    "Feishu authorization required",
                    action.label + " needs Feishu Drive or Doc permission.",
                    Arrays.asList("Run lark-cli auth login on backend", "Confirm document:create and document:retrieve scopes")
            );
        }
        if (action.requiresVectorReady && !safeIntent.vectorReady) {
            return RouteDecision.warning(
                    actionKey,
                    "Vector search unavailable",
                    "The action can continue, but answers may not include knowledge-base evidence.",
                    Arrays.asList("Start Weaviate", "Use document keyword search as fallback")
            );
        }
        if (action.destructive) {
            return RouteDecision.warning(
                    actionKey,
                    "Confirmation required",
                    action.label + " changes stored data. Show a confirmation sheet before continuing.",
                    action.recoveryTips
            );
        }
        return RouteDecision.allowed(actionKey, "Action ready", action.label + " can run.", action.recoveryTips);
    }

    public static List<FieldProblem> validateFields(String formKey, Map<String, String> values) {
        List<FieldRule> rules = FIELD_RULES.get(formKey);
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, String> safeValues = values == null ? Collections.<String, String>emptyMap() : values;
        List<FieldProblem> problems = new ArrayList<>();
        for (FieldRule rule : rules) {
            String value = safeValues.get(rule.name);
            FieldProblem problem = rule.validate(value);
            if (problem != null) {
                problems.add(problem);
            }
        }
        return problems;
    }

    public static String normalizeScene(String scene) {
        String value = scene == null ? "" : scene.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return SCENE_AGENT;
        }
        if ("ai".equals(value) || "chat".equals(value) || "workspace".equals(value)) {
            return SCENE_AGENT;
        }
        if ("kb".equals(value) || "vector".equals(value) || "doc".equals(value)) {
            return SCENE_KNOWLEDGE;
        }
        if ("topic".equals(value) || "copy".equals(value) || "calendar".equals(value)) {
            return SCENE_CONTENT;
        }
        if ("school-project".equals(value) || "project".equals(value)) {
            return SCENE_SCHOOL;
        }
        return value;
    }

    public static List<RouteProfile> listRoutes() {
        return new ArrayList<>(ROUTES.values());
    }

    public static List<ActionPolicy> listActions() {
        return new ArrayList<>(ACTIONS.values());
    }

    public static RouteProfile getRoute(String scene) {
        return ROUTES.get(normalizeScene(scene));
    }

    public static ActionPolicy getAction(String actionKey) {
        return ACTIONS.get(actionKey);
    }

    public static List<String> visibleScenesForRole(String role) {
        List<String> scenes = new ArrayList<>();
        for (RouteProfile profile : ROUTES.values()) {
            if (profile.allowedRoles.isEmpty() || profile.allowedRoles.contains(role)) {
                scenes.add(profile.scene);
            }
        }
        return scenes;
    }

    public static List<String> disabledScenesForRole(String role) {
        List<String> scenes = new ArrayList<>();
        for (RouteProfile profile : ROUTES.values()) {
            if (!profile.allowedRoles.isEmpty() && !profile.allowedRoles.contains(role)) {
                scenes.add(profile.scene);
            }
        }
        return scenes;
    }

    public static Map<String, Object> exportSnapshot(String role) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("role", role);
        snapshot.put("visibleScenes", visibleScenesForRole(role));
        snapshot.put("disabledScenes", disabledScenesForRole(role));
        snapshot.put("routeCount", ROUTES.size());
        snapshot.put("actionCount", ACTIONS.size());
        snapshot.put("formCount", FIELD_RULES.size());
        List<Map<String, Object>> routes = new ArrayList<>();
        for (RouteProfile route : ROUTES.values()) {
            routes.add(route.toMap());
        }
        snapshot.put("routes", routes);
        return snapshot;
    }

    public static String renderDebugReport(String role) {
        StringBuilder builder = new StringBuilder();
        builder.append("FlowMind Mobile Route Guard Report\n");
        builder.append("Role: ").append(role).append('\n');
        builder.append("Visible scenes:\n");
        for (String scene : visibleScenesForRole(role)) {
            RouteProfile route = ROUTES.get(scene);
            builder.append("- ").append(route.label).append(" (").append(scene).append(")\n");
        }
        builder.append("Disabled scenes:\n");
        for (String scene : disabledScenesForRole(role)) {
            RouteProfile route = ROUTES.get(scene);
            builder.append("- ").append(route.label).append(" (").append(scene).append(")\n");
        }
        builder.append("Registered actions:\n");
        for (ActionPolicy action : ACTIONS.values()) {
            builder.append("- ").append(action.key).append(": ").append(action.label).append('\n');
        }
        return builder.toString();
    }

    private static Map<String, RouteProfile> buildRoutes() {
        Map<String, RouteProfile> map = new LinkedHashMap<>();
        map.put(SCENE_AGENT, new RouteProfile.Builder(SCENE_AGENT, "AI Workspace")
                .icon("message-circle")
                .requiresLogin(true)
                .requiresNetwork(true)
                .offlineReadable(true)
                .requiresVectorReady(true)
                .roles(ROLE_CONTENT, ROLE_TEACHER, ROLE_IP, ROLE_ADMIN, ROLE_STUDENT)
                .endpoint("/api/agents/chat/stream")
                .emptyTitle("No conversation yet")
                .emptyMessage("Ask about content, knowledge, schools or Feishu operations.")
                .quickTips("Vector search is checked first", "Only called tools should be displayed", "Thinking text can be expanded after completion")
                .build());
        map.put(SCENE_KNOWLEDGE, new RouteProfile.Builder(SCENE_KNOWLEDGE, "Knowledge Base")
                .icon("book-open")
                .requiresLogin(true)
                .requiresNetwork(true)
                .offlineReadable(true)
                .requiresVectorReady(true)
                .roles(ROLE_CONTENT, ROLE_TEACHER, ROLE_IP, ROLE_ADMIN, ROLE_STUDENT)
                .endpoint("/api/knowledge/docs")
                .endpoint("/api/knowledge/vector/search")
                .emptyTitle("No documents")
                .emptyMessage("Sync Feishu documents or add local mock documents first.")
                .quickTips("Prefer vector results with title and snippet", "Fallback to document list when semantic result is empty", "Show source token when available")
                .build());
        map.put(SCENE_CONTENT, new RouteProfile.Builder(SCENE_CONTENT, "Content Creation")
                .icon("calendar-edit")
                .requiresLogin(true)
                .requiresNetwork(true)
                .offlineReadable(true)
                .roles(ROLE_CONTENT, ROLE_TEACHER, ROLE_IP, ROLE_ADMIN)
                .endpoint("/api/content/topics")
                .endpoint("/api/content/copies")
                .endpoint("/api/content/calendar")
                .emptyTitle("No content assets")
                .emptyMessage("Generate topics in AI Workspace or add content manually.")
                .quickTips("Calendar markers should open daily publication list", "Copy cards can include image suggestions", "Ratings help identify reusable assets")
                .build());
        map.put(SCENE_SCHOOL, new RouteProfile.Builder(SCENE_SCHOOL, "School Intelligence")
                .icon("school")
                .requiresLogin(true)
                .requiresNetwork(true)
                .offlineReadable(true)
                .roles(ROLE_CONTENT, ROLE_TEACHER, ROLE_IP, ROLE_ADMIN, ROLE_STUDENT)
                .endpoint("/api/schools")
                .endpoint("/api/school-projects")
                .endpoint("/api/schools/recommend")
                .emptyTitle("No school projects")
                .emptyMessage("Import project records or use demo data.")
                .quickTips("Deadline risk must be visible", "Student users can browse but cannot edit", "Recommendation needs profile information")
                .build());
        map.put(SCENE_SETTINGS, new RouteProfile.Builder(SCENE_SETTINGS, "Settings")
                .icon("settings")
                .requiresLogin(false)
                .requiresNetwork(false)
                .offlineReadable(true)
                .roles(ROLE_CONTENT, ROLE_TEACHER, ROLE_IP, ROLE_ADMIN, ROLE_STUDENT)
                .endpoint("/api/users/me")
                .emptyTitle("Settings unavailable")
                .emptyMessage("Local settings can still be edited offline.")
                .quickTips("Backend URL must start with http or https", "Local profile keeps the API key out of Git", "Health check before saving is recommended")
                .build());
        map.put(SCENE_STUDENT, new RouteProfile.Builder(SCENE_STUDENT, "Student Management")
                .icon("users")
                .requiresLogin(true)
                .requiresNetwork(true)
                .offlineReadable(false)
                .roles(ROLE_TEACHER, ROLE_ADMIN)
                .endpoint("/api/students")
                .endpoint("/api/students/{id}/analyze")
                .emptyTitle("No students")
                .emptyMessage("Add student profiles from the web admin console.")
                .quickTips("Risk level must be confirmed before saving", "Do not expose private data to student roles", "AI analysis should show evidence")
                .build());
        map.put(SCENE_FEISHU, new RouteProfile.Builder(SCENE_FEISHU, "Feishu Sync")
                .icon("cloud-sync")
                .requiresLogin(true)
                .requiresNetwork(true)
                .offlineReadable(true)
                .requiresFeishuAuth(true)
                .roles(ROLE_CONTENT, ROLE_TEACHER, ROLE_IP, ROLE_ADMIN)
                .endpoint("/api/feishu/sync/status")
                .endpoint("/api/feishu/logs")
                .emptyTitle("No sync logs")
                .emptyMessage("Run a sync action after lark-cli authorization.")
                .quickTips("Show the exact called Feishu tool", "Folder token must be configurable", "Permission errors should include recovery guidance")
                .build());
        map.put(SCENE_ANALYTICS, new RouteProfile.Builder(SCENE_ANALYTICS, "Analytics")
                .icon("chart")
                .requiresLogin(true)
                .requiresNetwork(true)
                .offlineReadable(true)
                .roles(ROLE_CONTENT, ROLE_TEACHER, ROLE_IP, ROLE_ADMIN)
                .endpoint("/api/analytics/overview")
                .endpoint("/api/analytics/content-stats")
                .emptyTitle("No analytics")
                .emptyMessage("Analytics uses mock or MySQL-backed aggregate data.")
                .quickTips("Charts should degrade to metric cards", "Explain empty datasets", "Never block AI Workspace when charts fail")
                .build());
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, ActionPolicy> buildActions() {
        Map<String, ActionPolicy> map = new LinkedHashMap<>();
        add(map, new ActionPolicy.Builder("agent.send", "Send AI message")
                .scenes(SCENE_AGENT)
                .roles(ROLE_CONTENT, ROLE_TEACHER, ROLE_IP, ROLE_ADMIN, ROLE_STUDENT)
                .requiresNetwork(true)
                .requiresVectorReady(true)
                .tips("Validate prompt length", "Show streaming response", "Persist tool calls and thinking text when backend returns them")
                .build());
        add(map, new ActionPolicy.Builder("agent.stop", "Stop streaming response")
                .scenes(SCENE_AGENT)
                .roles(ROLE_CONTENT, ROLE_TEACHER, ROLE_IP, ROLE_ADMIN, ROLE_STUDENT)
                .tips("Abort local stream first", "Keep partial answer visible", "Allow resend after stop")
                .build());
        add(map, new ActionPolicy.Builder("knowledge.search", "Search knowledge base")
                .scenes(SCENE_KNOWLEDGE, SCENE_AGENT)
                .roles(ROLE_CONTENT, ROLE_TEACHER, ROLE_IP, ROLE_ADMIN, ROLE_STUDENT)
                .requiresNetwork(true)
                .requiresVectorReady(true)
                .tips("Search title, content, source and tags", "Show empty-state suggestions", "Keep top results collapsible")
                .build());
        add(map, new ActionPolicy.Builder("knowledge.sync.feishu", "Sync Feishu documents")
                .scenes(SCENE_KNOWLEDGE, SCENE_FEISHU, SCENE_AGENT)
                .roles(ROLE_CONTENT, ROLE_TEACHER, ROLE_IP, ROLE_ADMIN)
                .requiresNetwork(true)
                .requiresFeishuAuth(true)
                .destructive(false)
                .tips("Show folder token", "Write sync record", "Avoid duplicate vector chunks")
                .build());
        add(map, new ActionPolicy.Builder("content.topic.create", "Create content topic")
                .scenes(SCENE_CONTENT, SCENE_AGENT)
                .roles(ROLE_CONTENT, ROLE_TEACHER, ROLE_IP, ROLE_ADMIN)
                .requiresNetwork(true)
                .tips("Validate title", "Default rating to three stars", "Keep source prompt for traceability")
                .build());
        add(map, new ActionPolicy.Builder("content.topic.delete", "Delete content topic")
                .scenes(SCENE_CONTENT)
                .roles(ROLE_CONTENT, ROLE_ADMIN)
                .requiresNetwork(true)
                .destructive(true)
                .tips("Confirm before deleting", "Warn when historical copies exist", "Prefer soft delete")
                .build());
        add(map, new ActionPolicy.Builder("content.copy.create", "Create copywriting asset")
                .scenes(SCENE_CONTENT, SCENE_AGENT)
                .roles(ROLE_CONTENT, ROLE_TEACHER, ROLE_IP, ROLE_ADMIN)
                .requiresNetwork(true)
                .tips("Support image URL or image suggestion", "Validate usage date", "Attach topic id when known")
                .build());
        add(map, new ActionPolicy.Builder("content.copy.delete", "Delete copywriting asset")
                .scenes(SCENE_CONTENT)
                .roles(ROLE_CONTENT, ROLE_ADMIN)
                .requiresNetwork(true)
                .destructive(true)
                .tips("Confirm before deleting", "Show used date if already published", "Prefer archived status")
                .build());
        add(map, new ActionPolicy.Builder("content.calendar.open", "Open publication day")
                .scenes(SCENE_CONTENT)
                .roles(ROLE_CONTENT, ROLE_TEACHER, ROLE_IP, ROLE_ADMIN)
                .tips("Only mark days with content", "Expand the list below calendar", "Keep selected date visible")
                .build());
        add(map, new ActionPolicy.Builder("school.recommend", "Recommend schools")
                .scenes(SCENE_SCHOOL, SCENE_AGENT)
                .roles(ROLE_TEACHER, ROLE_ADMIN, ROLE_STUDENT)
                .requiresNetwork(true)
                .tips("Use student profile when available", "Explain match factors", "Show deadline risk")
                .build());
        add(map, new ActionPolicy.Builder("school.project.save", "Save school project")
                .scenes(SCENE_SCHOOL)
                .roles(ROLE_TEACHER, ROLE_ADMIN)
                .requiresNetwork(true)
                .tips("Validate deadline", "Validate material checklist", "Keep project source URL")
                .build());
        add(map, new ActionPolicy.Builder("student.profile.save", "Save student profile")
                .scenes(SCENE_STUDENT)
                .roles(ROLE_TEACHER, ROLE_ADMIN)
                .requiresNetwork(true)
                .tips("Validate GPA range", "Validate English score", "Mark risk level explicitly")
                .build());
        add(map, new ActionPolicy.Builder("student.profile.delete", "Delete student profile")
                .scenes(SCENE_STUDENT)
                .roles(ROLE_ADMIN)
                .requiresNetwork(true)
                .destructive(true)
                .tips("Confirm with student name", "Prefer soft delete", "Keep audit log")
                .build());
        add(map, new ActionPolicy.Builder("feishu.doc.create", "Create Feishu document")
                .scenes(SCENE_FEISHU, SCENE_AGENT, SCENE_KNOWLEDGE)
                .roles(ROLE_CONTENT, ROLE_TEACHER, ROLE_IP, ROLE_ADMIN)
                .requiresNetwork(true)
                .requiresFeishuAuth(true)
                .tips("Require title and content", "Use parent folder token when provided", "Return document URL")
                .build());
        add(map, new ActionPolicy.Builder("feishu.bot.push", "Push Feishu bot message")
                .scenes(SCENE_FEISHU, SCENE_AGENT)
                .roles(ROLE_CONTENT, ROLE_TEACHER, ROLE_IP, ROLE_ADMIN)
                .requiresNetwork(true)
                .requiresFeishuAuth(true)
                .tips("Preview message before push", "Show target chat", "Store push log")
                .build());
        add(map, new ActionPolicy.Builder("settings.backend.save", "Save backend URL")
                .scenes(SCENE_SETTINGS)
                .roles(ROLE_CONTENT, ROLE_TEACHER, ROLE_IP, ROLE_ADMIN, ROLE_STUDENT)
                .tips("Normalize trailing slash", "Run health check", "Keep last working URL for rollback")
                .build());
        add(map, new ActionPolicy.Builder("settings.logout", "Logout")
                .scenes(SCENE_SETTINGS)
                .roles(ROLE_CONTENT, ROLE_TEACHER, ROLE_IP, ROLE_ADMIN, ROLE_STUDENT)
                .destructive(true)
                .tips("Confirm logout", "Clear token", "Keep backend URL")
                .build());
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, List<FieldRule>> buildFieldRules() {
        Map<String, List<FieldRule>> map = new LinkedHashMap<>();
        map.put("login", Arrays.asList(
                FieldRule.required("username", "Account is required"),
                FieldRule.length("username", 2, 64, "Account length should be 2-64"),
                FieldRule.required("password", "Password is required"),
                FieldRule.length("password", 3, 64, "Password length should be 3-64")
        ));
        map.put("backend-settings", Arrays.asList(
                FieldRule.required("baseUrl", "Backend URL is required"),
                FieldRule.pattern("baseUrl", "^https?://.+", "Backend URL must start with http:// or https://"),
                FieldRule.max("baseUrl", 240, "Backend URL is too long")
        ));
        map.put("agent-message", Arrays.asList(
                FieldRule.required("prompt", "Prompt is required"),
                FieldRule.length("prompt", 2, 4000, "Prompt length should be 2-4000"),
                FieldRule.notSecret("prompt", "Prompt may contain API keys or secrets")
        ));
        map.put("content-topic", Arrays.asList(
                FieldRule.required("title", "Topic title is required"),
                FieldRule.length("title", 2, 120, "Topic title should be 2-120"),
                FieldRule.max("description", 1000, "Topic description is too long"),
                FieldRule.rating("rating", "Rating must be 1-5")
        ));
        map.put("content-copy", Arrays.asList(
                FieldRule.required("title", "Copy title is required"),
                FieldRule.length("title", 2, 120, "Copy title should be 2-120"),
                FieldRule.required("body", "Copy body is required"),
                FieldRule.length("body", 8, 12000, "Copy body should be 8-12000"),
                FieldRule.max("imageUrl", 500, "Image URL is too long"),
                FieldRule.rating("rating", "Rating must be 1-5")
        ));
        map.put("school-project", Arrays.asList(
                FieldRule.required("schoolName", "School name is required"),
                FieldRule.required("projectName", "Project name is required"),
                FieldRule.max("requirement", 2000, "Requirement text is too long"),
                FieldRule.max("materials", 2000, "Material text is too long")
        ));
        map.put("student-profile", Arrays.asList(
                FieldRule.required("studentName", "Student name is required"),
                FieldRule.length("studentName", 2, 80, "Student name should be 2-80"),
                FieldRule.decimalRange("gpa", 0, 5, "GPA should be 0-5"),
                FieldRule.max("targetSchools", 1000, "Target schools text is too long")
        ));
        map.put("feishu-create-doc", Arrays.asList(
                FieldRule.required("title", "Document title is required"),
                FieldRule.length("title", 2, 120, "Document title should be 2-120"),
                FieldRule.required("content", "Document content is required"),
                FieldRule.length("content", 2, 20000, "Document content should be 2-20000"),
                FieldRule.max("parentToken", 160, "Folder token is too long")
        ));
        return Collections.unmodifiableMap(map);
    }

    private static void add(Map<String, ActionPolicy> map, ActionPolicy action) {
        map.put(action.key, action);
    }

    public enum DecisionStatus {
        ALLOWED,
        WARNING,
        BLOCKED
    }

    public static final class RouteIntent {
        public final String scene;
        public final String role;
        public final boolean loggedIn;
        public final boolean networkAvailable;
        public final boolean feishuAuthorized;
        public final boolean vectorReady;
        public final Map<String, String> extras;

        public RouteIntent(String scene, String role, boolean loggedIn, boolean networkAvailable, boolean feishuAuthorized, boolean vectorReady, Map<String, String> extras) {
            this.scene = normalizeScene(scene);
            this.role = role == null || role.trim().isEmpty() ? ROLE_STUDENT : role.trim();
            this.loggedIn = loggedIn;
            this.networkAvailable = networkAvailable;
            this.feishuAuthorized = feishuAuthorized;
            this.vectorReady = vectorReady;
            this.extras = extras == null ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(extras));
        }

        public static RouteIntent empty() {
            return new Builder().scene(SCENE_AGENT).role(ROLE_STUDENT).loggedIn(false).networkAvailable(false).build();
        }

        public static final class Builder {
            private String scene = SCENE_AGENT;
            private String role = ROLE_STUDENT;
            private boolean loggedIn;
            private boolean networkAvailable = true;
            private boolean feishuAuthorized;
            private boolean vectorReady;
            private final Map<String, String> extras = new LinkedHashMap<>();

            public Builder scene(String scene) {
                this.scene = scene;
                return this;
            }

            public Builder role(String role) {
                this.role = role;
                return this;
            }

            public Builder loggedIn(boolean loggedIn) {
                this.loggedIn = loggedIn;
                return this;
            }

            public Builder networkAvailable(boolean networkAvailable) {
                this.networkAvailable = networkAvailable;
                return this;
            }

            public Builder feishuAuthorized(boolean feishuAuthorized) {
                this.feishuAuthorized = feishuAuthorized;
                return this;
            }

            public Builder vectorReady(boolean vectorReady) {
                this.vectorReady = vectorReady;
                return this;
            }

            public Builder extra(String key, String value) {
                if (key != null) {
                    extras.put(key, value == null ? "" : value);
                }
                return this;
            }

            public RouteIntent build() {
                return new RouteIntent(scene, role, loggedIn, networkAvailable, feishuAuthorized, vectorReady, extras);
            }
        }
    }

    public static final class RouteDecision {
        public final DecisionStatus status;
        public final String target;
        public final String title;
        public final String message;
        public final List<String> suggestions;

        private RouteDecision(DecisionStatus status, String target, String title, String message, List<String> suggestions) {
            this.status = status;
            this.target = target;
            this.title = title;
            this.message = message;
            this.suggestions = suggestions == null ? Collections.<String>emptyList() : Collections.unmodifiableList(new ArrayList<>(suggestions));
        }

        public static RouteDecision allowed(String target, String title, String message, List<String> suggestions) {
            return new RouteDecision(DecisionStatus.ALLOWED, target, title, message, suggestions);
        }

        public static RouteDecision warning(String target, String title, String message, List<String> suggestions) {
            return new RouteDecision(DecisionStatus.WARNING, target, title, message, suggestions);
        }

        public static RouteDecision blocked(String target, String title, String message, List<String> suggestions) {
            return new RouteDecision(DecisionStatus.BLOCKED, target, title, message, suggestions);
        }

        public boolean canProceed() {
            return status == DecisionStatus.ALLOWED || status == DecisionStatus.WARNING;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("status", status.name());
            map.put("target", target);
            map.put("title", title);
            map.put("message", message);
            map.put("suggestions", suggestions);
            return map;
        }
    }

    public static final class RouteProfile {
        public final String scene;
        public final String label;
        public final String icon;
        public final boolean requiresLogin;
        public final boolean requiresNetwork;
        public final boolean offlineReadable;
        public final boolean requiresFeishuAuth;
        public final boolean requiresVectorReady;
        public final Set<String> allowedRoles;
        public final List<String> endpoints;
        public final String emptyTitle;
        public final String emptyMessage;
        public final List<String> quickTips;

        private RouteProfile(Builder builder) {
            scene = builder.scene;
            label = builder.label;
            icon = builder.icon;
            requiresLogin = builder.requiresLogin;
            requiresNetwork = builder.requiresNetwork;
            offlineReadable = builder.offlineReadable;
            requiresFeishuAuth = builder.requiresFeishuAuth;
            requiresVectorReady = builder.requiresVectorReady;
            allowedRoles = Collections.unmodifiableSet(new LinkedHashSet<>(builder.allowedRoles));
            endpoints = Collections.unmodifiableList(new ArrayList<>(builder.endpoints));
            emptyTitle = builder.emptyTitle;
            emptyMessage = builder.emptyMessage;
            quickTips = Collections.unmodifiableList(new ArrayList<>(builder.quickTips));
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("scene", scene);
            map.put("label", label);
            map.put("icon", icon);
            map.put("requiresLogin", requiresLogin);
            map.put("requiresNetwork", requiresNetwork);
            map.put("offlineReadable", offlineReadable);
            map.put("requiresFeishuAuth", requiresFeishuAuth);
            map.put("requiresVectorReady", requiresVectorReady);
            map.put("allowedRoles", new ArrayList<>(allowedRoles));
            map.put("endpoints", endpoints);
            map.put("emptyTitle", emptyTitle);
            map.put("emptyMessage", emptyMessage);
            map.put("quickTips", quickTips);
            return map;
        }

        public static final class Builder {
            private final String scene;
            private final String label;
            private String icon = "circle";
            private boolean requiresLogin = true;
            private boolean requiresNetwork = true;
            private boolean offlineReadable;
            private boolean requiresFeishuAuth;
            private boolean requiresVectorReady;
            private final Set<String> allowedRoles = new LinkedHashSet<>();
            private final List<String> endpoints = new ArrayList<>();
            private String emptyTitle = "No data";
            private String emptyMessage = "Pull to refresh or check backend status.";
            private final List<String> quickTips = new ArrayList<>();

            public Builder(String scene, String label) {
                this.scene = normalizeScene(scene);
                this.label = label;
            }

            public Builder icon(String icon) {
                this.icon = icon;
                return this;
            }

            public Builder requiresLogin(boolean requiresLogin) {
                this.requiresLogin = requiresLogin;
                return this;
            }

            public Builder requiresNetwork(boolean requiresNetwork) {
                this.requiresNetwork = requiresNetwork;
                return this;
            }

            public Builder offlineReadable(boolean offlineReadable) {
                this.offlineReadable = offlineReadable;
                return this;
            }

            public Builder requiresFeishuAuth(boolean requiresFeishuAuth) {
                this.requiresFeishuAuth = requiresFeishuAuth;
                return this;
            }

            public Builder requiresVectorReady(boolean requiresVectorReady) {
                this.requiresVectorReady = requiresVectorReady;
                return this;
            }

            public Builder roles(String... roles) {
                if (roles != null) {
                    allowedRoles.addAll(Arrays.asList(roles));
                }
                return this;
            }

            public Builder endpoint(String endpoint) {
                if (endpoint != null && !endpoint.trim().isEmpty()) {
                    endpoints.add(endpoint.trim());
                }
                return this;
            }

            public Builder emptyTitle(String emptyTitle) {
                this.emptyTitle = emptyTitle;
                return this;
            }

            public Builder emptyMessage(String emptyMessage) {
                this.emptyMessage = emptyMessage;
                return this;
            }

            public Builder quickTips(String... tips) {
                if (tips != null) {
                    quickTips.addAll(Arrays.asList(tips));
                }
                return this;
            }

            public RouteProfile build() {
                return new RouteProfile(this);
            }
        }
    }

    public static final class ActionPolicy {
        public final String key;
        public final String label;
        public final Set<String> allowedScenes;
        public final Set<String> allowedRoles;
        public final boolean requiresNetwork;
        public final boolean requiresFeishuAuth;
        public final boolean requiresVectorReady;
        public final boolean destructive;
        public final List<String> recoveryTips;

        private ActionPolicy(Builder builder) {
            key = builder.key;
            label = builder.label;
            allowedScenes = Collections.unmodifiableSet(new LinkedHashSet<>(builder.allowedScenes));
            allowedRoles = Collections.unmodifiableSet(new LinkedHashSet<>(builder.allowedRoles));
            requiresNetwork = builder.requiresNetwork;
            requiresFeishuAuth = builder.requiresFeishuAuth;
            requiresVectorReady = builder.requiresVectorReady;
            destructive = builder.destructive;
            recoveryTips = Collections.unmodifiableList(new ArrayList<>(builder.recoveryTips));
        }

        public static final class Builder {
            private final String key;
            private final String label;
            private final Set<String> allowedScenes = new LinkedHashSet<>();
            private final Set<String> allowedRoles = new LinkedHashSet<>();
            private boolean requiresNetwork;
            private boolean requiresFeishuAuth;
            private boolean requiresVectorReady;
            private boolean destructive;
            private final List<String> recoveryTips = new ArrayList<>();

            public Builder(String key, String label) {
                this.key = key;
                this.label = label;
            }

            public Builder scenes(String... scenes) {
                if (scenes != null) {
                    for (String scene : scenes) {
                        allowedScenes.add(normalizeScene(scene));
                    }
                }
                return this;
            }

            public Builder roles(String... roles) {
                if (roles != null) {
                    allowedRoles.addAll(Arrays.asList(roles));
                }
                return this;
            }

            public Builder requiresNetwork(boolean requiresNetwork) {
                this.requiresNetwork = requiresNetwork;
                return this;
            }

            public Builder requiresFeishuAuth(boolean requiresFeishuAuth) {
                this.requiresFeishuAuth = requiresFeishuAuth;
                return this;
            }

            public Builder requiresVectorReady(boolean requiresVectorReady) {
                this.requiresVectorReady = requiresVectorReady;
                return this;
            }

            public Builder destructive(boolean destructive) {
                this.destructive = destructive;
                return this;
            }

            public Builder tips(String... tips) {
                if (tips != null) {
                    recoveryTips.addAll(Arrays.asList(tips));
                }
                return this;
            }

            public ActionPolicy build() {
                return new ActionPolicy(this);
            }
        }
    }

    public static final class FieldProblem {
        public final String field;
        public final String message;
        public final boolean blocking;

        public FieldProblem(String field, String message, boolean blocking) {
            this.field = field;
            this.message = message;
            this.blocking = blocking;
        }
    }

    public static final class FieldRule {
        private final String name;
        private final String type;
        private final int min;
        private final int max;
        private final String pattern;
        private final String message;

        private FieldRule(String name, String type, int min, int max, String pattern, String message) {
            this.name = name;
            this.type = type;
            this.min = min;
            this.max = max;
            this.pattern = pattern;
            this.message = message;
        }

        public static FieldRule required(String name, String message) {
            return new FieldRule(name, "required", 0, 0, null, message);
        }

        public static FieldRule length(String name, int min, int max, String message) {
            return new FieldRule(name, "length", min, max, null, message);
        }

        public static FieldRule max(String name, int max, String message) {
            return new FieldRule(name, "max", 0, max, null, message);
        }

        public static FieldRule pattern(String name, String pattern, String message) {
            return new FieldRule(name, "pattern", 0, 0, pattern, message);
        }

        public static FieldRule rating(String name, String message) {
            return new FieldRule(name, "rating", 1, 5, null, message);
        }

        public static FieldRule decimalRange(String name, int min, int max, String message) {
            return new FieldRule(name, "decimal", min, max, null, message);
        }

        public static FieldRule notSecret(String name, String message) {
            return new FieldRule(name, "secret", 0, 0, null, message);
        }

        public FieldProblem validate(String raw) {
            String value = raw == null ? "" : raw.trim();
            if ("required".equals(type) && value.isEmpty()) {
                return new FieldProblem(name, message, true);
            }
            if ("length".equals(type) && !value.isEmpty() && (value.length() < min || value.length() > max)) {
                return new FieldProblem(name, message, true);
            }
            if ("max".equals(type) && value.length() > max) {
                return new FieldProblem(name, message, true);
            }
            if ("pattern".equals(type) && !value.isEmpty() && !value.matches(pattern)) {
                return new FieldProblem(name, message, true);
            }
            if ("rating".equals(type) && !value.isEmpty()) {
                try {
                    int rating = Integer.parseInt(value);
                    if (rating < min || rating > max) {
                        return new FieldProblem(name, message, true);
                    }
                } catch (NumberFormatException ex) {
                    return new FieldProblem(name, message, true);
                }
            }
            if ("decimal".equals(type) && !value.isEmpty()) {
                try {
                    double number = Double.parseDouble(value);
                    if (number < min || number > max) {
                        return new FieldProblem(name, message, true);
                    }
                } catch (NumberFormatException ex) {
                    return new FieldProblem(name, message, true);
                }
            }
            if ("secret".equals(type) && looksLikeSecret(value)) {
                return new FieldProblem(name, message, false);
            }
            return null;
        }

        private static boolean looksLikeSecret(String value) {
            String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
            return lower.contains("api-key")
                    || lower.contains("apikey")
                    || lower.contains("secret")
                    || lower.contains("token=")
                    || lower.contains("sk-");
        }
    }
}
