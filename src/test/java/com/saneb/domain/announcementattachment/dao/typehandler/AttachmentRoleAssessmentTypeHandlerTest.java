package com.saneb.domain.announcementattachment.dao.typehandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier;
import com.saneb.domain.announcementattachment.vo.AttachmentSetEvidence;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AttachmentRoleAssessmentTypeHandlerTest {
    private final ObjectMapper json=new ObjectMapper();
    private final AttachmentRoleAssessmentTypeHandler handler=new AttachmentRoleAssessmentTypeHandler();
    private AttachmentDocumentRoleClassifier.Assessment assessment() {
        String text="지원사업 공고\n지원대상: 소상공인\n지원내용: 지원금\n신청기간: 9월";
        return new AttachmentDocumentRoleClassifier().selectAssessment(new AttachmentSetEvidence.Extraction("COMPLETE_TEXT",text,
                List.of(new AttachmentSetEvidence.Block(0,0,text.length(),"p:1",true,"p:1")),1,1));
    }
    @Test void exactAssessmentRoundTripsAndLegacyNullRemainsAbsent() throws Exception {
        var result=mock(ResultSet.class);when(result.getString("role_assessment_json")).thenReturn(null,json.writeValueAsString(assessment()));
        assertThat(handler.getNullableResult(result,"role_assessment_json")).isNull();
        assertThat(handler.getNullableResult(result,"role_assessment_json")).isEqualTo(assessment());
    }
    @Test void arbitraryFieldsDuplicateKeysMalformedOffsetsAndContradictoryRolesAreRejectedWithoutRawLeak() throws Exception {
        String valid=json.writeValueAsString(assessment());
        var extra=json.valueToTree(assessment()).deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)extra).put("rawText","PRIVATE_CANARY");
        var stringOffset=json.valueToTree(assessment()).deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)stringOffset.path("evidence").get(0)).put("startOffset","0");
        var wrongReason=json.valueToTree(assessment()).deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)wrongReason).put("reasonCode","STRUCTURE_UNCERTAIN");
        for(String value:List.of("", "null", "[]", valid+" {}", extra.toString(),stringOffset.toString(),wrongReason.toString(),
                valid.replace("\"blockIndex\":0","\"blockIndex\":0,\"blockIndex\":0")," ".repeat(32769))) {
            var result=mock(ResultSet.class);when(result.getString(1)).thenReturn(value);
            assertThatThrownBy(()->handler.getNullableResult(result,1)).isInstanceOf(SQLException.class).hasMessageNotContaining("PRIVATE_CANARY");
        }
    }
}
