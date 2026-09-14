package com.saneb.domain.announcementattachment.dao.typehandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/** 첨부 고정 경고 코드 배열만 취급한다. 범용 원문 JSON 노출에 사용하지 않는다. */
public final class AttachmentWarningCodesTypeHandler extends BaseTypeHandler<List<String>> {
    private static final ObjectMapper MAPPER=new ObjectMapper();
    @Override public void setNonNullParameter(PreparedStatement statement,int index,List<String> value,JdbcType type) throws SQLException {
        try { statement.setObject(index,MAPPER.writeValueAsString(value),java.sql.Types.OTHER); }
        catch (java.io.IOException exception) { throw new SQLException("첨부 경고 코드를 저장할 수 없습니다."); }
    }
    @Override public List<String> getNullableResult(ResultSet result,String column) throws SQLException { return selectCodes(result.getString(column)); }
    @Override public List<String> getNullableResult(ResultSet result,int column) throws SQLException { return selectCodes(result.getString(column)); }
    @Override public List<String> getNullableResult(CallableStatement result,int column) throws SQLException { return selectCodes(result.getString(column)); }
    private List<String> selectCodes(String value) throws SQLException {
        if (value==null) return List.of();
        try {
            if (value.length()>2000) throw new IllegalArgumentException();
            var node=MAPPER.readTree(value);
            if (!node.isArray() || node.size()>20) throw new IllegalArgumentException();
            var codes=new ArrayList<String>();
            for (var code:node) {
                if (!code.isTextual() || !code.textValue().matches("[A-Z][A-Z0-9_]{0,79}")) throw new IllegalArgumentException();
                codes.add(code.textValue());
            }
            return List.copyOf(codes);
        } catch (java.io.IOException | IllegalArgumentException exception) { throw new SQLException("첨부 경고 코드 형식이 올바르지 않습니다."); }
    }
}
