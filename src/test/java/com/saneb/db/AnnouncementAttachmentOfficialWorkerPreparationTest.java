package com.saneb.db;

import static org.junit.jupiter.api.Assertions.*;
import com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier;
import com.saneb.domain.announcementattachment.dao.AnnouncementAttachmentJobDao;
import com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentJobService;
import com.saneb.domain.announcementattachment.vo.AttachmentExecutionSnapshot;
import com.saneb.domain.announcementsource.classification.*;
import com.saneb.domain.announcementsource.classification.AnnouncementSourceClassificationCodes.*;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 합성 본문으로 공개 worker 시험의 실제 DB 준비 경로만 검증한다. HTTP/추출 성공 증거가 아니다. */
class AnnouncementAttachmentOfficialWorkerPreparationTest {
    @ParameterizedTest @ValueSource(strings={"YANGPYEONG","CHUNGJU","JECHEON","BOEUN","BOEUN_SEGMENT"})
    void realClassificationPersistenceProducesReservableVersionedLocalSource(String group) throws Exception {
        try {
            AnnouncementAttachmentOfficialWorkerIntegrationTest.startDatabase();
            var engine=new AnnouncementSourceClassificationEngine();
            var rules=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectRules();
            var samples=AnnouncementAttachmentBbsOfficialObservationTest.selectCases(AnnouncementAttachmentOfficialWorkerProbe.selectObservationGroup(group)).toList();
            for(var candidate:samples) {
                var title=engine.selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",candidate.title(),
                        null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules);
                assertEquals(AnnouncementAttachmentOfficialWorkerProbe.selectTitleStopExpected(candidate.code()),
                        AnnouncementAttachmentOfficialWorkerIntegrationTest.selectPlannedTitleStop(candidate,title));
            }
            var sample=samples.stream().filter(s->!AnnouncementAttachmentOfficialWorkerProbe.selectTitleStopExpected(s.code())).findFirst().orElseThrow();
            var unexpectedStop=engine.selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE","청년 수출 지원",
                    null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules);
            assertThrows(AssertionError.class,()->AnnouncementAttachmentOfficialWorkerIntegrationTest.selectPlannedTitleStop(sample,unexpectedStop));
            var expectedCandidate=engine.selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),
                    null,null,List.of(),BodySourceCode.NONE,BodyAvailabilityCode.UNAVAILABLE),rules);
            for(var negative:samples.stream().filter(s->AnnouncementAttachmentOfficialWorkerProbe.selectTitleStopExpected(s.code())).toList()) {
                assertThrows(AssertionError.class,()->AnnouncementAttachmentOfficialWorkerIntegrationTest.selectPlannedTitleStop(negative,expectedCandidate));
                assertThrows(AssertionError.class,()->AnnouncementAttachmentOfficialWorkerIntegrationTest.selectPlannedTitleStop(negative,unexpectedStop));
            }
            var base=new AnnouncementSourceClassificationEngine().selectDecision(new AnnouncementSourceClassificationInput("LOCAL_GOV_NOTICE",sample.title(),
                    "지원대상: 소상공인. 지원내용: 지원금. 수출기업 포함 여부는 관리자 검수 필요.",null,List.of(),BodySourceCode.DETAIL_PAGE_TEXT,BodyAvailabilityCode.AVAILABLE),
                    AnnouncementAttachmentOfficialWorkerIntegrationTest.selectRules());
            assertEquals(SemanticStatusCode.REVIEW_REQUIRED,base.semanticStatusCode());
            var execution=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectExecution(group,sample,"1.0.5","b".repeat(64));
            var request=AnnouncementAttachmentOfficialWorkerIntegrationTest.insertSourceRequest(sample,
                    "지원대상: 소상공인. 지원내용: 지원금. 수출기업 포함 여부는 관리자 검수 필요.",base,execution);
            var service=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectService(AnnouncementAttachmentJobService.class);
            var job=service.insertAttachmentJob(request);
            assertNotNull(job.jobId());
            var current=AnnouncementAttachmentOfficialWorkerIntegrationTest.selectService(AnnouncementAttachmentJobDao.class).selectSourceContextDetails(request.sourceId());
            assertEquals(request.expectedBaseDecisionId(),current.baseEvaluationId());
            assertEquals(request.expectedSourceVersion(),current.sourceVersion());
            assertEquals("COMBINATION_MATCHED",current.titleStageCode());
            assertEquals("REVIEW_REQUIRED",current.semanticStatusCode());
            assertTrue(current.attachmentReviewRequired());
        } finally {AnnouncementAttachmentOfficialWorkerIntegrationTest.stop();}
    }
}
