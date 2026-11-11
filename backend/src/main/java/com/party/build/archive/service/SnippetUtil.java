package com.party.build.archive.service;

public final class SnippetUtil {
    private SnippetUtil() {
    }

    public static String snippet(String text, String keyword) {
        if (text == null || keyword == null || keyword.isBlank()) {
            return "";
        }
        String plain = text.replaceAll("\\s+", " ");
        int idx = plain.toLowerCase().indexOf(keyword.toLowerCase());
        if (idx < 0) {
            return plain.length() <= 120 ? plain : plain.substring(0, 120) + "...";
        }
        int start = Math.max(0, idx - 50);
        int end = Math.min(plain.length(), idx + keyword.length() + 70);
        return (start > 0 ? "..." : "") + plain.substring(start, end) + (end < plain.length() ? "..." : "");
    }
}