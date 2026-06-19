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

/**
 * Small Markdown-to-HTML renderer for the native Android demo.
 *
 * Android TextView cannot render full Markdown by itself. This renderer handles
 * the formats produced by common LLM replies and by FlowMind knowledge records:
 * headings, bold, italic, inline code, fenced code, quotes, ordered lists,
 * unordered lists, task lists, tables, links, horizontal rules and JSON blocks.
 * The output is intentionally conservative HTML supported by Html.fromHtml.
 */
public final class MobileMarkdownRenderer {
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)]\\(([^)]+)\\)");
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC = Pattern.compile("(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)");
    private static final Pattern STRIKE = Pattern.compile("~~(.+?)~~");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+)`");

    private MobileMarkdownRenderer() {
    }

    public static Spanned render(String raw) {
        String html = toHtml(raw == null ? "" : raw);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        }
        return Html.fromHtml(html);
    }

    public static String toHtml(String raw) {
        String normalized = normalize(raw);
        List<String> lines = splitLines(normalized);
        StringBuilder html = new StringBuilder();
        boolean inCode = false;
        boolean inUl = false;
        boolean inOl = false;
        StringBuilder paragraph = new StringBuilder();
        String codeLanguage = "";

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();

            if (trimmed.startsWith("```")) {
                flushParagraph(html, paragraph);
                if (inUl) {
                    html.append("</ul>");
                    inUl = false;
                }
                if (inOl) {
                    html.append("</ol>");
                    inOl = false;
                }
                if (!inCode) {
                    inCode = true;
                    codeLanguage = trimmed.length() > 3 ? trimmed.substring(3).trim() : "";
                    html.append("<blockquote>");
                    if (!codeLanguage.isEmpty()) {
                        html.append("<b>").append(escape(codeLanguage)).append("</b><br/>");
                    }
                } else {
                    inCode = false;
                    html.append("</blockquote>");
                    codeLanguage = "";
                }
                continue;
            }

            if (inCode) {
                html.append("<font color='#344054'><tt>")
                        .append(escape(line).replace(" ", "&nbsp;"))
                        .append("</tt></font><br/>");
                continue;
            }

            if (trimmed.isEmpty()) {
                flushParagraph(html, paragraph);
                if (inUl) {
                    html.append("</ul>");
                    inUl = false;
                }
                if (inOl) {
                    html.append("</ol>");
                    inOl = false;
                }
                html.append("<br/>");
                continue;
            }

            if (trimmed.matches("^[-*_]{3,}$")) {
                flushParagraph(html, paragraph);
                closeLists(html, inUl, inOl);
                inUl = false;
                inOl = false;
                html.append("<br/><font color='#D0D5DD'>────────────</font><br/>");
                continue;
            }

            if (isTableDivider(trimmed)) {
                flushParagraph(html, paragraph);
                continue;
            }

            if (looksLikeTableRow(trimmed)) {
                flushParagraph(html, paragraph);
                closeLists(html, inUl, inOl);
                inUl = false;
                inOl = false;
                html.append(renderTableRow(trimmed));
                continue;
            }

            if (trimmed.startsWith("#")) {
                flushParagraph(html, paragraph);
                closeLists(html, inUl, inOl);
                inUl = false;
                inOl = false;
                html.append(renderHeading(trimmed));
                continue;
            }

            if (trimmed.startsWith(">")) {
                flushParagraph(html, paragraph);
                closeLists(html, inUl, inOl);
                inUl = false;
                inOl = false;
                String quote = trimmed.replaceFirst("^>\\s?", "");
                html.append("<blockquote><font color='#667085'>")
                        .append(inline(quote))
                        .append("</font></blockquote>");
                continue;
            }

            if (trimmed.matches("^[-*+]\\s+.+$") || trimmed.matches("^[-*+]\\s+\\[[ xX]]\\s+.+$")) {
                flushParagraph(html, paragraph);
                if (inOl) {
                    html.append("</ol>");
                    inOl = false;
                }
                if (!inUl) {
                    html.append("<ul>");
                    inUl = true;
                }
                html.append("<li>").append(inline(cleanUnordered(trimmed))).append("</li>");
                continue;
            }

            if (trimmed.matches("^\\d+[.)]\\s+.+$")) {
                flushParagraph(html, paragraph);
                if (inUl) {
                    html.append("</ul>");
                    inUl = false;
                }
                if (!inOl) {
                    html.append("<ol>");
                    inOl = true;
                }
                html.append("<li>").append(inline(trimmed.replaceFirst("^\\d+[.)]\\s+", ""))).append("</li>");
                continue;
            }

            if (paragraph.length() > 0) {
                paragraph.append(' ');
            }
            paragraph.append(trimmed);
        }

        flushParagraph(html, paragraph);
        closeLists(html, inUl, inOl);
        if (inCode) {
            html.append("</blockquote>");
        }
        return html.toString();
    }

    public static String jsonToMarkdown(Object value) {
        if (value instanceof JSONObject) {
            return objectToMarkdown((JSONObject) value, 0);
        }
        if (value instanceof JSONArray) {
            return arrayToMarkdown((JSONArray) value, 0);
        }
        return value == null ? "" : String.valueOf(value);
    }

    public static String compactKnowledge(JSONObject item) {
        if (item == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        appendField(builder, "摘要", first(item, "summary", "chunkText", "content", "description"));
        appendField(builder, "来源", first(item, "source", "sourceName", "documentSource"));
        appendField(builder, "分类", first(item, "category", "type", "docType"));
        appendField(builder, "标签", tagText(item));
        appendField(builder, "匹配字段", first(item, "matchedField", "field", "hitField"));
        appendField(builder, "相似度", first(item, "score", "distance", "similarity"));
        return builder.toString().trim();
    }

    public static String compactSchool(JSONObject item) {
        if (item == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        appendField(builder, "学校", first(item, "schoolName", "name"));
        appendField(builder, "层次", first(item, "level", "schoolLevel"));
        appendField(builder, "地区", first(item, "region", "location", "city"));
        appendField(builder, "项目", first(item, "projectName", "programName"));
        appendField(builder, "类型", first(item, "projectType", "type"));
        appendField(builder, "截止", first(item, "deadline", "applyDeadline"));
        appendField(builder, "条件", first(item, "requirements", "condition", "applyCondition"));
        appendField(builder, "材料", first(item, "materials", "materialRequirements"));
        appendField(builder, "匹配", first(item, "matchScore", "score"));
        appendField(builder, "标签", first(item, "disciplineTags", "tags"));
        return builder.toString().trim();
    }

    public static String tagText(JSONObject item) {
        if (item == null) {
            return "";
        }
        Object tags = item.opt("tags");
        if (tags instanceof JSONArray) {
            JSONArray array = (JSONArray) tags;
            List<String> values = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                Object entry = array.opt(i);
                if (entry instanceof JSONObject) {
                    values.add(first((JSONObject) entry, "name", "tagName", "title"));
                } else if (entry != null) {
                    values.add(String.valueOf(entry));
                }
            }
            return joinNonEmpty(values, " / ");
        }
        String text = first(item, "tags", "tagNames", "category", "disciplineTags");
        return text == null ? "" : text;
    }

    public static String first(JSONObject item, String... keys) {
        if (item == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            Object value = item.opt(key);
            if (value == null || JSONObject.NULL.equals(value)) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) {
                return text;
            }
        }
        return "";
    }

    private static void appendField(StringBuilder builder, String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        builder.append("- **").append(label).append("**：").append(value.trim()).append('\n');
    }

    private static String objectToMarkdown(JSONObject object, int depth) {
        StringBuilder builder = new StringBuilder();
        JSONArray names = object.names();
        if (names == null) {
            return "";
        }
        for (int i = 0; i < names.length(); i++) {
            String key = names.optString(i);
            Object value = object.opt(key);
            if (value instanceof JSONObject) {
                builder.append("- **").append(key).append("**：\n")
                        .append(indent(objectToMarkdown((JSONObject) value, depth + 1), depth + 1));
            } else if (value instanceof JSONArray) {
                builder.append("- **").append(key).append("**：\n")
                        .append(indent(arrayToMarkdown((JSONArray) value, depth + 1), depth + 1));
            } else {
                builder.append("- **").append(key).append("**：").append(value == null ? "" : value).append('\n');
            }
        }
        return builder.toString();
    }

    private static String arrayToMarkdown(JSONArray array, int depth) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.opt(i);
            if (value instanceof JSONObject) {
                builder.append(i + 1).append(". ").append('\n')
                        .append(indent(objectToMarkdown((JSONObject) value, depth + 1), depth + 1));
            } else if (value instanceof JSONArray) {
                builder.append(i + 1).append(". ").append('\n')
                        .append(indent(arrayToMarkdown((JSONArray) value, depth + 1), depth + 1));
            } else {
                builder.append(i + 1).append(". ").append(value == null ? "" : value).append('\n');
            }
        }
        return builder.toString();
    }

    private static String indent(String text, int depth) {
        String prefix = "";
        for (int i = 0; i < depth; i++) {
            prefix += "  ";
        }
        return prefix + text.replace("\n", "\n" + prefix);
    }

    private static String normalize(String raw) {
        return raw.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private static List<String> splitLines(String raw) {
        List<String> lines = new ArrayList<>();
        String[] parts = raw.split("\n", -1);
        for (String part : parts) {
            lines.add(part);
        }
        return lines;
    }

    private static void flushParagraph(StringBuilder html, StringBuilder paragraph) {
        if (paragraph.length() == 0) {
            return;
        }
        html.append("<p>").append(inline(paragraph.toString())).append("</p>");
        paragraph.setLength(0);
    }

    private static void closeLists(StringBuilder html, boolean inUl, boolean inOl) {
        if (inUl) {
            html.append("</ul>");
        }
        if (inOl) {
            html.append("</ol>");
        }
    }

    private static String renderHeading(String trimmed) {
        int level = 0;
        while (level < trimmed.length() && trimmed.charAt(level) == '#') {
            level++;
        }
        String content = trimmed.substring(level).trim();
        if (level <= 1) {
            return "<h2>" + inline(content) + "</h2>";
        }
        if (level == 2) {
            return "<h3>" + inline(content) + "</h3>";
        }
        return "<b>" + inline(content) + "</b><br/>";
    }

    private static boolean isTableDivider(String trimmed) {
        return trimmed.matches("^\\|?\\s*:?-{3,}:?\\s*(\\|\\s*:?-{3,}:?\\s*)+\\|?$");
    }

    private static boolean looksLikeTableRow(String trimmed) {
        return trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.indexOf('|', 1) > 0;
    }

    private static String renderTableRow(String trimmed) {
        String[] cells = trimmed.substring(1, trimmed.length() - 1).split("\\|");
        StringBuilder builder = new StringBuilder();
        builder.append("<blockquote>");
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                builder.append("<br/>");
            }
            builder.append("<b>").append(i + 1).append(".</b> ").append(inline(cells[i].trim()));
        }
        builder.append("</blockquote>");
        return builder.toString();
    }

    private static String cleanUnordered(String trimmed) {
        String value = trimmed.replaceFirst("^[-*+]\\s+", "");
        value = value.replaceFirst("^\\[ ]\\s+", "☐ ");
        value = value.replaceFirst("^\\[[xX]]\\s+", "☑ ");
        return value;
    }

    private static String inline(String raw) {
        String value = escape(raw);
        value = replaceAll(value, LINK, "<a href='$2'>$1</a>");
        value = replaceAll(value, BOLD, "<b>$1</b>");
        value = replaceAll(value, STRIKE, "<s>$1</s>");
        value = replaceAll(value, INLINE_CODE, "<font color='#4F5CFF'><tt>$1</tt></font>");
        value = replaceAll(value, ITALIC, "<i>$1</i>");
        return value;
    }

    private static String replaceAll(String value, Pattern pattern, String replacement) {
        Matcher matcher = pattern.matcher(value);
        return matcher.replaceAll(replacement);
    }

    private static String escape(String raw) {
        return raw == null ? "" : raw
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String joinNonEmpty(List<String> values, String separator) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(value.trim());
        }
        return builder.toString();
    }
}
