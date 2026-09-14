package com.saneb.domain.announcementattachment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.saneb.common.error.ApiException;
import com.saneb.common.error.ErrorCode;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentHistoryResponses;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentHistoryService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties="spring.flyway.enabled=false")
@AutoConfigureMockMvc
class AnnouncementAttachmentHistoryControllerSmokeTest {
    private static final UUID SOURCE=UUID.randomUUID(),EVALUATION=UUID.randomUUID(),FILE=UUID.randomUUID();
    private static final String ROOT="/api/v2/admin/announcement-sources/"+SOURCE+"/attachment-classification";
    @Autowired MockMvc mvc;
    @MockitoBean AnnouncementAttachmentHistoryService service;
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void allReadRolesReceivePagedHistoryAndExactInputMatchesWithoutCache(String role) throws Exception {
        when(service.selectEvaluationList(SOURCE,1,20)).thenReturn(PageResponse.of(List.of(),1,20,0));
        when(service.selectEvaluationDetails(SOURCE,EVALUATION)).thenReturn(new AttachmentHistoryResponses.Details(null,List.of(),List.of(),0,0,2,3));
        when(service.selectInputList(SOURCE,EVALUATION,1,20)).thenReturn(PageResponse.of(List.of(),1,20,0));
        when(service.selectMatchList(SOURCE,EVALUATION,null,1,20)).thenReturn(PageResponse.of(List.of(),1,20,0));
        for(String suffix:List.of("/history","/"+EVALUATION,"/"+EVALUATION+"/inputs","/"+EVALUATION+"/matches")) {
            mvc.perform(get(ROOT+suffix).with(user("qa").roles(role)))
                    .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                    .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data").exists());
        }
        verify(service).selectEvaluationList(SOURCE,1,20);verify(service).selectEvaluationDetails(SOURCE,EVALUATION);
        verify(service).selectInputList(SOURCE,EVALUATION,1,20);verify(service).selectMatchList(SOURCE,EVALUATION,null,1,20);
    }
    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER"})
    void externalRolesCannotReadAnyHistoryRoute(String role) throws Exception {
        for(String suffix:List.of("/history","/"+EVALUATION,"/"+EVALUATION+"/inputs","/"+EVALUATION+"/matches"))
            mvc.perform(get(ROOT+suffix).with(user("qa").roles(role))).andExpect(status().isForbidden()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(service);
    }
    @Test void anonymousHistoryAndMatchesAreDeniedBeforeService() throws Exception {
        mvc.perform(get(ROOT+"/history")).andExpect(status().isUnauthorized());
        mvc.perform(get(ROOT+"/"+EVALUATION+"/matches")).andExpect(status().isUnauthorized());verifyNoInteractions(service);
    }
    @Test void malformedUuidAndPageHaveSpecificSafeErrors() throws Exception {
        mvc.perform(get(ROOT+"/bad-uuid/matches").with(user("qa").roles("ADMIN"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("UUID")));
        mvc.perform(get(ROOT+"/history?page=abc").with(user("qa").roles("ADMIN"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("정수")));
        mvc.perform(get(ROOT+"/"+EVALUATION+"/matches?fileId=bad-uuid").with(user("qa").roles("ADMIN"))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void foreignFileIsNotFoundWrapperAndDoesNotBecomeEmpty200() throws Exception {
        when(service.selectMatchList(SOURCE,EVALUATION,FILE,1,20)).thenThrow(new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"이 판정의 파일이 아닙니다."));
        mvc.perform(get(ROOT+"/"+EVALUATION+"/matches").param("fileId",FILE.toString()).with(user("qa").roles("APPROVER")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("RESOURCE_NOT_FOUND"));
    }
    @Test void matchResponseExposesCoordinatesButNoWholeTextOrPrivateOperationalFields() throws Exception {
        var match=new AttachmentHistoryResponses.Match(UUID.randomUUID(),FILE,UUID.randomUUID(),"NOTICE",UUID.randomUUID(),"BUSINESS",
                UUID.randomUUID(),"BUSINESS_SMALL",UUID.randomUUID(),"소상공인","TAG",2,10,14);
        when(service.selectMatchList(SOURCE,EVALUATION,FILE,2,10)).thenReturn(PageResponse.of(List.of(match),2,10,11));
        mvc.perform(get(ROOT+"/"+EVALUATION+"/matches").param("fileId",FILE.toString()).param("page","2").param("size","10")
                        .with(user("qa").roles("OPERATOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(11)).andExpect(jsonPath("$.data.items[0].blockIndex").value(2))
                .andExpect(jsonPath("$.data.items[0].termText").value("소상공인"))
                .andExpect(jsonPath("$.data.items[0].text").doesNotExist()).andExpect(jsonPath("$.data.items[0].extractedText").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].fetchUrl").doesNotExist()).andExpect(jsonPath("$.data.items[0].leaseToken").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].reviewNote").doesNotExist());
    }
}
