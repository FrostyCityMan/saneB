/*
 * Copyright (c) 2026 범데이터소프트. All rights reserved.
 *
 * 본 소프트웨어 및 관련 문서는 범데이터소프트의 지식재산입니다.
 * 사전 서면 동의 없이 본 파일의 복제, 수정, 배포, 공개, 사용을 금지합니다.
 *
 * 프로젝트명: saneB
 * 파일명: UuidTypeHandler.java
 * 작성자: 김도훈
 *
 */

package com.saneb.config.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(UUID.class)
public class UuidTypeHandler extends BaseTypeHandler<UUID> {

    /**
     * 업무 처리를 수행합니다.
     *
     * @param preparedStatement 입력 값
     *
     * @param index 입력 값
     *
     * @param parameter 입력 값
     *
     * @param jdbcType 입력 값
     *
     * @throws SQLException 처리 중 예외가 발생한 경우
     */
    @Override
    public void setNonNullParameter(
            PreparedStatement preparedStatement,
            int index,
            UUID parameter,
            JdbcType jdbcType
    ) throws SQLException {
        preparedStatement.setObject(index, parameter);
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param resultSet 입력 값
     *
     * @param columnName 입력 값
     *
     * @return 처리 결과
     *
     * @throws SQLException 처리 중 예외가 발생한 경우
     */
    @Override
    public UUID getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return toUuid(resultSet.getObject(columnName));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param resultSet 입력 값
     *
     * @param columnIndex 입력 값
     *
     * @return 처리 결과
     *
     * @throws SQLException 처리 중 예외가 발생한 경우
     */
    @Override
    public UUID getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return toUuid(resultSet.getObject(columnIndex));
    }

    /**
     * 업무 처리를 수행합니다.
     *
     * @param callableStatement 입력 값
     *
     * @param columnIndex 입력 값
     *
     * @return 처리 결과
     *
     * @throws SQLException 처리 중 예외가 발생한 경우
     */
    @Override
    public UUID getNullableResult(CallableStatement callableStatement, int columnIndex) throws SQLException {
        return toUuid(callableStatement.getObject(columnIndex));
    }

    /**
     * 업무 데이터를 응답 형식으로 변환합니다.
     *
     * @param value 입력 값
     *
     * @return 처리 결과
     */
    private UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }
}
