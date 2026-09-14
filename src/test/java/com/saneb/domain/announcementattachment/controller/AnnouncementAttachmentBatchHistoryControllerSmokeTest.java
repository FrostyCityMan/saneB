package com.saneb.domain.announcementattachment.controller;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.saneb.common.error.*;
import com.saneb.common.response.PageResponse;
import com.saneb.domain.announcementattachment.dto.AttachmentBatchHistoryResponses.*;
import com.saneb.domain.announcementattachment.service.AnnouncementAttachmentBatchHistoryService;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties="spring.flyway.enabled=false") @AutoConfigureMockMvc
class AnnouncementAttachmentBatchHistoryControllerSmokeTest {
    private static final UUID BATCH=UUID.randomUUID();private static final String PATH="/api/v2/admin/announcement-attachment-batches/"+BATCH+"/action-history";
    @Autowired MockMvc mvc;
    @MockitoBean AnnouncementAttachmentBatchHistoryService service;
    @ParameterizedTest @ValueSource(strings={"ADMIN","OPERATOR","APPROVER"})
    void rolesReceiveAnchoredPageWithoutActorReasonOrCurrentSuccessFields(String role) throws Exception {
        var entry=new Entry(UUID.randomUUID(),BATCH,"APPLICATION","PAUSE",4,UUID.randomUUID(),2,1,0,null,null,null,null,OffsetDateTime.now());
        when(service.selectActionList(any(),eq(BATCH),eq(5),eq(2),eq(1))).thenReturn(new History(BATCH,5,10,PageResponse.of(List.of(entry),2,1,3),0));
        mvc.perform(get(PATH+"?throughVersion=5&page=2&size=1").with(user("history-qa").roles(role))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.throughVersion").value(5)).andExpect(jsonPath("$.data.history.totalCount").value(3))
                .andExpect(jsonPath("$.data.history.items[0].actionCode").value("PAUSE")).andExpect(jsonPath("$.data.history.items[0].actorId").doesNotExist())
                .andExpect(jsonPath("$.data.history.items[0].reasonHash").doesNotExist()).andExpect(jsonPath("$.data.history.items[0].idempotencyKey").doesNotExist())
                .andExpect(jsonPath("$.data.history.items[0].appliedCount").doesNotExist());
    }
    @ParameterizedTest @ValueSource(strings={"USER","PARTNER","REVIEWER"})
    void externalRolesCannotRead(String role) throws Exception {mvc.perform(get(PATH).with(user("history-qa").roles(role))).andExpect(status().isForbidden());verifyNoInteractions(service);}
    @Test void anonymousCannotRead() throws Exception {mvc.perform(get(PATH)).andExpect(status().isUnauthorized());verifyNoInteractions(service);}
    @ParameterizedTest @ValueSource(strings={"throughVersion=bad","page=bad","size=bad","throughVersion=2147483648"})
    void malformedQueriesReturnWrapperWithoutEcho(String input) throws Exception {mvc.perform(get(PATH+"?"+input).with(user("history-qa").roles("ADMIN"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));verifyNoInteractions(service);}
    @Test void missingBatchAndFutureVersionRemain404And409() throws Exception {
        when(service.selectActionList(any(),eq(BATCH),isNull(),eq(1),eq(20))).thenThrow(new ApiException(ErrorCode.RESOURCE_NOT_FOUND,HttpStatus.NOT_FOUND,"배치 없음"));
        mvc.perform(get(PATH).with(user("history-qa").roles("ADMIN"))).andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));
        when(service.selectActionList(any(),eq(BATCH),eq(9),eq(1),eq(20))).thenThrow(new ApiException(ErrorCode.ANNOUNCEMENT_ATTACHMENT_VERSION_CONFLICT,HttpStatus.CONFLICT,"기준 버전 확인"));
        mvc.perform(get(PATH+"?throughVersion=9").with(user("history-qa").roles("ADMIN"))).andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
    }
}
