package com.flowmind.mobile;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String PREF = "flowmind_mobile";
    private static final int BG = Color.rgb(245, 247, 252);
    private static final int SURFACE = Color.WHITE;
    private static final int INK = Color.rgb(17, 24, 39);
    private static final int MUTED = Color.rgb(99, 113, 133);
    private static final int SOFT = Color.rgb(232, 237, 247);
    private static final int BRAND = Color.rgb(79, 92, 255);
    private static final int PURPLE = Color.rgb(139, 92, 246);
    private static final int CYAN = Color.rgb(14, 165, 233);
    private static final int GREEN = Color.rgb(18, 183, 106);
    private static final int AMBER = Color.rgb(245, 158, 11);
    private static final int RED = Color.rgb(240, 68, 56);

    private SharedPreferences prefs;
    private ApiClient api;
    private FrameLayout stage;
    private LinearLayout bottomNav;
    private final List<LinearLayout> navItems = new ArrayList<>();
    private int currentTab = 0;
    private String currentSessionId = "";
    private MobileConversationStore conversationStore;
    private LinearLayout chatMessages;
    private ScrollView chatScroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.WHITE);
        prefs = getSharedPreferences(PREF, MODE_PRIVATE);
        api = new ApiClient(prefs.getString("baseUrl", BuildConfig.DEFAULT_BASE_URL));
        api.setToken(prefs.getString("token", "mock-jwt.demo"));
        conversationStore = new MobileConversationStore(prefs);

        if (prefs.getBoolean("loggedIn", false)) {
            showShell();
        } else {
            showLogin();
        }
    }

    private void showLogin() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = vertical();
        root.setPadding(dp(22), dp(34), dp(22), dp(26));
        root.setBackgroundColor(BG);
        scroll.addView(root);

        LinearLayout hero = vertical();
        hero.setPadding(dp(22), dp(24), dp(22), dp(24));
        hero.setBackground(gradient(BRAND, PURPLE, dp(26)));
        hero.addView(text("FlowMind", 34, Color.WHITE, true));
        hero.addView(gap(8));
        hero.addView(text("AI 内容运营与知识管理移动工作台", 14, Color.argb(225, 255, 255, 255), false));
        hero.addView(gap(20));
        hero.addView(metricRow("Agent", "5 个能力", "API", "后端直连"));
        root.addView(hero, lp(-1, -2));
        root.addView(gap(18));

        LinearLayout form = glassCard();
        form.addView(text("进入演示空间", 22, INK, true));
        form.addView(text("可使用账号登录，也可以直接使用 Demo Token。", 13, MUTED, false));
        form.addView(gap(14));

        EditText baseUrl = input("后端 Base URL");
        baseUrl.setText(api.getBaseUrl());
        EditText username = input("账号");
        username.setText("admin");
        EditText password = input("密码");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setText("123456");
        form.addView(baseUrl);
        form.addView(gap(10));
        form.addView(username);
        form.addView(gap(10));
        form.addView(password);
        form.addView(gap(16));

        Button login = primaryButton("登录");
        Button demo = ghostButton("使用 Demo Token");
        form.addView(login);
        form.addView(gap(8));
        form.addView(demo);
        root.addView(form, lp(-1, -2));
        setContentView(scroll);
        animateIn(hero, 0);
        animateIn(form, 120);

        login.setOnClickListener(v -> {
            api.setBaseUrl(baseUrl.getText().toString());
            JSONObject body = new JSONObject();
            try {
                body.put("username", username.getText().toString());
                body.put("password", password.getText().toString());
            } catch (Exception ignored) {}
            login.setEnabled(false);
            api.post("/api/auth/login", body, new ApiClient.JsonCallback() {
                @Override public void onSuccess(JSONObject response) {
                    login.setEnabled(true);
                    JSONObject data = response.optJSONObject("data");
                    saveSession(baseUrl.getText().toString(), data == null ? "mock-jwt.demo" : data.optString("token", "mock-jwt.demo"));
                    showShell();
                }

                @Override public void onError(String message) {
                    login.setEnabled(true);
                    toast("登录失败：" + message);
                }
            });
        });

        demo.setOnClickListener(v -> {
            saveSession(baseUrl.getText().toString(), "mock-jwt.demo");
            showShell();
        });
    }

    private void saveSession(String baseUrl, String token) {
        api.setBaseUrl(baseUrl);
        api.setToken(token);
        prefs.edit()
                .putString("baseUrl", api.getBaseUrl())
                .putString("token", token)
                .putBoolean("loggedIn", true)
                .apply();
    }

    private void showShell() {
        LinearLayout root = vertical();
        root.setBackgroundColor(BG);

        stage = new FrameLayout(this);
        root.addView(stage, lp(-1, 0, 1));

        bottomNav = horizontal();
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setPadding(dp(8), dp(8), dp(8), dp(10));
        bottomNav.setBackground(round(Color.WHITE, dp(22), SOFT));
        root.addView(bottomNav, lp(-1, dp(78)));
        setContentView(root);

        addBottomTab("AI", "工作台", () -> showAgent());
        addBottomTab("库", "知识库", () -> showKnowledge());
        addBottomTab("创", "内容", () -> showContent());
        addBottomTab("校", "院校", () -> showSchool());
        addBottomTab("设", "设置", () -> showSettings());
        selectTab(0);
    }

    private void addBottomTab(String icon, String label, Runnable action) {
        LinearLayout item = vertical();
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(6), dp(6), dp(6), dp(6));
        TextView iconView = text(icon, 16, MUTED, true);
        iconView.setGravity(Gravity.CENTER);
        TextView labelView = text(label, 11, MUTED, true);
        labelView.setGravity(Gravity.CENTER);
        item.addView(iconView, lp(dp(34), dp(28)));
        item.addView(labelView);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1);
        p.setMargins(dp(2), 0, dp(2), 0);
        bottomNav.addView(item, p);
        int index = navItems.size();
        navItems.add(item);
        item.setOnClickListener(v -> {
            selectTab(index);
            action.run();
        });
    }

    private void selectTab(int index) {
        currentTab = index;
        for (int i = 0; i < navItems.size(); i++) {
            LinearLayout item = navItems.get(i);
            boolean selected = i == index;
            item.setBackground(selected ? gradient(BRAND, PURPLE, dp(18)) : null);
            for (int j = 0; j < item.getChildCount(); j++) {
                View child = item.getChildAt(j);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(selected ? Color.WHITE : MUTED);
                }
            }
            item.animate().scaleX(selected ? 1.04f : 1f).scaleY(selected ? 1.04f : 1f).setDuration(180).start();
        }
        if (stage.getChildCount() == 0) showAgent();
    }

    private void replace(View view) {
        stage.removeAllViews();
        stage.addView(view, lp(-1, -1));
        animateIn(view, 0);
    }

    private LinearLayout pageRoot(String title, String subtitle) {
        LinearLayout root = vertical();
        root.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout head = vertical();
        head.setPadding(dp(16), dp(16), dp(16), dp(16));
        head.setBackground(gradient(Color.rgb(238, 242, 255), Color.WHITE, dp(24)));
        TextView h = text(title, 24, INK, true);
        TextView s = text(subtitle, 13, MUTED, false);
        head.addView(h);
        head.addView(gap(6));
        head.addView(s);
        root.addView(head, lp(-1, -2));
        root.addView(gap(12));
        animateIn(head, 80);
        return root;
    }

    private void showAgent() {
        LinearLayout root = vertical();
        root.setPadding(dp(12), dp(12), dp(12), dp(12));

        LinearLayout top = horizontal();
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleBox = vertical();
        titleBox.addView(text("AI 工作台", 24, INK, true));
        titleBox.addView(text("总智能体会自动选择知识库、飞书、内容或院校能力。", 12, MUTED, false));
        top.addView(titleBox, lp(0, -2, 1));
        Button history = ghostButton("历史");
        Button fresh = ghostButton("新建");
        top.addView(history, lp(dp(68), dp(40)));
        top.addView(gapW(6));
        top.addView(fresh, lp(dp(68), dp(40)));
        top.addView(gapW(6));
        top.addView(pill("Auto Agent", BRAND, Color.WHITE));
        root.addView(top);
        root.addView(gap(10));

        HorizontalScrollView quickScroll = new HorizontalScrollView(this);
        quickScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout quick = horizontal();
        quick.addView(chip("查知识库", () -> sendAgent("根据保研知识库，期末如何速成课程论文？")));
        quick.addView(chip("生成选题", () -> sendAgent("生成 10 个保研小红书爆款选题，给出结构和标题。")));
        quick.addView(chip("院校推荐", () -> sendAgent("结合保研申请场景，推荐适合食品专业学生关注的夏令营项目。")));
        quick.addView(chip("飞书能力", () -> sendAgent("检查飞书知识库状态，并说明可以创建或读取哪些文档。")));
        quickScroll.addView(quick);
        root.addView(quickScroll, lp(-1, -2));
        root.addView(gap(10));

        chatScroll = new ScrollView(this);
        chatScroll.setFillViewport(false);
        chatMessages = vertical();
        chatMessages.setPadding(0, 0, 0, dp(10));
        chatScroll.addView(chatMessages);
        root.addView(chatScroll, lp(-1, 0, 1));
        restoreLatestConversation();

        LinearLayout composer = horizontal();
        composer.setGravity(Gravity.BOTTOM);
        composer.setPadding(dp(10), dp(10), dp(10), dp(10));
        composer.setBackground(round(Color.WHITE, dp(22), SOFT));
        EditText input = input("输入任务，例如：根据知识库总结课程论文方法");
        input.setSingleLine(false);
        input.setMinLines(1);
        input.setMaxLines(4);
        composer.addView(input, lp(0, -2, 1));
        composer.addView(gapW(8));
        Button send = primaryButton("发送");
        composer.addView(send, lp(dp(76), dp(50)));
        root.addView(composer, lp(-1, -2));

        send.setOnClickListener(v -> {
            String msg = input.getText().toString().trim();
            if (msg.isEmpty()) return;
            input.setText("");
            sendAgent(msg);
        });
        history.setOnClickListener(v -> showConversationHistory());
        fresh.setOnClickListener(v -> {
            MobileConversationStore.ConversationSnapshot snapshot = conversationStore.startNew("FlowMind conversation");
            currentSessionId = snapshot.sessionId;
            chatMessages.removeAllViews();
            addChatMessage("FlowMind", "已新建对话。你可以直接输入任务，我会优先检索向量知识库，再决定是否调用飞书、内容、院校等能力。", false);
        });
        replace(root);
    }

    private void restoreLatestConversation() {
        if (conversationStore == null || chatMessages == null) {
            return;
        }
        MobileConversationStore.ConversationSnapshot snapshot = conversationStore.loadActiveOrLatest();
        chatMessages.removeAllViews();
        if (snapshot == null || snapshot.messages.length() == 0) {
            MobileConversationStore.ConversationSnapshot created = conversationStore.startNew("FlowMind conversation");
            currentSessionId = created.sessionId;
            addChatMessage("FlowMind", "你好，我会优先检索向量知识库，再根据任务调用飞书、内容、院校等能力。", false);
            return;
        }
        currentSessionId = snapshot.sessionId;
        renderConversation(snapshot);
    }

    private void renderConversation(MobileConversationStore.ConversationSnapshot snapshot) {
        chatMessages.removeAllViews();
        if (snapshot == null || snapshot.messages.length() == 0) {
            addChatMessage("FlowMind", "这个会话还没有消息。你可以直接输入任务开始。", false);
            return;
        }
        for (int i = 0; i < snapshot.messages.length(); i++) {
            JSONObject item = snapshot.messages.optJSONObject(i);
            if (item == null) continue;
            String role = item.optString("role", "assistant");
            String content = item.optString("content", "");
            if ("user".equals(role)) {
                addChatMessage("我", content, true);
            } else if ("system".equals(role)) {
                addChatMessage("FlowMind", content, false);
            } else {
                addStoredAssistantMessage(item);
            }
        }
        scrollChatToEnd();
    }

    private void addStoredAssistantMessage(JSONObject item) {
        LinearLayout bubble = chatBubble(false);
        bubble.addView(text("FlowMind", 12, BRAND, true));
        String status = item.optString("status", "done");
        if (!status.isEmpty()) {
            bubble.addView(text("历史记录 / " + status, 12, MUTED, false));
        }
        JSONArray tools = item.optJSONArray("tools");
        String reasoning = item.optString("reasoning", "");
        if (tools != null && tools.length() > 0) {
            TextView toolView = text(traceText(tools), 12, Color.rgb(71, 84, 103), false);
            toolView.setOnClickListener(v -> showJsonDialog("工具调用详情", tools));
            bubble.addView(toolView);
        }
        if (!reasoning.isEmpty()) {
            TextView thinking = text("Thinking 已保存，点击展开", 12, Color.rgb(139, 146, 160), false);
            thinking.setOnClickListener(v -> showTextDialog("模型 Thinking", reasoning));
            bubble.addView(thinking);
        }
        bubble.addView(gap(6));
        bubble.addView(markdown(item.optString("content", "")));
        chatMessages.addView(bubble);
        animateIn(bubble, 0);
    }

    private void showConversationHistory() {
        List<MobileConversationStore.ConversationSummary> summaries = conversationStore.listSummaries();
        LinearLayout box = dialogBody();
        if (summaries.isEmpty()) {
            box.addView(emptyView("暂无历史对话。发送一条消息后，App 会自动保存最近会话。"));
            dialog("历史对话", box).show();
            return;
        }
        for (MobileConversationStore.ConversationSummary summary : summaries) {
            LinearLayout card = glassCard();
            card.addView(text(summary.title, 16, INK, true));
            card.addView(text(summary.displayTime() + " · " + summary.messageCount + " 条消息", 12, MUTED, false));
            if (!summary.compactPreview().isEmpty()) {
                card.addView(markdown(limit(summary.compactPreview(), 130)));
            }
            LinearLayout actions = horizontal();
            Button open = primaryButton("打开");
            Button delete = ghostButton("删除");
            actions.addView(open, lp(0, dp(42), 1));
            actions.addView(gapW(8));
            actions.addView(delete, lp(0, dp(42), 1));
            card.addView(actions);
            open.setOnClickListener(v -> {
                MobileConversationStore.ConversationSnapshot snapshot = conversationStore.load(summary.sessionId);
                if (snapshot != null) {
                    currentSessionId = snapshot.sessionId;
                    conversationStore.setActive(snapshot.sessionId);
                    renderConversation(snapshot);
                    toast("已打开历史对话");
                }
            });
            delete.setOnClickListener(v -> {
                conversationStore.remove(summary.sessionId);
                toast("已删除本地历史");
            });
            box.addView(card);
        }
        dialog("历史对话", box).show();
    }

    private void sendAgent(String message) {
        MobileConversationStore.ConversationSnapshot localSnapshot = conversationStore.ensureActive();
        currentSessionId = currentSessionId == null || currentSessionId.isEmpty() ? localSnapshot.sessionId : currentSessionId;
        conversationStore.appendUserMessage(currentSessionId, message);
        addChatMessage("我", message, true);

        LinearLayout bubble = chatBubble(false);
        TextView status = text("正在连接智能体...", 12, MUTED, false);
        TextView tools = text("", 12, Color.rgb(71, 84, 103), false);
        TextView reasoning = text("", 12, Color.rgb(139, 146, 160), false);
        TextView answer = markdown("");
        bubble.addView(text("FlowMind", 12, BRAND, true));
        bubble.addView(status);
        bubble.addView(tools);
        bubble.addView(reasoning);
        bubble.addView(gap(6));
        bubble.addView(answer);
        chatMessages.addView(bubble);
        scrollChatToEnd();
        animateIn(bubble, 0);

        JSONObject body = new JSONObject();
        try {
            body.put("agentType", "auto");
            body.put("message", message);
            if (!currentSessionId.isEmpty()) body.put("sessionId", currentSessionId);
            body.put("context", new JSONObject());
        } catch (Exception ignored) {}

        final String localSessionForRequest = currentSessionId;
        api.postSse("/api/agents/chat/stream", body, new ApiClient.SseCallback() {
            final StringBuilder fullAnswer = new StringBuilder();
            final StringBuilder fullReasoning = new StringBuilder();
            final JSONArray traceItems = new JSONArray();

            @Override public void onEvent(String event, JSONObject data) {
                if ("session".equals(event)) {
                    String backendSessionId = data.optString("sessionId", currentSessionId);
                    if (!backendSessionId.isEmpty() && !backendSessionId.equals(localSessionForRequest)) {
                        conversationStore.mapBackendSession(localSessionForRequest, backendSessionId);
                    }
                    currentSessionId = backendSessionId;
                } else if ("thinking".equals(event)) {
                    status.setText(data.optString("content", "正在思考..."));
                } else if ("reasoning".equals(event)) {
                    fullReasoning.append(data.optString("content", ""));
                    reasoning.setText("Thinking " + tail(fullReasoning.toString(), 120));
                } else if ("trace".equals(event)) {
                    JSONArray items = data.optJSONArray("items");
                    if (items != null) {
                        for (int i = 0; i < items.length(); i++) traceItems.put(items.opt(i));
                    }
                    tools.setText(traceText(traceItems));
                } else if ("delta".equals(event)) {
                    fullAnswer.append(data.optString("content", ""));
                    answer.setText(md(fullAnswer.toString()));
                } else if ("error".equals(event)) {
                    status.setTextColor(RED);
                    status.setText("错误：" + data.optString("message"));
                    conversationStore.appendAssistantMessage(currentSessionId, data.optString("message"), traceItems, fullReasoning.toString(), "error");
                } else if ("done".equals(event)) {
                    status.setTextColor(GREEN);
                    status.setText("回答完成");
                    if (fullReasoning.length() > 0) {
                        reasoning.setText("Thinking 已记录，点击展开");
                        reasoning.setOnClickListener(v -> showTextDialog("模型 Thinking", fullReasoning.toString()));
                    }
                    if (traceItems.length() > 0) {
                        tools.setOnClickListener(v -> showJsonDialog("工具调用详情", traceItems));
                    }
                    conversationStore.appendAssistantMessage(currentSessionId, fullAnswer.toString(), traceItems, fullReasoning.toString(), "done");
                }
                scrollChatToEnd();
            }

            @Override public void onError(String message) {
                status.setTextColor(RED);
                status.setText("请求失败：" + message);
                conversationStore.appendAssistantMessage(currentSessionId, "请求失败：" + message, traceItems, fullReasoning.toString(), "failed");
                scrollChatToEnd();
            }

            @Override public void onComplete() {}
        });
    }

    private void addChatMessage(String role, String message, boolean mine) {
        LinearLayout bubble = chatBubble(mine);
        bubble.addView(text(role, 12, mine ? Color.WHITE : BRAND, true));
        TextView body = markdown(message);
        if (mine) body.setTextColor(Color.WHITE);
        bubble.addView(body);
        chatMessages.addView(bubble);
        animateIn(bubble, 0);
        scrollChatToEnd();
    }

    private LinearLayout chatBubble(boolean mine) {
        LinearLayout box = vertical();
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setBackground(mine ? gradient(BRAND, PURPLE, dp(20)) : round(Color.WHITE, dp(20), SOFT));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams((int) (getResources().getDisplayMetrics().widthPixels * 0.86f), -2);
        p.setMargins(mine ? dp(36) : 0, 0, mine ? 0 : dp(36), dp(10));
        box.setLayoutParams(p);
        return box;
    }

    private String traceText(JSONArray items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null || "skipped".equals(item.optString("status"))) continue;
            if (sb.length() > 0) sb.append("  ");
            sb.append("工具: ").append(item.optString("name", "Tool"))
                    .append(" / ").append(item.optString("status", "done"));
        }
        return sb.toString();
    }

    private void scrollChatToEnd() {
        if (chatScroll != null) chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void showKnowledge() {
        LinearLayout root = pageRoot("知识库", "向量检索、文档卡片和知识库状态。");

        LinearLayout stats = horizontal();
        root.addView(stats, lp(-1, -2));
        loadKnowledgeStats(stats);

        LinearLayout searchBox = glassCard();
        EditText q = input("检索关键词，例如：课程论文、保研简历");
        Button search = primaryButton("语义检索");
        searchBox.addView(q);
        searchBox.addView(gap(10));
        searchBox.addView(search);
        root.addView(searchBox);

        LinearLayout results = vertical();
        root.addView(results);
        search.setOnClickListener(v -> {
            String keyword = q.getText().toString().trim();
            if (keyword.isEmpty()) keyword = "保研";
            loadVectorResults(results, keyword);
        });

        Button docs = ghostButton("加载知识库文档");
        root.addView(docs);
        docs.setOnClickListener(v -> loadKnowledgeDocs(results));
        loadKnowledgeDocs(results);
        replace(wrap(root));
    }

    private void loadKnowledgeStats(LinearLayout stats) {
        stats.removeAllViews();
        stats.addView(statCard("文档", "...", BRAND), lp(0, -2, 1));
        stats.addView(gapW(8));
        stats.addView(statCard("向量", "...", CYAN), lp(0, -2, 1));
        api.get("/api/knowledge/stats", new ApiClient.JsonCallback() {
            @Override public void onSuccess(JSONObject body) {
                stats.removeAllViews();
                JSONObject data = body.optJSONObject("data");
                stats.addView(statCard("文档", valueOf(data, "docCount", "totalDocs", "count"), BRAND), lp(0, -2, 1));
                stats.addView(gapW(8));
                stats.addView(statCard("标签", valueOf(data, "tagCount", "tags", "totalTags"), CYAN), lp(0, -2, 1));
            }
            @Override public void onError(String message) {}
        });
    }

    private void loadVectorResults(LinearLayout results, String keyword) {
        results.removeAllViews();
        results.addView(loading("正在检索向量知识库..."));
        api.get("/api/knowledge/vector/search?q=" + Uri.encode(keyword) + "&topK=8", new ApiClient.JsonCallback() {
            @Override public void onSuccess(JSONObject body) {
                results.removeAllViews();
                JSONArray arr = asArray(body.opt("data"));
                results.addView(sectionTitle("语义检索结果"));
                if (empty(arr)) {
                    arr = MobileOfflineFallbacks.vectorHits();
                    results.addView(infoCard("本地兜底", "接口没有返回向量结果，已展示移动端内置知识片段，便于课堂演示。"));
                }
                renderCards(results, arr, "title", "chunkText", item -> showJsonDialog("知识片段详情", item));
            }
            @Override public void onError(String message) {
                results.removeAllViews();
                results.addView(errorView(message));
                results.addView(infoCard("本地兜底", "向量检索接口暂时不可用，先展示内置知识片段。"));
                renderCards(results, MobileOfflineFallbacks.vectorHits(), "title", "chunkText", item -> showJsonDialog("知识片段详情", item));
            }
        });
    }

    private void loadKnowledgeDocs(LinearLayout results) {
        results.removeAllViews();
        results.addView(loading("正在加载知识库文档..."));
        api.get("/api/knowledge/docs", new ApiClient.JsonCallback() {
            @Override public void onSuccess(JSONObject body) {
                results.removeAllViews();
                JSONArray arr = asArray(body.opt("data"));
                results.addView(sectionTitle("知识库文档"));
                if (empty(arr)) {
                    arr = MobileOfflineFallbacks.knowledgeDocs();
                    results.addView(infoCard("本地兜底", "接口返回为空，已展示移动端内置知识库文档。"));
                }
                renderCards(results, arr, "title", "summary", item -> showJsonDialog("文档详情", item));
            }
            @Override public void onError(String message) {
                results.removeAllViews();
                results.addView(errorView(message));
                results.addView(infoCard("本地兜底", "知识库接口暂时不可用，先展示内置文档和标签信息。"));
                renderCards(results, MobileOfflineFallbacks.knowledgeDocs(), "title", "summary", item -> showJsonDialog("文档详情", item));
            }
        });
    }

    private void showContent() {
        LinearLayout root = pageRoot("内容创作", "主题库、文案库、日历与内容表现。");
        LinearLayout overview = horizontal();
        root.addView(overview);
        root.addView(gap(10));
        loadContentOverview(overview);

        LinearLayout tabs = horizontal();
        LinearLayout body = vertical();
        TextView themes = segment("主题库", true);
        TextView drafts = segment("文案库", false);
        TextView calendar = segment("日历", false);
        tabs.addView(themes, lp(0, dp(42), 1));
        tabs.addView(drafts, lp(0, dp(42), 1));
        tabs.addView(calendar, lp(0, dp(42), 1));
        root.addView(tabs);
        root.addView(gap(10));
        root.addView(body);

        themes.setOnClickListener(v -> {
            setSegments(tabs, 0);
            loadThemes(body);
        });
        drafts.setOnClickListener(v -> {
            setSegments(tabs, 1);
            loadDrafts(body);
        });
        calendar.setOnClickListener(v -> {
            setSegments(tabs, 2);
            loadCalendar(body);
        });
        loadThemes(body);
        replace(wrap(root));
    }

    private void loadContentOverview(LinearLayout overview) {
        overview.removeAllViews();
        overview.addView(statCard("主题", "...", BRAND), lp(0, -2, 1));
        overview.addView(gapW(8));
        overview.addView(statCard("文案", "...", PURPLE), lp(0, -2, 1));
        api.get("/api/content/themes", new ApiClient.JsonCallback() {
            @Override public void onSuccess(JSONObject themes) {
                api.get("/api/content/drafts", new ApiClient.JsonCallback() {
                    @Override public void onSuccess(JSONObject drafts) {
                        overview.removeAllViews();
                        overview.addView(statCard("主题", String.valueOf(asArray(themes.opt("data")).length()), BRAND), lp(0, -2, 1));
                        overview.addView(gapW(8));
                        overview.addView(statCard("文案", String.valueOf(asArray(drafts.opt("data")).length()), PURPLE), lp(0, -2, 1));
                    }
                    @Override public void onError(String message) {}
                });
            }
            @Override public void onError(String message) {}
        });
    }

    private void loadThemes(LinearLayout body) {
        body.removeAllViews();
        body.addView(loading("正在加载主题库..."));
        api.get("/api/content/themes", new ApiClient.JsonCallback() {
            @Override public void onSuccess(JSONObject response) {
                body.removeAllViews();
                JSONArray arr = asArray(response.opt("data"));
                body.addView(sectionTitle("主题库"));
                if (empty(arr)) body.addView(emptyView("暂无主题。"));
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.optJSONObject(i);
                    if (item == null) continue;
                    LinearLayout card = richContentCard(item, "title", "summary", true);
                    card.setOnClickListener(v -> openThemeDetail(item));
                    body.addView(card);
                    animateIn(card, i * 35);
                }
            }
            @Override public void onError(String message) {
                body.removeAllViews();
                body.addView(errorView(message));
            }
        });
    }

    private void loadDrafts(LinearLayout body) {
        body.removeAllViews();
        body.addView(loading("正在加载文案库..."));
        api.get("/api/content/drafts", new ApiClient.JsonCallback() {
            @Override public void onSuccess(JSONObject response) {
                body.removeAllViews();
                JSONArray arr = asArray(response.opt("data"));
                body.addView(sectionTitle("文案库"));
                if (empty(arr)) body.addView(emptyView("暂无文案。"));
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.optJSONObject(i);
                    if (item == null) continue;
                    LinearLayout card = richContentCard(item, "title", "content", false);
                    card.setOnClickListener(v -> openDraftDetail(item));
                    body.addView(card);
                    animateIn(card, i * 35);
                }
            }
            @Override public void onError(String message) {
                body.removeAllViews();
                body.addView(errorView(message));
            }
        });
    }

    private void loadCalendar(LinearLayout body) {
        body.removeAllViews();
        body.addView(loading("正在加载内容日历..."));
        String month = new SimpleDateFormat("yyyy-MM", Locale.CHINA).format(new Date());
        api.get("/api/content/calendar?month=" + month, new ApiClient.JsonCallback() {
            @Override public void onSuccess(JSONObject response) {
                body.removeAllViews();
                JSONArray arr = asArray(response.opt("data"));
                body.addView(sectionTitle("本月内容日历"));
                body.addView(new CalendarBoard(arr), lp(-1, dp(330)));
                body.addView(gap(10));
                body.addView(sectionTitle("发布列表"));
                renderCards(body, arr, "title", "publishDate", item -> showJsonDialog("发布详情", item));
            }
            @Override public void onError(String message) {
                body.removeAllViews();
                body.addView(errorView(message));
            }
        });
    }

    private void openThemeDetail(JSONObject item) {
        LinearLayout box = dialogBody();
        box.addView(text(item.optString("title", "主题详情"), 21, INK, true));
        box.addView(gap(8));
        box.addView(stars(item.optInt("rating", 0), rating -> rateTheme(item.optLong("id"), rating)));
        box.addView(gap(8));
        box.addView(markdown(firstNonEmpty(item, "summary", "description", "content")));
        box.addView(gap(10));
        box.addView(pill("状态 " + firstNonEmpty(item, "status", "channel"), BRAND, Color.WHITE));
        box.addView(gap(12));
        Button drafts = primaryButton("查看该主题历史文案");
        box.addView(drafts);
        AlertDialog dialog = dialog("主题详情", box);
        drafts.setOnClickListener(v -> {
            dialog.dismiss();
            loadThemeDrafts(item.optLong("id"), item.optString("title", "主题"));
        });
        dialog.show();
    }

    private void loadThemeDrafts(long themeId, String title) {
        LinearLayout box = dialogBody();
        box.addView(loading("正在加载历史文案..."));
        AlertDialog dialog = dialog(title + " · 历史文案", box);
        dialog.show();
        api.get("/api/content/themes/" + themeId + "/drafts", new ApiClient.JsonCallback() {
            @Override public void onSuccess(JSONObject response) {
                box.removeAllViews();
                JSONArray arr = asArray(response.opt("data"));
                if (empty(arr)) box.addView(emptyView("这个主题下暂无历史文案。"));
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.optJSONObject(i);
                    if (item == null) continue;
                    LinearLayout card = richContentCard(item, "title", "content", false);
                    card.setOnClickListener(v -> openDraftDetail(item));
                    box.addView(card);
                }
            }
            @Override public void onError(String message) {
                box.removeAllViews();
                box.addView(errorView(message));
            }
        });
    }

    private void openDraftDetail(JSONObject item) {
        LinearLayout box = dialogBody();
        box.addView(text(item.optString("title", "文案详情"), 21, INK, true));
        box.addView(gap(8));
        box.addView(stars(item.optInt("rating", 0), rating -> rateDraft(item.optLong("id"), rating)));
        box.addView(gap(10));
        String image = firstNonEmpty(item, "imageUrl", "coverUrl", "image");
        if (!image.isEmpty()) {
            box.addView(pill("配图 " + image, CYAN, Color.WHITE));
            box.addView(gap(8));
        } else {
            String suggestion = firstNonEmpty(item, "imageSuggestion", "imageAdvice", "pictureSuggestion");
            box.addView(pill("配图建议 " + (suggestion.isEmpty() ? "使用校园、录取通知、书桌复盘类图片" : suggestion), AMBER, Color.WHITE));
            box.addView(gap(8));
        }
        box.addView(markdown(firstNonEmpty(item, "content", "body", "text")));
        box.addView(gap(10));
        box.addView(text("使用状态：" + firstNonEmpty(item, "usageStatus", "usedStatus", "status"), 13, MUTED, false));
        box.addView(text("发布日期：" + firstNonEmpty(item, "usedDate", "publishDate", "publishedAt"), 13, MUTED, false));
        box.addView(gap(12));
        LinearLayout actions = horizontal();
        Button edit = primaryButton("编辑");
        Button upload = ghostButton("登记图片");
        actions.addView(edit, lp(0, dp(48), 1));
        actions.addView(gapW(8));
        actions.addView(upload, lp(0, dp(48), 1));
        box.addView(actions);
        AlertDialog dialog = dialog("文案详情", box);
        edit.setOnClickListener(v -> openDraftEditor(item));
        upload.setOnClickListener(v -> openImageUrlDialog(item.optLong("id")));
        dialog.show();
    }

    private void openDraftEditor(JSONObject item) {
        LinearLayout box = dialogBody();
        EditText title = input("标题");
        title.setText(item.optString("title"));
        EditText content = input("文案内容");
        content.setSingleLine(false);
        content.setMinLines(8);
        content.setText(firstNonEmpty(item, "content", "body", "text"));
        box.addView(title);
        box.addView(gap(10));
        box.addView(content);
        box.addView(gap(12));
        Button save = primaryButton("保存修改");
        box.addView(save);
        AlertDialog dialog = dialog("编辑文案", box);
        save.setOnClickListener(v -> {
            JSONObject body = new JSONObject();
            try {
                body.put("title", title.getText().toString());
                body.put("content", content.getText().toString());
            } catch (Exception ignored) {}
            api.put("/api/content/drafts/" + item.optLong("id"), body, new ApiClient.JsonCallback() {
                @Override public void onSuccess(JSONObject response) {
                    toast("已保存");
                    dialog.dismiss();
                }
                @Override public void onError(String message) {
                    toast("保存失败：" + message);
                }
            });
        });
        dialog.show();
    }

    private void openImageUrlDialog(long draftId) {
        LinearLayout box = dialogBody();
        EditText url = input("图片 URL");
        box.addView(url);
        box.addView(gap(12));
        Button save = primaryButton("保存图片引用");
        box.addView(save);
        AlertDialog dialog = dialog("登记文案配图", box);
        save.setOnClickListener(v -> {
            JSONObject body = new JSONObject();
            try {
                body.put("imageUrl", url.getText().toString());
            } catch (Exception ignored) {}
            api.post("/api/content/drafts/" + draftId + "/images", body, new ApiClient.JsonCallback() {
                @Override public void onSuccess(JSONObject response) {
                    toast("已保存图片引用");
                    dialog.dismiss();
                }
                @Override public void onError(String message) {
                    toast("保存失败：" + message);
                }
            });
        });
        dialog.show();
    }

    private void rateTheme(long id, int rating) {
        JSONObject body = new JSONObject();
        try { body.put("rating", rating); } catch (Exception ignored) {}
        api.put("/api/content/themes/" + id + "/rating", body, simpleToast("主题评分已更新"));
    }

    private void rateDraft(long id, int rating) {
        JSONObject body = new JSONObject();
        try { body.put("rating", rating); } catch (Exception ignored) {}
        api.put("/api/content/drafts/" + id + "/rating", body, simpleToast("文案评分已更新"));
    }

    private void showSchool() {
        LinearLayout root = pageRoot("院校情报", "学校库、夏令营项目和截止趋势。");
        LinearLayout chartCard = glassCard();
        chartCard.addView(text("项目截止趋势", 16, INK, true));
        chartCard.addView(new MiniBarChartView(new float[]{3, 5, 8, 6, 4}, new int[]{BRAND, PURPLE, CYAN, GREEN, AMBER}), lp(-1, dp(150)));
        root.addView(chartCard);

        LinearLayout tabs = horizontal();
        LinearLayout body = vertical();
        TextView schools = segment("学校", true);
        TextView projects = segment("项目", false);
        TextView recommend = segment("推荐", false);
        tabs.addView(schools, lp(0, dp(42), 1));
        tabs.addView(projects, lp(0, dp(42), 1));
        tabs.addView(recommend, lp(0, dp(42), 1));
        root.addView(tabs);
        root.addView(gap(10));
        root.addView(body);

        schools.setOnClickListener(v -> {
            setSegments(tabs, 0);
            loadSchools(body);
        });
        projects.setOnClickListener(v -> {
            setSegments(tabs, 1);
            loadProjects(body);
        });
        recommend.setOnClickListener(v -> {
            setSegments(tabs, 2);
            showRecommendForm(body);
        });
        loadSchools(body);
        replace(wrap(root));
    }

    private void loadSchools(LinearLayout body) {
        body.removeAllViews();
        body.addView(loading("正在加载学校列表..."));
        api.get("/api/schools", new ApiClient.JsonCallback() {
            @Override public void onSuccess(JSONObject response) {
                body.removeAllViews();
                body.addView(sectionTitle("学校列表"));
                JSONArray arr = asArray(response.opt("data"));
                if (empty(arr)) {
                    arr = MobileSchoolMockData.schools();
                    body.addView(infoCard("本地兜底", "学校接口返回为空，已展示移动端内置院校情报。"));
                }
                renderCards(body, arr, "name", "summary", item -> showJsonDialog("学校详情", item));
            }
            @Override public void onError(String message) {
                body.removeAllViews();
                body.addView(errorView(message));
                body.addView(infoCard("本地兜底", "学校接口暂时不可用，先展示移动端内置院校情报。"));
                renderCards(body, MobileSchoolMockData.schools(), "name", "summary", item -> showJsonDialog("学校详情", item));
            }
        });
    }

    private void loadProjects(LinearLayout body) {
        body.removeAllViews();
        body.addView(loading("正在加载院校项目..."));
        api.get("/api/school-projects", new ApiClient.JsonCallback() {
            @Override public void onSuccess(JSONObject response) {
                body.removeAllViews();
                body.addView(sectionTitle("夏令营与预推免项目"));
                JSONArray arr = asArray(response.opt("data"));
                if (empty(arr)) {
                    arr = MobileSchoolMockData.projects();
                    body.addView(infoCard("本地兜底", "项目接口返回为空，已展示移动端内置夏令营与预推免数据。"));
                }
                renderCards(body, arr, "projectName", "requirements", item -> showJsonDialog("项目详情", item));
            }
            @Override public void onError(String message) {
                body.removeAllViews();
                body.addView(errorView(message));
                body.addView(infoCard("本地兜底", "院校项目接口暂时不可用，先展示移动端内置项目数据。"));
                renderCards(body, MobileSchoolMockData.projects(), "projectName", "requirements", item -> showJsonDialog("项目详情", item));
            }
        });
    }

    private void showRecommendForm(LinearLayout body) {
        body.removeAllViews();
        LinearLayout form = glassCard();
        EditText profile = input("输入学生背景：GPA、排名、英语、科研、目标方向");
        profile.setSingleLine(false);
        profile.setMinLines(5);
        profile.setText("GPA 3.75，专业排名前 10%，六级 560，有食品科研经历，目标 985 食品/生物方向。");
        Button run = primaryButton("AI 匹配推荐");
        form.addView(profile);
        form.addView(gap(12));
        form.addView(run);
        body.addView(form);
        LinearLayout results = vertical();
        body.addView(results);
        run.setOnClickListener(v -> {
            results.removeAllViews();
            results.addView(loading("正在匹配院校项目..."));
            JSONObject req = new JSONObject();
            try { req.put("profile", profile.getText().toString()); } catch (Exception ignored) {}
            api.post("/api/schools/recommend", req, new ApiClient.JsonCallback() {
                @Override public void onSuccess(JSONObject response) {
                    results.removeAllViews();
                    Object data = response.opt("data");
                    if (data == null || JSONObject.NULL.equals(data)) {
                        data = MobileSchoolMockData.recommendation(profile.getText().toString());
                        results.addView(infoCard("本地兜底", "推荐接口返回为空，已展示移动端内置推荐逻辑。"));
                    }
                    results.addView(jsonCard("推荐结果", data));
                }
                @Override public void onError(String message) {
                    results.removeAllViews();
                    results.addView(errorView(message));
                    results.addView(infoCard("本地兜底", "推荐接口暂时不可用，先展示移动端内置匹配建议。"));
                    results.addView(jsonCard("推荐结果", MobileSchoolMockData.recommendation(profile.getText().toString())));
                }
            });
        });
    }

    private void showSettings() {
        LinearLayout root = pageRoot("设置", "后端地址、账号状态、飞书与同步工具入口。");
        LinearLayout form = glassCard();
        EditText base = input("Base URL");
        base.setText(api.getBaseUrl());
        Button save = primaryButton("保存后端地址");
        Button me = ghostButton("检查当前用户");
        Button feishu = ghostButton("检查飞书同步状态");
        Button logout = ghostButton("退出登录");
        form.addView(base);
        form.addView(gap(10));
        form.addView(save);
        form.addView(gap(8));
        form.addView(me);
        form.addView(gap(8));
        form.addView(feishu);
        form.addView(gap(8));
        form.addView(logout);
        root.addView(form);

        LinearLayout output = vertical();
        root.addView(output);
        save.setOnClickListener(v -> {
            api.setBaseUrl(base.getText().toString());
            prefs.edit().putString("baseUrl", api.getBaseUrl()).apply();
            toast("已保存：" + api.getBaseUrl());
        });
        me.setOnClickListener(v -> loadJson(output, "/api/users/me", "当前用户"));
        feishu.setOnClickListener(v -> loadJson(output, "/api/feishu/sync/status", "飞书同步状态"));
        logout.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            showLogin();
        });

        root.addView(sectionTitle("移动端说明"));
        root.addView(infoCard("接口复用", "App 只通过 HTTPS/SSE 调用后端服务，不直接访问数据库、Weaviate 或飞书 CLI。"));
        root.addView(infoCard("当前后端", api.getBaseUrl()));
        replace(wrap(root));
    }

    private void loadJson(LinearLayout output, String path, String title) {
        output.removeAllViews();
        output.addView(loading("正在请求 " + title + "..."));
        api.get(path, new ApiClient.JsonCallback() {
            @Override public void onSuccess(JSONObject body) {
                output.removeAllViews();
                output.addView(jsonCard(title, body.opt("data")));
            }
            @Override public void onError(String message) {
                output.removeAllViews();
                output.addView(errorView(message));
            }
        });
    }

    private void renderCards(LinearLayout root, JSONArray arr, String titleKey, String bodyKey, ItemClick click) {
        if (empty(arr)) {
            root.addView(emptyView("暂无数据。"));
            return;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item == null) continue;
            LinearLayout card = glassCard();
            card.addView(text(firstNonEmpty(item, titleKey, "title", "name", "projectName"), 16, INK, true));
            String body = firstNonEmpty(item, bodyKey, "summary", "content", "description", "requirements");
            if (!body.isEmpty()) card.addView(markdown(limit(body, 160)));
            String meta = firstNonEmpty(item, "status", "channel", "deadline", "publishDate", "source");
            if (!meta.isEmpty()) {
                card.addView(gap(6));
                card.addView(pill(meta, BRAND, Color.WHITE));
            }
            addSmartMetadata(card, item);
            card.setOnClickListener(v -> click.open(item));
            root.addView(card);
            animateIn(card, i * 35);
        }
    }

    private void addSmartMetadata(LinearLayout card, JSONObject item) {
        String tagText = MobileRichTextRenderer.tagText(item);
        if (!tagText.isEmpty()) {
            card.addView(gap(6));
            card.addView(wrapTags(tagText));
        }
        String schoolMeta = MobileRichTextRenderer.compactSchool(item);
        String knowledgeMeta = MobileRichTextRenderer.compactKnowledge(item);
        boolean looksSchool = !firstNonEmpty(item, "schoolName", "projectName", "deadline", "materials", "matchScore", "level").isEmpty();
        boolean looksKnowledge = !firstNonEmpty(item, "source", "category", "chunkText", "matchedField", "distance", "similarity").isEmpty();
        String detail = looksSchool ? schoolMeta : looksKnowledge ? knowledgeMeta : "";
        if (!detail.isEmpty()) {
            TextView detailView = markdown(limit(detail, 420));
            detailView.setTextColor(Color.rgb(71, 84, 103));
            card.addView(gap(6));
            card.addView(detailView);
        }
    }

    private View wrapTags(String tagText) {
        LinearLayout row = horizontal();
        row.setPadding(0, 0, 0, 0);
        String[] parts = tagText.split("[,，/、 ]+");
        int count = 0;
        for (String part : parts) {
            String value = part == null ? "" : part.trim();
            if (value.isEmpty()) continue;
            row.addView(pill(value, CYAN, Color.WHITE));
            row.addView(gapW(6));
            count++;
            if (count >= 4) break;
        }
        if (count == 0) {
            row.addView(pill(tagText, CYAN, Color.WHITE));
        }
        return row;
    }

    private LinearLayout richContentCard(JSONObject item, String titleKey, String bodyKey, boolean theme) {
        LinearLayout card = glassCard();
        LinearLayout top = horizontal();
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout texts = vertical();
        texts.addView(text(firstNonEmpty(item, titleKey, "title", "name"), 17, INK, true));
        texts.addView(starsStatic(item.optInt("rating", 0)));
        top.addView(texts, lp(0, -2, 1));
        top.addView(pill(theme ? firstNonEmpty(item, "channel", "status") : firstNonEmpty(item, "usageStatus", "status"), theme ? BRAND : PURPLE, Color.WHITE));
        card.addView(top);
        card.addView(gap(8));
        card.addView(markdown(limit(firstNonEmpty(item, bodyKey, "summary", "content", "description"), 170)));
        String image = firstNonEmpty(item, "imageUrl", "coverUrl", "image");
        String suggestion = firstNonEmpty(item, "imageSuggestion", "imageAdvice", "pictureSuggestion");
        if (!image.isEmpty() || !suggestion.isEmpty()) {
            card.addView(gap(8));
            card.addView(pill(image.isEmpty() ? "配图建议" : "含配图", image.isEmpty() ? AMBER : CYAN, Color.WHITE));
        }
        return card;
    }

    private LinearLayout statCard(String title, String value, int color) {
        LinearLayout card = vertical();
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(round(Color.WHITE, dp(18), SOFT));
        card.addView(text(title, 12, MUTED, false));
        card.addView(text(value == null || value.isEmpty() ? "0" : value, 24, color, true));
        return card;
    }

    private View infoCard(String title, String body) {
        LinearLayout card = glassCard();
        card.addView(text(title, 16, INK, true));
        card.addView(text(body, 13, MUTED, false));
        return card;
    }

    private View jsonCard(String title, Object data) {
        LinearLayout card = glassCard();
        card.addView(text(title, 16, INK, true));
        card.addView(markdown(MobileRichTextRenderer.jsonToMarkdown(data)));
        card.setOnClickListener(v -> {
            if (data instanceof JSONObject) showJsonDialog(title, (JSONObject) data);
            else if (data instanceof JSONArray) showJsonDialog(title, (JSONArray) data);
            else showTextDialog(title, String.valueOf(data));
        });
        return card;
    }

    private TextView sectionTitle(String value) {
        TextView tv = text(value, 18, INK, true);
        tv.setPadding(dp(2), dp(14), dp(2), dp(8));
        return tv;
    }

    private View emptyView(String msg) {
        TextView tv = text(msg, 13, MUTED, false);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(18), dp(20), dp(18), dp(20));
        tv.setBackground(round(Color.WHITE, dp(18), SOFT));
        return tv;
    }

    private View errorView(String msg) {
        TextView tv = text("请求失败：" + msg, 13, RED, true);
        tv.setPadding(dp(14), dp(14), dp(14), dp(14));
        tv.setBackground(round(Color.rgb(255, 244, 244), dp(18), Color.rgb(255, 205, 210)));
        return tv;
    }

    private View loading(String msg) {
        LinearLayout box = horizontal();
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        box.setBackground(round(Color.WHITE, dp(18), SOFT));
        ProgressBar bar = new ProgressBar(this);
        box.addView(bar, lp(dp(32), dp(32)));
        TextView tv = text(msg, 13, MUTED, false);
        tv.setPadding(dp(10), 0, 0, 0);
        box.addView(tv);
        return box;
    }

    private LinearLayout metricRow(String a, String av, String b, String bv) {
        LinearLayout row = horizontal();
        row.addView(heroMetric(a, av), lp(0, -2, 1));
        row.addView(gapW(10));
        row.addView(heroMetric(b, bv), lp(0, -2, 1));
        return row;
    }

    private LinearLayout heroMetric(String title, String value) {
        LinearLayout box = vertical();
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackground(round(Color.argb(42, 255, 255, 255), dp(16), Color.argb(70, 255, 255, 255)));
        box.addView(text(title, 12, Color.argb(210, 255, 255, 255), false));
        box.addView(text(value, 16, Color.WHITE, true));
        return box;
    }

    private TextView segment(String label, boolean selected) {
        TextView tv = text(label, 14, selected ? Color.WHITE : MUTED, true);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(selected ? gradient(BRAND, PURPLE, dp(16)) : round(Color.WHITE, dp(16), SOFT));
        return tv;
    }

    private void setSegments(LinearLayout tabs, int selected) {
        for (int i = 0; i < tabs.getChildCount(); i++) {
            TextView tv = (TextView) tabs.getChildAt(i);
            boolean on = i == selected;
            tv.setTextColor(on ? Color.WHITE : MUTED);
            tv.setBackground(on ? gradient(BRAND, PURPLE, dp(16)) : round(Color.WHITE, dp(16), SOFT));
            tv.animate().scaleX(on ? 1.02f : 1f).scaleY(on ? 1.02f : 1f).setDuration(160).start();
        }
    }

    private TextView chip(String label, Runnable action) {
        TextView tv = text(label, 13, BRAND, true);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(14), dp(9), dp(14), dp(9));
        tv.setBackground(round(Color.WHITE, dp(999), Color.rgb(211, 218, 255)));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.setMargins(0, 0, dp(8), 0);
        tv.setLayoutParams(p);
        tv.setOnClickListener(v -> action.run());
        return tv;
    }

    private TextView pill(String label, int bg, int fg) {
        TextView tv = text(label == null || label.isEmpty() ? "未设置" : label, 11, fg, true);
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine(true);
        tv.setPadding(dp(9), dp(5), dp(9), dp(5));
        tv.setBackground(round(bg, dp(999), 0));
        return tv;
    }

    private LinearLayout starsStatic(int rating) {
        LinearLayout row = horizontal();
        for (int i = 1; i <= 5; i++) {
            TextView star = text(i <= rating ? "★" : "☆", 18, AMBER, true);
            row.addView(star);
        }
        return row;
    }

    private LinearLayout stars(int rating, RatingClick click) {
        LinearLayout row = horizontal();
        for (int i = 1; i <= 5; i++) {
            final int value = i;
            TextView star = text(i <= rating ? "★" : "☆", 26, AMBER, true);
            star.setPadding(0, 0, dp(4), 0);
            star.setOnClickListener(v -> click.rate(value));
            row.addView(star);
        }
        return row;
    }

    private ApiClient.JsonCallback simpleToast(String success) {
        return new ApiClient.JsonCallback() {
            @Override public void onSuccess(JSONObject body) { toast(success); }
            @Override public void onError(String message) { toast("操作失败：" + message); }
        };
    }

    private AlertDialog dialog(String title, View content) {
        return new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(wrap(content))
                .setPositiveButton("关闭", null)
                .create();
    }

    private LinearLayout dialogBody() {
        LinearLayout box = vertical();
        box.setPadding(dp(4), dp(4), dp(4), dp(4));
        return box;
    }

    private void showTextDialog(String title, String value) {
        TextView tv = markdown(value == null ? "" : value);
        tv.setPadding(dp(8), dp(8), dp(8), dp(8));
        dialog(title, tv).show();
    }

    private void showJsonDialog(String title, Object json) {
        showTextDialog(title, MobileRichTextRenderer.jsonToMarkdown(json));
    }

    private TextView markdown(String value) {
        TextView tv = text("", 14, INK, false);
        tv.setText(md(value == null ? "" : value));
        return tv;
    }

    private Spanned md(String raw) {
        return MobileRichTextRenderer.render(raw);
    }

    private LinearLayout glassCard() {
        LinearLayout layout = vertical();
        layout.setPadding(dp(14), dp(14), dp(14), dp(14));
        layout.setBackground(round(Color.WHITE, dp(20), SOFT));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 0, 0, dp(10));
        layout.setLayoutParams(p);
        layout.setElevation(dp(1));
        return layout;
    }

    private EditText input(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextColor(INK);
        input.setHintTextColor(Color.rgb(152, 162, 179));
        input.setTextSize(14);
        input.setSingleLine(true);
        input.setPadding(dp(13), dp(8), dp(13), dp(8));
        input.setBackground(round(Color.WHITE, dp(15), Color.rgb(223, 229, 242)));
        return input;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        button.setBackground(gradient(BRAND, PURPLE, dp(15)));
        return button;
    }

    private Button ghostButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(BRAND);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        button.setBackground(round(Color.WHITE, dp(15), Color.rgb(211, 218, 255)));
        return button;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setLineSpacing(dp(2), 1.0f);
        if (bold) tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private ScrollView wrap(View child) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(child);
        return scroll;
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private View gap(int dp) {
        Space space = new Space(this);
        space.setLayoutParams(lp(1, dp(dp)));
        return space;
    }

    private View gapW(int dp) {
        Space space = new Space(this);
        space.setLayoutParams(lp(dp(dp), 1));
        return space;
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    private LinearLayout.LayoutParams lp(int w, int h, float weight) {
        return new LinearLayout.LayoutParams(w, h, weight);
    }

    private GradientDrawable round(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (stroke != 0) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private GradientDrawable gradient(int start, int end, int radius) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private void animateIn(View view, int delay) {
        view.setAlpha(0f);
        view.setTranslationY(dp(14));
        view.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(320).setInterpolator(new DecelerateInterpolator()).start();
        ObjectAnimator.ofFloat(view, "scaleX", 0.985f, 1f).setDuration(260).start();
        ObjectAnimator.ofFloat(view, "scaleY", 0.985f, 1f).setDuration(260).start();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String tail(String text, int len) {
        if (text == null || text.length() <= len) return text == null ? "" : text;
        return text.substring(text.length() - len);
    }

    private String limit(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }

    private boolean empty(JSONArray arr) {
        return arr == null || arr.length() == 0;
    }

    private JSONArray asArray(Object data) {
        if (data instanceof JSONArray) return (JSONArray) data;
        if (data instanceof JSONObject) {
            JSONObject obj = (JSONObject) data;
            JSONArray arr = obj.optJSONArray("records");
            if (arr != null) return arr;
            arr = obj.optJSONArray("items");
            if (arr != null) return arr;
            arr = obj.optJSONArray("list");
            if (arr != null) return arr;
            arr = obj.optJSONArray("data");
            if (arr != null) return arr;
        }
        return new JSONArray();
    }

    private String firstNonEmpty(JSONObject item, String... keys) {
        if (item == null) return "";
        for (String key : keys) {
            String value = item.optString(key, "");
            if (value != null && !value.trim().isEmpty() && !"null".equalsIgnoreCase(value)) return value;
        }
        return "";
    }

    private String valueOf(JSONObject obj, String... keys) {
        if (obj == null) return "0";
        for (String key : keys) {
            if (obj.has(key)) return String.valueOf(obj.opt(key));
        }
        return "0";
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private interface ItemClick {
        void open(JSONObject item);
    }

    private interface RatingClick {
        void rate(int rating);
    }

    private class MiniBarChartView extends View {
        private final float[] values;
        private final int[] colors;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        MiniBarChartView(float[] values, int[] colors) {
            super(MainActivity.this);
            this.values = values;
            this.colors = colors;
            setPadding(dp(8), dp(8), dp(8), dp(8));
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float max = 1;
            for (float v : values) if (v > max) max = v;
            float width = getWidth() - getPaddingLeft() - getPaddingRight();
            float height = getHeight() - getPaddingTop() - getPaddingBottom();
            float gap = dp(10);
            float barW = (width - gap * (values.length - 1)) / values.length;
            for (int i = 0; i < values.length; i++) {
                paint.setColor(colors[i % colors.length]);
                float barH = height * (values[i] / max);
                float left = getPaddingLeft() + i * (barW + gap);
                float top = getPaddingTop() + height - barH;
                canvas.drawRoundRect(left, top, left + barW, getPaddingTop() + height, dp(8), dp(8), paint);
            }
        }
    }

    private class CalendarBoard extends View {
        private final Map<Integer, Integer> dayCounts = new LinkedHashMap<>();
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        CalendarBoard(JSONArray items) {
            super(MainActivity.this);
            if (items != null) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.optJSONObject(i);
                    if (item == null) continue;
                    String date = firstNonEmpty(item, "publishDate", "date", "scheduledDate");
                    if (date.length() >= 10) {
                        try {
                            int day = Integer.parseInt(date.substring(8, 10));
                            dayCounts.put(day, dayCounts.containsKey(day) ? dayCounts.get(day) + 1 : 1);
                        } catch (Exception ignored) {}
                    }
                }
            }
            setBackground(round(Color.WHITE, dp(20), SOFT));
            setOnClickListener(v -> toast("下方列表可查看具体发布内容"));
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int cols = 7;
            float cellW = getWidth() / (float) cols;
            float cellH = getHeight() / 6f;
            paint.setTextAlign(Paint.Align.CENTER);
            for (int day = 1; day <= 31; day++) {
                int index = day - 1;
                float cx = (index % cols) * cellW + cellW / 2f;
                float cy = (index / cols) * cellH + cellH / 2f;
                boolean has = dayCounts.containsKey(day);
                paint.setColor(has ? Color.rgb(238, 242, 255) : Color.TRANSPARENT);
                canvas.drawRoundRect(cx - cellW * 0.38f, cy - cellH * 0.32f, cx + cellW * 0.38f, cy + cellH * 0.32f, dp(10), dp(10), paint);
                paint.setColor(has ? BRAND : MUTED);
                paint.setTextSize(dp(13));
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                canvas.drawText(String.valueOf(day), cx, cy + dp(4), paint);
                if (has) {
                    paint.setColor(PURPLE);
                    canvas.drawCircle(cx, cy + cellH * 0.24f, dp(3), paint);
                }
            }
        }
    }
}
