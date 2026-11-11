package com.party.build.archive.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SnippetUtilTest {
    @Test
    void shouldReturnSnippetAroundKeyword() {
        String text = "前面一些无关内容。".repeat(20) + "入党动机应当结合个人成长经历。" + "后面一些内容。".repeat(20);
        String snippet = SnippetUtil.snippet(text, "入党动机");
        assertThat(snippet).contains("入党动机");
        assertThat(snippet.length()).isLessThan(200);
    }

    @Test
    void shouldReturnEmptyForBlankKeyword() {
        assertThat(SnippetUtil.snippet("材料文本", "")).isEmpty();
        assertThat(SnippetUtil.snippet(null, "关键词")).isEmpty();
    }
}