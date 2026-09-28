package com.saneb.domain.announcementattachment.qa;

import com.saneb.domain.announcementattachment.discovery.AttachmentDiscoveryProfile;
import com.saneb.domain.announcementattachment.discovery.AttachmentDownloadFlowProfile;
import com.saneb.domain.announcementsource.provider.content.AttachmentPinnedDownloadClient;

/** 관측 전용 고정 상태. URL·form·응답·파일명은 저장하지 않고 HTTP 예약 원장도 대신하지 않는다. */
final class ObservationDownloadTrace {
    enum Step { NOT_STARTED, DIRECT, FLOW_STEP, BRIDGE_GET, PERIOD_POST, FINAL_POST }
    enum Phase { BEFORE_TRANSPORT, TRANSPORT, PROFILE_PROCESSING, COMPLETE }
    record Snapshot(int schemaVersion, Step step, Phase phase, int transportInvocations, int completedTransports) { }
    private Step step=Step.NOT_STARTED;
    private Phase phase=Phase.BEFORE_TRANSPORT;
    private int invocations, completed;

    void saveTransportStarted(AttachmentDiscoveryProfile profile, AttachmentPinnedDownloadClient.Request request) {
        if(phase==Phase.COMPLETE || phase==Phase.TRANSPORT || invocations>=4)
            throw new IllegalStateException("DOWNLOAD_TRACE_SEQUENCE_INVALID");
        step=profile instanceof AttachmentDownloadFlowProfile?Step.FLOW_STEP:Step.DIRECT;
        if("LOCAL_GANGBUK_LEGAL_GET_V1".equals(profile.selectProfileCode())) {
            String path=request.uri().getPath();
            if("GET".equals(request.method()) && "/emwp/jsp/ofr/FileDown.jsp".equals(path)) step=Step.BRIDGE_GET;
            else if("POST".equals(request.method()) && "/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do".equals(path)) step=Step.PERIOD_POST;
            else if("POST".equals(request.method()) && "/emwp/jsp/ofr/FDSendNewPbs.jsp".equals(path)) step=Step.FINAL_POST;
        }
        invocations++;phase=Phase.TRANSPORT;
    }
    void saveTransportCompleted() {
        if(phase!=Phase.TRANSPORT) throw new IllegalStateException("DOWNLOAD_TRACE_SEQUENCE_INVALID");
        completed++;phase=Phase.PROFILE_PROCESSING;
    }
    void saveComplete() {
        if(phase!=Phase.PROFILE_PROCESSING) throw new IllegalStateException("DOWNLOAD_TRACE_SEQUENCE_INVALID");
        phase=Phase.COMPLETE;
    }
    Snapshot selectSnapshot() {return new Snapshot(1,step,phase,invocations,completed);}
}
