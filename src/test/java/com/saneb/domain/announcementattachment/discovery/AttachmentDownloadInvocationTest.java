package com.saneb.domain.announcementattachment.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;

class AttachmentDownloadInvocationTest {
    @Test void parsesOnlyThreeLiteralStringsAndSupportedEscapesWithoutExecutingCode() {
        assertThat(AttachmentDownloadInvocation.selectArguments("javascript:goDownLoad('한글 공고.pdf','a\\'b','c\\\\d');", "goDownLoad", false, false))
                .containsExactly("한글 공고.pdf", "a'b", "c\\d");
        assertThat(AttachmentDownloadInvocation.selectArguments(" fn_egov_downFile ( 'a', 'b', 'c' ); return\tfalse; ", "fn_egov_downFile", true, true))
                .containsExactly("a", "b", "c");
    }
    @Test void maximumLengthStringsAreIterativeAndInvalidLargeInputsAreRejected() {
        String valid = "javascript:goDownLoad('" + "가".repeat(2048) + "','" + "B".repeat(2048) + "','" + "C".repeat(2048) + "')";
        assertThat(AttachmentDownloadInvocation.selectArguments(valid, "goDownLoad", false, false)).hasSize(3);
        for (String invalid : List.of("javascript:goDownLoad('" + "가".repeat(2049) + "','b','c')", valid + " ".repeat(8192),
                "javascript:goDownLoad('" + "A".repeat(7500), "javascript:goDownLoad('" + "\\'".repeat(3000) + "','b','c')"))
            assertThat(AttachmentDownloadInvocation.selectArguments(invalid, "goDownLoad", false, false)).isEmpty();
    }
    @Test void refusesExpressionsExtraCallsExtraArgumentsControlsAndUnknownEscapes() {
        for (String invalid : List.of("javascript:goDownLoad('a','b','c');arbitrary()", "javascript:goDownLoad('a','b','c','d')",
                "javascript:goDownLoad('a'+variable,'b','c')", "javascript:goDownLoad(variable,'b','c')", "javascript:goDownLoad('a','b')",
                "javascript:goDownLoad('a\\x27','b','c')", "javascript:goDownLoad('a\nb','b','c')", "javascript:goDownLoad('a','b','c'); return false;",
                "javascript:goDownLoad('a','b','c');;", "javascript:goDownLoad('a','b','c') /* comment */", "javascript:other('a','b','c')"))
            assertThat(AttachmentDownloadInvocation.selectArguments(invalid, "goDownLoad", false, false)).isEmpty();
    }
    @Test void bareCallAndReturnFalseAreAllowedOnlyForTheSpecificSeoguContract() {
        assertThat(AttachmentDownloadInvocation.selectArguments("goDownLoad('a','b','c')", "goDownLoad", false, false)).isEmpty();
        for (String tail : List.of("return true;", "returnfalse;", "return false;arbitrary();", "return falsex;"))
            assertThat(AttachmentDownloadInvocation.selectArguments("fn_egov_downFile('a','b','c');" + tail, "fn_egov_downFile", true, true)).isEmpty();
        assertThat(AttachmentDownloadInvocation.selectArguments(null, "goDownLoad", false, false)).isEmpty();
        assertThat(AttachmentDownloadInvocation.selectArguments("unknown('a','b','c')", "unknown", true, true)).isEmpty();
    }
}
