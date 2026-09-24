package com.saneb.extractor;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class HwpControlHeaderSummaryTest {
    private byte[] header(int size) {var data=new byte[size];ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).putInt(0x74626c20);return data;}
    @Test void lengthsAndFixedShapesNeverEmitPayload() throws Exception {
        var summary=new HwpControlHeaderSummary();
        for(int size:new int[]{39,40,44,46,50})summary.insert(header(size));
        var extra=header(52);extra[50]=17;summary.insert(extra);summary.insert(extra);
        var declared=header(48);declared[44]=4;summary.insert(declared);
        var exact=header(50);exact[44]=2;summary.insert(exact);
        var values=summary.select();assertEquals(8,values.size());
        assertTrue(values.contains(new ExtractionResult.ControlHeader("TABLE",40,"COMMON_ONLY",0,1)));
        assertTrue(values.contains(new ExtractionResult.ControlHeader("TABLE",50,"EXTRA_ZERO",4,1)));
        assertTrue(values.contains(new ExtractionResult.ControlHeader("TABLE",52,"EXTRA_NONZERO",6,2)));
        assertTrue(values.contains(new ExtractionResult.ControlHeader("TABLE",48,"DECLARED_TOO_LONG",0,1)));
        assertTrue(values.contains(new ExtractionResult.ControlHeader("TABLE",50,"EXTENDED_EXACT",0,1)));
        assertThrows(UnsupportedOperationException.class,()->values.clear());
    }
    @Test void unknownAndTruncatedIdsAreNotExportedAndDistinctShapesAreBounded() throws Exception {
        var summary=new HwpControlHeaderSummary();summary.insert(new byte[2]);
        var unknown="PRIVATE_CANARY_PAYLOAD".getBytes(java.nio.charset.StandardCharsets.UTF_8);summary.insert(unknown);
        assertTrue(summary.select().stream().allMatch(v->v.kind().equals("OTHER")&&v.shape().equals("NOT_TABLE")));
        assertFalse(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(summary.select()).contains("PRIVATE"));
        var limited=new HwpControlHeaderSummary();for(int i=0;i<128;i++)limited.insert(new byte[i]);
        assertEquals("LIMIT_EXCEEDED",assertThrows(java.io.IOException.class,()->limited.insert(new byte[128])).getMessage());
        limited.insert(new byte[127]);assertEquals(128,limited.select().size());
    }
}
