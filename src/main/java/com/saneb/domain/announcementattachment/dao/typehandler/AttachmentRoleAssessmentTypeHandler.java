package com.saneb.domain.announcementattachment.dao.typehandler;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.classification.AttachmentDocumentRoleClassifier.Assessment;
import java.sql.*;
import java.util.Set;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/** 고정 필드의 역할 근거만 반환한다. 과거 행의 NULL을 자동 판정 성공으로 채우지 않는다. */
public final class AttachmentRoleAssessmentTypeHandler extends BaseTypeHandler<Assessment> {
    private static final ObjectMapper JSON=new ObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            DeserializationFeature.FAIL_ON_TRAILING_TOKENS).disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    @Override public void setNonNullParameter(PreparedStatement statement,int index,Assessment value,JdbcType type) throws SQLException {
        try { String json=JSON.writeValueAsString(value); selectAssessment(json);statement.setObject(index,json,Types.OTHER); }
        catch(java.io.IOException exception){throw invalid();}
    }
    @Override public Assessment getNullableResult(ResultSet result,String column) throws SQLException {return selectAssessment(result.getString(column));}
    @Override public Assessment getNullableResult(ResultSet result,int column) throws SQLException {return selectAssessment(result.getString(column));}
    @Override public Assessment getNullableResult(CallableStatement result,int column) throws SQLException {return selectAssessment(result.getString(column));}
    private Assessment selectAssessment(String value) throws SQLException {
        if(value==null)return null;
        try {
            if(value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>32768)throw invalid();
            var node=JSON.readTree(value);
            if(node==null || !node.isObject() || node.size()!=7 || !node.path("evidence").isArray() || node.path("evidence").size()>100)throw invalid();
            for(String field:Set.of("ruleVersion","rulesHash","textHash","blocksHash","roleCode","reasonCode"))
                if(!node.path(field).isTextual())throw invalid();
            for(var proof:node.path("evidence")) {
                if(!proof.isObject() || proof.size()!=4 || !proof.path("ruleCode").isTextual())throw invalid();
                for(String field:Set.of("blockIndex","startOffset","endOffset"))
                    if(!proof.path(field).isIntegralNumber() || !proof.path(field).canConvertToInt())throw invalid();
            }
            var result=JSON.treeToValue(node,Assessment.class);
            if(!result.ruleVersion().matches("[A-Za-z0-9_.-]{1,40}") || !hash(result.rulesHash()) || !hash(result.textHash()) || !hash(result.blocksHash())
                    || !Set.of("NOTICE","GUIDE","FORM","REFERENCE","UNKNOWN").contains(result.roleCode())
                    || !Set.of("ROLE_TEXT_STRUCTURE_MATCHED","STRUCTURE_UNCERTAIN","ROLE_ANALYSIS_LIMIT","MIXED_DOCUMENT_ROLES",
                        "INITIAL_HEADING_REQUIRED","ROLE_STRUCTURE_INCOMPLETE").contains(result.reasonCode()))throw invalid();
            if ("UNKNOWN".equals(result.roleCode()) ? "ROLE_TEXT_STRUCTURE_MATCHED".equals(result.reasonCode())
                    : !"ROLE_TEXT_STRUCTURE_MATCHED".equals(result.reasonCode()) || result.evidence().size()<3 || result.evidence().size()>4)throw invalid();
            for(var proof:result.evidence()) if(!Set.of("NOTICE_HEADING","GUIDE_HEADING","FORM_HEADING","REFERENCE_HEADING","TARGET_SECTION",
                    "SUPPORT_SECTION","APPLICATION_SECTION","APPLICANT_FIELD","SIGNATURE_FIELD","QUESTION_ITEM","ANSWER_ITEM").contains(proof.ruleCode())
                    || proof.blockIndex()<0 || proof.blockIndex()>=20000 || proof.startOffset()<0 || proof.endOffset()<=proof.startOffset()
                    || proof.endOffset()>1_000_000)throw invalid();
            return result;
        }catch(java.io.IOException|IllegalArgumentException exception){throw invalid();}
    }
    private static boolean hash(String value){return value!=null && value.matches("[0-9a-f]{64}");}
    private static SQLException invalid(){return new SQLException("문서 역할 근거의 고정 필드·규칙 지문·위치 형식이 올바르지 않습니다.");}
}
