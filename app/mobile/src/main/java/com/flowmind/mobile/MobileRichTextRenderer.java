package com.flowmind.mobile;

import android.os.Build;
import android.text.Html;
import android.text.Spanned;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MobileRichTextRenderer {
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)]\\(([^)]+)\\)");
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC = Pattern.compile("(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)");
    private static final Pattern STRIKE = Pattern.compile("~~(.+?)~~");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+)`");

    private MobileRichTextRenderer() {
    }

    public static Spanned render(String raw) {
        String html = markdownToHtml(cleanRichText(raw == null ? "" : raw));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        }
        return Html.fromHtml(html);
    }

    public static String jsonToMarkdown(Object value) {
        if (value instanceof JSONObject) {
            return objectToMarkdown((JSONObject) value, 0);
        }
        if (value instanceof JSONArray) {
            return arrayToMarkdown((JSONArray) value, 0);
        }
        return cleanRichText(value == null ? "" : String.valueOf(value));
    }

    public static String compactKnowledge(JSONObject item) {
        if (item == null) return "";
        StringBuilder builder = new StringBuilder();
        appendField(builder, label("summary"), first(item, "summary", "chunkText", "content", "description"));
        appendField(builder, label("source"), first(item, "source", "sourceName", "documentSource", "feishuToken"));
        appendField(builder, label("category"), first(item, "category", "type", "docType"));
        appendField(builder, label("tags"), tagText(item));
        appendField(builder, label("matchedField"), first(item, "matchedField", "field", "hitField"));
        appendField(builder, label("score"), first(item, "score", "distance", "similarity"));
        return builder.toString().trim();
    }

    public static String compactSchool(JSONObject item) {
        if (item == null) return "";
        StringBuilder builder = new StringBuilder();
        appendField(builder, label("schoolName"), first(item, "schoolName", "name"));
        appendField(builder, label("level"), first(item, "level", "schoolLevel"));
        appendField(builder, label("region"), first(item, "region", "location", "city"));
        appendField(builder, label("projectName"), first(item, "projectName", "programName"));
        appendField(builder, label("projectType"), first(item, "projectType", "type"));
        appendField(builder, label("deadline"), first(item, "deadline", "applyDeadline"));
        appendField(builder, label("requirements"), first(item, "requirements", "condition", "applyCondition"));
        appendField(builder, label("materials"), first(item, "materials", "materialRequirements"));
        appendField(builder, label("matchScore"), first(item, "matchScore", "score"));
        appendField(builder, label("tags"), first(item, "disciplineTags", "tags"));
        return builder.toString().trim();
    }

    public static String tagText(JSONObject item) {
        if (item == null) return "";
        Object tags = firstObject(item, "tags", "tagList", "knowledgeTags");
        if (tags instanceof JSONArray) {
            return tagArrayToText((JSONArray) tags);
        }
        if (tags instanceof JSONObject) {
            return objectTagsToText((JSONObject) tags);
        }
        return cleanTagText(first(item, "tags", "tagNames", "tagName", "category", "disciplineTags"));
    }

    public static String cleanRichText(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) return "";
        text = decodeEntities(text);
        text = text.replaceAll("(?is)<title[^>]*>(.*?)</title>", "# $1\n\n");
        text = text.replaceAll("(?is)<h1[^>]*>(.*?)</h1>", "# $1\n\n");
        text = text.replaceAll("(?is)<h2[^>]*>(.*?)</h2>", "## $1\n\n");
        text = text.replaceAll("(?is)<h3[^>]*>(.*?)</h3>", "### $1\n\n");
        text = text.replaceAll("(?is)<callout[^>]*>", "\n> ");
        text = text.replaceAll("(?is)</callout>", "\n\n");
        text = text.replaceAll("(?is)<br\\s*/?>", "\n");
        text = text.replaceAll("(?is)<hr\\s*/?>", "\n\n---\n\n");
        text = text.replaceAll("(?is)</p>", "\n\n");
        text = text.replaceAll("(?is)<p[^>]*>", "");
        text = text.replaceAll("(?is)<li[^>]*>", "- ");
        text = text.replaceAll("(?is)</li>", "\n");
        text = text.replaceAll("(?is)<tr[^>]*>", "\n");
        text = text.replaceAll("(?is)</tr>", "\n");
        text = text.replaceAll("(?is)<t[hd][^>]*>", "- ");
        text = text.replaceAll("(?is)</t[hd]>", "\n");
        text = text.replaceAll("(?is)<b[^>]*>(.*?)</b>", "**$1**");
        text = text.replaceAll("(?is)<strong[^>]*>(.*?)</strong>", "**$1**");
        text = text.replaceAll("(?is)<i[^>]*>(.*?)</i>", "*$1*");
        text = text.replaceAll("(?is)<em[^>]*>(.*?)</em>", "*$1*");
        text = text.replaceAll("(?is)<a[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", "[$2]($1)");
        text = text.replaceAll("(?is)<colgroup[^>]*>.*?</colgroup>", "");
        text = text.replaceAll("(?is)<[^>]+>", "");
        text = decodeEntities(text);
        text = text.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
        text = text.replaceAll(" *\\n *", "\n");
        text = text.replaceAll("\\n{3,}", "\n\n");
        return text.trim();
    }

    public static String first(JSONObject item, String... keys) {
        if (item == null || keys == null) return "";
        for (String key : keys) {
            Object value = item.opt(key);
            if (value == null || JSONObject.NULL.equals(value)) continue;
            String text = String.valueOf(value).trim();
            if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) return cleanRichText(text);
        }
        return "";
    }

    private static Object firstObject(JSONObject item, String... keys) {
        if (item == null || keys == null) return null;
        for (String key : keys) {
            Object value = item.opt(key);
            if (value != null && !JSONObject.NULL.equals(value)) return value;
        }
        return null;
    }

    private static String objectToMarkdown(JSONObject object, int depth) {
        StringBuilder builder = new StringBuilder();
        JSONArray names = object.names();
        if (names == null) return "";
        for (int i = 0; i < names.length(); i++) {
            String key = names.optString(i);
            Object value = object.opt(key);
            if (isHiddenKey(key)) continue;
            if (value instanceof JSONObject) {
                builder.append("- **").append(label(key)).append("**:\n")
                        .append(indent(objectToMarkdown((JSONObject) value, depth + 1), depth + 1));
            } else if (value instanceof JSONArray) {
                if (isTagKey(key)) {
                    appendField(builder, label(key), tagArrayToText((JSONArray) value));
                } else {
                    builder.append("- **").append(label(key)).append("**:\n")
                            .append(indent(arrayToMarkdown((JSONArray) value, depth + 1), depth + 1));
                }
            } else {
                String text = value == null ? "" : String.valueOf(value);
                if (isContentKey(key)) text = cleanRichText(text);
                if (isTagKey(key)) text = cleanTagText(text);
                appendField(builder, label(key), text);
            }
        }
        return builder.toString().trim();
    }

    private static String arrayToMarkdown(JSONArray array, int depth) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.opt(i);
            if (value instanceof JSONObject) {
                builder.append(i + 1).append(".\n")
                        .append(indent(objectToMarkdown((JSONObject) value, depth + 1), depth + 1)).append('\n');
            } else if (value instanceof JSONArray) {
                builder.append(i + 1).append(".\n")
                        .append(indent(arrayToMarkdown((JSONArray) value, depth + 1), depth + 1)).append('\n');
            } else {
                builder.append(i + 1).append(". ").append(cleanRichText(value == null ? "" : String.valueOf(value))).append('\n');
            }
        }
        return builder.toString().trim();
    }

    private static void appendField(StringBuilder builder, String label, String value) {
        String cleaned = cleanRichText(value);
        if (cleaned.isEmpty()) return;
        builder.append("- **").append(label).append("**: ").append(cleaned).append('\n');
    }

    private static String markdownToHtml(String raw) {
        List<String> lines = split(raw);
        StringBuilder html = new StringBuilder();
        StringBuilder paragraph = new StringBuilder();
        boolean inCode = false;
        boolean inUl = false;
        boolean inOl = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                flushParagraph(html, paragraph);
                if (inUl) { html.append("</ul>"); inUl = false; }
                if (inOl) { html.append("</ol>"); inOl = false; }
                html.append(inCode ? "</blockquote>" : "<blockquote>");
                inCode = !inCode;
                continue;
            }
            if (inCode) {
                html.append("<font color='#344054'><tt>").append(escape(line).replace(" ", "&nbsp;")).append("</tt></font><br/>");
                continue;
            }
            if (trimmed.isEmpty()) {
                flushParagraph(html, paragraph);
                if (inUl) { html.append("</ul>"); inUl = false; }
                if (inOl) { html.append("</ol>"); inOl = false; }
                html.append("<br/>");
                continue;
            }
            if (trimmed.matches("^[-*_]{3,}$")) {
                flushParagraph(html, paragraph);
                html.append("<br/><font color='#D0D5DD'>------------</font><br/>");
                continue;
            }
            if (trimmed.startsWith("#")) {
                flushParagraph(html, paragraph);
                html.append(heading(trimmed));
                continue;
            }
            if (trimmed.startsWith(">")) {
                flushParagraph(html, paragraph);
                html.append("<blockquote><font color='#667085'>").append(inline(trimmed.replaceFirst("^>\\s?", ""))).append("</font></blockquote>");
                continue;
            }
            if (trimmed.matches("^[-*+]\\s+.+$") || trimmed.matches("^[-*+]\\s+\\[[ xX]]\\s+.+$")) {
                flushParagraph(html, paragraph);
                if (inOl) { html.append("</ol>"); inOl = false; }
                if (!inUl) { html.append("<ul>"); inUl = true; }
                html.append("<li>").append(inline(cleanBullet(trimmed))).append("</li>");
                continue;
            }
            if (trimmed.matches("^\\d+[.)]\\s+.+$")) {
                flushParagraph(html, paragraph);
                if (inUl) { html.append("</ul>"); inUl = false; }
                if (!inOl) { html.append("<ol>"); inOl = true; }
                html.append("<li>").append(inline(trimmed.replaceFirst("^\\d+[.)]\\s+", ""))).append("</li>");
                continue;
            }
            if (paragraph.length() > 0) paragraph.append(' ');
            paragraph.append(trimmed);
        }
        flushParagraph(html, paragraph);
        if (inUl) html.append("</ul>");
        if (inOl) html.append("</ol>");
        if (inCode) html.append("</blockquote>");
        return html.toString();
    }

    private static String heading(String trimmed) {
        int level = 0;
        while (level < trimmed.length() && trimmed.charAt(level) == '#') level++;
        String content = trimmed.substring(level).trim();
        if (level <= 1) return "<h2>" + inline(content) + "</h2>";
        if (level == 2) return "<h3>" + inline(content) + "</h3>";
        return "<b>" + inline(content) + "</b><br/>";
    }

    private static String inline(String raw) {
        String value = escape(raw);
        value = replace(value, LINK, "<a href='$2'>$1</a>");
        value = replace(value, BOLD, "<b>$1</b>");
        value = replace(value, STRIKE, "<s>$1</s>");
        value = replace(value, INLINE_CODE, "<font color='#4F5CFF'><tt>$1</tt></font>");
        value = replace(value, ITALIC, "<i>$1</i>");
        return value;
    }

    private static String replace(String value, Pattern pattern, String replacement) {
        Matcher matcher = pattern.matcher(value);
        return matcher.replaceAll(replacement);
    }

    private static void flushParagraph(StringBuilder html, StringBuilder paragraph) {
        if (paragraph.length() == 0) return;
        html.append("<p>").append(inline(paragraph.toString())).append("</p>");
        paragraph.setLength(0);
    }

    private static String cleanBullet(String trimmed) {
        String value = trimmed.replaceFirst("^[-*+]\\s+", "");
        value = value.replaceFirst("^\\[ ]\\s+", "\u2610 ");
        value = value.replaceFirst("^\\[[xX]]\\s+", "\u2611 ");
        return value;
    }

    private static String tagArrayToText(JSONArray array) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object entry = array.opt(i);
            if (entry instanceof JSONObject) {
                values.add(first((JSONObject) entry, "name", "tagName", "title", "label"));
            } else if (entry != null && !JSONObject.NULL.equals(entry)) {
                values.add(cleanTagText(String.valueOf(entry)));
            }
        }
        return join(values, " / ");
    }

    private static String objectTagsToText(JSONObject object) {
        List<String> values = new ArrayList<>();
        JSONArray names = object.names();
        if (names == null) return "";
        for (int i = 0; i < names.length(); i++) {
            String key = names.optString(i);
            Object value = object.opt(key);
            values.add(cleanTagText(value == null ? key : String.valueOf(value)));
        }
        return join(values, " / ");
    }

    private static String cleanTagText(String raw) {
        String text = cleanRichText(raw);
        text = text.replace("[", "").replace("]", "").replace("\"", "");
        text = text.replaceAll("\\s*,\\s*", " / ");
        text = text.replaceAll("\\s*，\\s*", " / ");
        text = text.replaceAll("\\s*、\\s*", " / ");
        return text.trim();
    }

    private static boolean isContentKey(String key) {
        String k = key == null ? "" : key.toLowerCase();
        return k.contains("content") || k.contains("html") || k.contains("body") || k.contains("summary") || k.contains("chunktext");
    }

    private static boolean isTagKey(String key) {
        String k = key == null ? "" : key.toLowerCase();
        return k.contains("tag") || k.contains("category") || k.contains("discipline");
    }

    private static boolean isHiddenKey(String key) {
        String k = key == null ? "" : key.toLowerCase();
        return k.equals("deleted") || k.equals("deletedat");
    }

    private static String label(String key) {
        String k = key == null ? "" : key;
        switch (k) {
            case "id": return "ID";
            case "title": return "\u6807\u9898";
            case "content": return "\u6b63\u6587";
            case "summary": return "\u6458\u8981";
            case "source": return "\u6765\u6e90";
            case "category": return "\u5206\u7c7b";
            case "tags":
            case "tagNames":
            case "tagList": return "\u6807\u7b7e";
            case "feishuToken": return "\u98de\u4e66 Token";
            case "schoolName":
            case "name": return "\u5b66\u6821";
            case "projectName": return "\u9879\u76ee";
            case "level": return "\u5c42\u6b21";
            case "region":
            case "location": return "\u5730\u533a";
            case "projectType": return "\u7c7b\u578b";
            case "deadline": return "\u622a\u6b62";
            case "requirements": return "\u6761\u4ef6";
            case "materials": return "\u6750\u6599";
            case "matchScore": return "\u5339\u914d";
            case "matchedField": return "\u5339\u914d\u5b57\u6bb5";
            case "score":
            case "distance":
            case "similarity": return "\u76f8\u4f3c\u5ea6";
            default: return k;
        }
    }

    private static String decodeEntities(String text) {
        return text
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    private static String escape(String raw) {
        return raw == null ? "" : raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static List<String> split(String raw) {
        List<String> lines = new ArrayList<>();
        String[] parts = raw.replace("\r\n", "\n").replace('\r', '\n').trim().split("\n", -1);
        for (String part : parts) lines.add(part);
        return lines;
    }

    private static String indent(String text, int depth) {
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < depth; i++) prefix.append("  ");
        return prefix + text.replace("\n", "\n" + prefix);
    }

    private static String join(List<String> values, String separator) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            if (builder.length() > 0) builder.append(separator);
            builder.append(value.trim());
        }
        return builder.toString();
    }
}
