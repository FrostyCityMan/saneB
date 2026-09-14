package com.saneb.domain.announcementattachment.qa;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** 실사이트/격리 실행 성공이 아니라 명시 시험의 요청·용량·자원 한도 계약이다. */
class AnnouncementAttachmentBbsFixedCaseQaContractTest {
    final AtomicLong nano=new AtomicLong();
    final AnnouncementAttachmentBbsFixedCaseQaTest.BoundedControl control=new AnnouncementAttachmentBbsFixedCaseQaTest.BoundedControl(
            new AttachmentProviderQaCase.Limits(420,3,100),nano::get);
    @Test void requestAndByteReservationsNeverExceedLimit() {
        for(int i=0;i<3;i++)assertTrue(control.saveRequestReservation());
        assertFalse(control.saveRequestReservation());assertEquals(3,control.requests);
        assertFalse(control.saveByteReservation(0));assertFalse(control.saveByteReservation(-1));
        assertFalse(control.saveByteReservation(Long.MAX_VALUE));assertEquals(0,control.bytes);
        assertTrue(control.saveByteReservation(99));assertFalse(control.saveByteReservation(2));
        assertTrue(control.saveByteReservation(1));assertFalse(control.saveByteReservation(1));assertEquals(100,control.bytes);
    }
    @Test void singleLocalPermitAndIdempotentCloseProtectNextOwner() throws Exception {
        assertNull(control.selectDownloadPermit("invalid"));
        var first=control.selectDownloadPermit("a".repeat(64));assertNotNull(first);
        assertNull(control.selectExtractionPermit());first.close();
        var second=control.selectExtractionPermit();assertNotNull(second);first.close();
        assertTrue(control.inUse.get());assertNull(control.selectExtractionPermit());second.close();assertFalse(control.inUse.get());
    }
    @Test void deadlineStopsNewRequestsBytesAndPermits() {
        nano.set(420_000_000_000L);assertFalse(control.selectExecutionAllowed());
        assertFalse(control.saveRequestReservation());assertFalse(control.saveByteReservation(1));assertNull(control.selectExtractionPermit());
    }
    @Test void interruptionStopsWithoutConsumingBudget() {
        Thread.currentThread().interrupt();
        try {assertFalse(control.selectExecutionAllowed());assertFalse(control.saveRequestReservation());assertFalse(control.saveByteReservation(1));}
        finally {Thread.interrupted();}
        assertEquals(0,control.requests);assertEquals(0,control.bytes);
    }
}
