package com.flowmind.mobile;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public final class MobileUiKit {
    public static final int BG = Color.rgb(245, 247, 252);
    public static final int SURFACE = Color.WHITE;
    public static final int INK = Color.rgb(17, 24, 39);
    public static final int MUTED = Color.rgb(99, 113, 133);
    public static final int SOFT = Color.rgb(232, 237, 247);
    public static final int BRAND = Color.rgb(79, 92, 255);
    public static final int PURPLE = Color.rgb(139, 92, 246);
    public static final int CYAN = Color.rgb(14, 165, 233);
    public static final int GREEN = Color.rgb(18, 183, 106);
    public static final int AMBER = Color.rgb(245, 158, 11);
    public static final int RED = Color.rgb(240, 68, 56);

    private MobileUiKit() {
    }

    public static LinearLayout vertical(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    public static LinearLayout horizontal(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    public static TextView text(Context context, String value, int sp, int color, boolean bold) {
        TextView tv = new TextView(context);
        tv.setText(value);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setLineSpacing(dp(context, 2), 1.0f);
        if (bold) tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    public static TextView markdown(Context context, String value) {
        TextView tv = text(context, "", 14, INK, false);
        tv.setText(md(value));
        return tv;
    }

    public static Spanned md(String raw) {
        String text = MobileGuardrails.normalizeMarkdown(raw);
        String html = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        html = html.replaceAll("(?m)^###\\s*(.+)$", "<b>$1</b><br/>")
                .replaceAll("(?m)^##\\s*(.+)$", "<h3>$1</h3>")
                .replaceAll("(?m)^#\\s*(.+)$", "<h2>$1</h2>")
                .replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>")
                .replaceAll("`([^`]+)`", "<font color='#4F5CFF'>$1</font>")
                .replaceAll("(?m)^[-*]\\s+(.+)$", "• $1<br/>")
                .replace("\n", "<br/>");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        }
        return Html.fromHtml(html);
    }

    public static LinearLayout page(Context context, String title, String subtitle) {
        LinearLayout root = vertical(context);
        root.setPadding(dp(context, 14), dp(context, 14), dp(context, 14), dp(context, 14));
        LinearLayout header = card(context);
        header.setBackground(gradient(Color.rgb(238, 242, 255), Color.WHITE, dp(context, 24)));
        header.addView(text(context, title, 24, INK, true));
        header.addView(gap(context, 6));
        header.addView(text(context, subtitle, 13, MUTED, false));
        root.addView(header, matchWrap());
        root.addView(gap(context, 12));
        return root;
    }

    public static LinearLayout card(Context context) {
        LinearLayout layout = vertical(context);
        layout.setPadding(dp(context, 14), dp(context, 14), dp(context, 14), dp(context, 14));
        layout.setBackground(round(Color.WHITE, dp(context, 20), SOFT, context));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 0, 0, dp(context, 10));
        layout.setLayoutParams(p);
        layout.setElevation(dp(context, 1));
        return layout;
    }

    public static LinearLayout compactCard(Context context) {
        LinearLayout layout = vertical(context);
        layout.setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12));
        layout.setBackground(round(Color.WHITE, dp(context, 16), SOFT, context));
        return layout;
    }

    public static TextView pill(Context context, String label, int bg, int fg) {
        TextView tv = text(context, label == null || label.isEmpty() ? "未设置" : label, 11, fg, true);
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine(true);
        tv.setPadding(dp(context, 9), dp(context, 5), dp(context, 9), dp(context, 5));
        tv.setBackground(round(bg, dp(context, 999), 0, context));
        return tv;
    }

    public static TextView chip(Context context, String label, View.OnClickListener listener) {
        TextView tv = text(context, label, 13, BRAND, true);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(context, 14), dp(context, 9), dp(context, 14), dp(context, 9));
        tv.setBackground(round(Color.WHITE, dp(context, 999), Color.rgb(211, 218, 255), context));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.setMargins(0, 0, dp(context, 8), 0);
        tv.setLayoutParams(p);
        tv.setOnClickListener(listener);
        return tv;
    }

    public static HorizontalScrollView chipRow(Context context, List<String> labels, ChipClick click) {
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = horizontal(context);
        for (String label : labels) {
            row.addView(chip(context, label, v -> click.onClick(label)));
        }
        scroll.addView(row);
        return scroll;
    }

    public static Button primaryButton(Context context, String label) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        button.setBackground(gradient(BRAND, PURPLE, dp(context, 15)));
        return button;
    }

    public static Button ghostButton(Context context, String label) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextColor(BRAND);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        button.setBackground(round(Color.WHITE, dp(context, 15), Color.rgb(211, 218, 255), context));
        return button;
    }

    public static EditText input(Context context, String hint) {
        EditText input = new EditText(context);
        input.setHint(hint);
        input.setTextColor(INK);
        input.setHintTextColor(Color.rgb(152, 162, 179));
        input.setTextSize(14);
        input.setSingleLine(true);
        input.setPadding(dp(context, 13), dp(context, 8), dp(context, 13), dp(context, 8));
        input.setBackground(round(Color.WHITE, dp(context, 15), Color.rgb(223, 229, 242), context));
        return input;
    }

    public static LinearLayout statCard(Context context, String title, String value, int color) {
        LinearLayout card = vertical(context);
        card.setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12));
        card.setBackground(round(Color.WHITE, dp(context, 18), SOFT, context));
        card.addView(text(context, title, 12, MUTED, false));
        card.addView(text(context, value == null || value.isEmpty() ? "0" : value, 24, color, true));
        return card;
    }

    public static LinearLayout stateView(Context context, String title, String message, int color, List<String> actions) {
        LinearLayout box = card(context);
        TextView badge = pill(context, title, color, Color.WHITE);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(-2, -2);
        box.addView(badge, badgeParams);
        box.addView(gap(context, 10));
        box.addView(markdown(context, message));
        if (actions != null && !actions.isEmpty()) {
            box.addView(gap(context, 10));
            for (String action : actions) {
                TextView item = text(context, "• " + action, 13, MUTED, false);
                box.addView(item);
            }
        }
        return box;
    }

    public static LinearLayout emptyState(Context context, String route) {
        MobileFeatureCatalog.Scene scene = MobileGuardrails.findScene(route);
        String title = scene == null ? "暂无数据" : scene.emptyTitle;
        String message = scene == null ? "当前页面没有可展示的数据。" : scene.emptyMessage;
        return stateView(context, title, message, BRAND, MobileGuardrails.recoveryActions(route));
    }

    public static LinearLayout errorState(Context context, String scene, String operation, String error) {
        String message = MobileGuardrails.friendlyHttpError(error);
        return stateView(context, "请求失败", operation + "\n\n" + message, RED, MobileGuardrails.recoveryActions(scene));
    }

    public static LinearLayout loadingState(Context context, String message) {
        LinearLayout box = horizontal(context);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(context, 14), dp(context, 14), dp(context, 14), dp(context, 14));
        box.setBackground(round(Color.WHITE, dp(context, 18), SOFT, context));
        ProgressBar bar = new ProgressBar(context);
        box.addView(bar, new LinearLayout.LayoutParams(dp(context, 32), dp(context, 32)));
        TextView tv = text(context, message, 13, MUTED, false);
        tv.setPadding(dp(context, 10), 0, 0, 0);
        box.addView(tv);
        return box;
    }

    public static LinearLayout checklist(Context context, String title, List<String> items) {
        LinearLayout card = card(context);
        card.addView(text(context, title, 16, INK, true));
        card.addView(gap(context, 8));
        if (items == null || items.isEmpty()) {
            card.addView(text(context, "暂无检查项", 13, MUTED, false));
            return card;
        }
        for (int i = 0; i < items.size(); i++) {
            LinearLayout row = horizontal(context);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView index = pill(context, String.valueOf(i + 1), Color.rgb(238, 242, 255), BRAND);
            row.addView(index, new LinearLayout.LayoutParams(dp(context, 30), dp(context, 30)));
            TextView label = text(context, items.get(i), 13, MUTED, false);
            label.setPadding(dp(context, 8), 0, 0, 0);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
            card.addView(row);
            if (i < items.size() - 1) card.addView(gap(context, 6));
        }
        return card;
    }

    public static LinearLayout jsonCard(Context context, String title, Object data) {
        LinearLayout card = card(context);
        card.addView(text(context, title, 16, INK, true));
        card.addView(gap(context, 8));
        card.addView(markdown(context, "```json\n" + (data == null ? "{}" : data.toString()) + "\n```"));
        return card;
    }

    public static LinearLayout themeCard(Context context, MobileModels.ContentTheme theme) {
        LinearLayout card = card(context);
        LinearLayout row = horizontal(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout textBox = vertical(context);
        textBox.addView(text(context, theme.title.isBlank() ? "未命名主题" : theme.title, 17, INK, true));
        textBox.addView(starsStatic(context, theme.rating));
        row.addView(textBox, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(pill(context, theme.statusLine().isBlank() ? "主题" : theme.statusLine(), BRAND, Color.WHITE));
        card.addView(row);
        card.addView(gap(context, 8));
        card.addView(markdown(context, MobileGuardrails.compactOneLine(theme.summary, 180)));
        return card;
    }

    public static LinearLayout draftCard(Context context, MobileModels.CopyDraft draft) {
        LinearLayout card = card(context);
        card.addView(text(context, draft.title.isBlank() ? "未命名文案" : draft.title, 17, INK, true));
        card.addView(starsStatic(context, draft.rating));
        card.addView(gap(context, 8));
        card.addView(markdown(context, MobileGuardrails.compactOneLine(draft.content, 180)));
        card.addView(gap(context, 8));
        card.addView(pill(context, draft.imageLine(), draft.hasImage() ? CYAN : AMBER, Color.WHITE));
        return card;
    }

    public static LinearLayout projectCard(Context context, MobileModels.SchoolProject project) {
        LinearLayout card = card(context);
        card.addView(text(context, project.title(), 17, INK, true));
        String subtitle = project.subtitle();
        if (!subtitle.isBlank()) {
            card.addView(gap(context, 4));
            card.addView(text(context, subtitle, 12, BRAND, true));
        }
        card.addView(gap(context, 8));
        card.addView(markdown(context, MobileGuardrails.compactOneLine(project.requirements, 180)));
        return card;
    }

    public static LinearLayout starsStatic(Context context, int rating) {
        LinearLayout row = horizontal(context);
        for (int i = 1; i <= 5; i++) {
            TextView star = text(context, i <= rating ? "★" : "☆", 18, AMBER, true);
            row.addView(star);
        }
        return row;
    }

    public static LinearLayout starsInteractive(Context context, int rating, RatingClick click) {
        LinearLayout row = horizontal(context);
        for (int i = 1; i <= 5; i++) {
            final int value = i;
            TextView star = text(context, i <= rating ? "★" : "☆", 26, AMBER, true);
            star.setPadding(0, 0, dp(context, 4), 0);
            star.setOnClickListener(v -> click.rate(value));
            row.addView(star);
        }
        return row;
    }

    public static ScrollView scroll(View child) {
        ScrollView scroll = new ScrollView(child.getContext());
        scroll.setFillViewport(false);
        scroll.addView(child);
        return scroll;
    }

    public static Space gap(Context context, int value) {
        Space space = new Space(context);
        space.setLayoutParams(new LinearLayout.LayoutParams(1, dp(context, value)));
        return space;
    }

    public static Space gapW(Context context, int value) {
        Space space = new Space(context);
        space.setLayoutParams(new LinearLayout.LayoutParams(dp(context, value), 1));
        return space;
    }

    public static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    public static GradientDrawable round(int color, int radius, int stroke, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (stroke != 0) drawable.setStroke(dp(context, 1), stroke);
        return drawable;
    }

    public static GradientDrawable gradient(int start, int end, int radius) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        drawable.setCornerRadius(radius);
        return drawable;
    }

    public static void animateIn(View view, int delayMs) {
        view.setAlpha(0f);
        view.setTranslationY(dp(view.getContext(), 14));
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delayMs)
                .setDuration(320)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        ObjectAnimator.ofFloat(view, "scaleX", 0.985f, 1f).setDuration(260).start();
        ObjectAnimator.ofFloat(view, "scaleY", 0.985f, 1f).setDuration(260).start();
    }

    public static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public interface ChipClick {
        void onClick(String label);
    }

    public interface RatingClick {
        void rate(int rating);
    }

    public static LinearLayout endpointList(Context context, MobileFeatureCatalog.Scene scene) {
        LinearLayout card = card(context);
        card.addView(text(context, "接口能力", 16, INK, true));
        card.addView(gap(context, 8));
        for (MobileFeatureCatalog.Endpoint endpoint : scene.endpoints) {
            LinearLayout row = vertical(context);
            row.setPadding(0, dp(context, 6), 0, dp(context, 6));
            LinearLayout top = horizontal(context);
            top.setGravity(Gravity.CENTER_VERTICAL);
            top.addView(pill(context, endpoint.method, endpoint.mutating ? AMBER : BRAND, Color.WHITE));
            TextView name = text(context, endpoint.name, 14, INK, true);
            name.setPadding(dp(context, 8), 0, 0, 0);
            top.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
            if (endpoint.streaming) top.addView(pill(context, "SSE", GREEN, Color.WHITE));
            row.addView(top);
            row.addView(text(context, endpoint.path, 12, MUTED, false));
            row.addView(text(context, endpoint.description, 12, MUTED, false));
            card.addView(row);
        }
        return card;
    }

    public static LinearLayout operationBanner(Context context, MobileModels.OperationState state) {
        int color = state.failed ? RED : state.loading ? AMBER : GREEN;
        String title = state.failed ? "操作失败" : state.loading ? "处理中" : "已完成";
        LinearLayout card = card(context);
        LinearLayout row = horizontal(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(pill(context, title, color, Color.WHITE));
        TextView op = text(context, state.operation, 14, INK, true);
        op.setPadding(dp(context, 10), 0, 0, 0);
        row.addView(op, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(row);
        if (state.message != null && !state.message.isBlank()) {
            card.addView(gap(context, 8));
            card.addView(markdown(context, state.message));
        }
        if (!state.loading) {
            card.addView(gap(context, 6));
            card.addView(text(context, "耗时 " + state.durationMs() + " ms", 12, MUTED, false));
        }
        return card;
    }

    public static LinearLayout permissionHint(Context context, MobileFeatureCatalog.Scene scene, boolean allowed) {
        LinearLayout card = card(context);
        card.addView(text(context, allowed ? "权限正常" : "权限受限", 16, allowed ? GREEN : RED, true));
        card.addView(gap(context, 6));
        if (allowed) {
            card.addView(text(context, "当前账号可以访问「" + scene.title + "」。", 13, MUTED, false));
        } else {
            card.addView(text(context, "当前账号不能访问「" + scene.title + "」，请联系团队管理员调整角色权限。", 13, MUTED, false));
        }
        card.addView(gap(context, 8));
        card.addView(text(context, "所需权限：" + scene.permission, 12, MUTED, false));
        return card;
    }

    public static LinearLayout jsonSummary(Context context, JSONObject json, String... keys) {
        LinearLayout card = card(context);
        for (String key : keys) {
            LinearLayout row = horizontal(context);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(text(context, key, 12, MUTED, true), new LinearLayout.LayoutParams(dp(context, 96), -2));
            row.addView(text(context, json == null ? "" : json.optString(key, ""), 13, INK, false), new LinearLayout.LayoutParams(0, -2, 1));
            card.addView(row);
            card.addView(gap(context, 4));
        }
        return card;
    }

    public static LinearLayout section(Context context, String title, View content) {
        LinearLayout box = vertical(context);
        TextView heading = text(context, title, 18, INK, true);
        heading.setPadding(dp(context, 2), dp(context, 14), dp(context, 2), dp(context, 8));
        box.addView(heading);
        box.addView(content);
        return box;
    }

    public static LinearLayout cardGrid(Context context, List<View> cards) {
        LinearLayout box = vertical(context);
        if (cards == null || cards.isEmpty()) {
            box.addView(stateView(context, "暂无卡片", "当前没有可展示的卡片数据。", BRAND, List.of("刷新数据", "检查后端接口")));
            return box;
        }
        for (int i = 0; i < cards.size(); i++) {
            View card = cards.get(i);
            box.addView(card);
            animateIn(card, i * 30);
        }
        return box;
    }

    public static String summarizeTrace(JSONArray array) {
        if (array == null || array.length() == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null || "skipped".equalsIgnoreCase(item.optString("status"))) continue;
            if (sb.length() > 0) sb.append("  ");
            sb.append("工具: ").append(item.optString("name", "Tool"))
                    .append(" / ").append(item.optString("status", "done"));
        }
        return sb.toString();
    }
}
