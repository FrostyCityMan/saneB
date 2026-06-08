package com.saneb.domain.aiassist.dao;

import com.saneb.domain.aiassist.vo.AiAssistInsertCommand;
import com.saneb.domain.aiassist.vo.AiAssistResultInsertCommand;
import com.saneb.domain.aiassist.vo.AiAssistRow;
import com.saneb.domain.aiassist.vo.AiAssistSearchCondition;
import com.saneb.domain.aiassist.vo.AuditLogCommand;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiAssistDao {

    void insertAiAssistRequest(AiAssistInsertCommand command);

    void insertAiAssistResult(AiAssistResultInsertCommand command);

    List<AiAssistRow> selectAiAssistList(AiAssistSearchCondition condition);

    long selectAiAssistCount(AiAssistSearchCondition condition);

    AiAssistRow selectAiAssistDetails(@Param("requestId") UUID requestId);

    AiAssistRow selectAiAssistDetailsByResultId(@Param("resultId") UUID resultId);

    void updateAiAssistResultReviewStatus(
            @Param("resultId") UUID resultId,
            @Param("reviewStatusCode") String reviewStatusCode,
            @Param("actorUserId") UUID actorUserId
    );

    void insertAuditLog(AuditLogCommand command);
}
