package com.flowmind.knowledge.vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits long document content into chunks suitable for embedding.
 *
 * Strategy (Chinese-friendly):
 * 1. Clean HTML tags → plain text
 * 2. Split by double-newline (paragraph boundaries)
 * 3. If a paragraph is too long (> maxChars), further split by sentence-ending punctuation
 * 4. Overlap between chunks for continuity
 */
public class TextChunker {
    private static final Logger log = LoggerFactory.getLogger(TextChunker.class);

    // Chinese + Western sentence-ending markers
    private static final Pattern SENTENCE_END = Pattern.compile(
            "(?<=[。！？；\\n])(?=[^。！？；\\n])");
    private static final Pattern PARAGRAPH_SPLIT = Pattern.compile("\\n{2,}|\\r\\n{2,}");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final int maxChars;
    private final int overlapChars;
    private final int minChars;

    public TextChunker(int maxChars, int overlapChars, int minChars) {
        this.maxChars = maxChars;
        this.overlapChars = overlapChars;
        this.minChars = minChars;
    }

    public TextChunker() {
        this(800, 100, 50);
    }

    /**
     * Chunk the document content into a list of text chunks.
     */
    public List<String> chunk(String title, String content) {
        List<String> chunks = new ArrayList<>();

        // Always include title as first chunk (high signal)
        if (title != null && !title.isBlank()) {
            chunks.add(title.trim());
        }

        if (content == null || content.isBlank()) return chunks;

        // Clean HTML
        String plain = HTML_TAG.matcher(content).replaceAll(" ");
        plain = plain.replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&nbsp;", " ");
        plain = WHITESPACE.matcher(plain).replaceAll(" ").trim();

        if (plain.isBlank()) return chunks;

        // Split by paragraphs
        String[] paragraphs = PARAGRAPH_SPLIT.split(plain);

        for (String para : paragraphs) {
            String p = para.trim();
            if (p.length() < minChars) continue;

            if (p.length() <= maxChars) {
                chunks.add(p);
            } else {
                // Further split long paragraphs by sentences
                String[] sentences = SENTENCE_END.split(p);
                StringBuilder buf = new StringBuilder();

                for (String sent : sentences) {
                    String s = sent.trim();
                    if (s.isEmpty()) continue;

                    if (buf.length() + s.length() > maxChars) {
                        if (buf.length() >= minChars) {
                            chunks.add(buf.toString().trim());
                            // Keep overlap: last overlapChars chars
                            if (overlapChars > 0 && buf.length() > overlapChars) {
                                String tail = buf.substring(buf.length() - overlapChars);
                                buf = new StringBuilder(tail);
                            } else {
                                buf = new StringBuilder();
                            }
                        }
                    }

                    if (buf.length() > 0) buf.append(" ");
                    buf.append(s);
                }

                if (buf.length() >= minChars) {
                    chunks.add(buf.toString().trim());
                }
            }
        }

        log.debug("Chunked doc into {} chunks (title + content)", chunks.size());
        return chunks;
    }
}
