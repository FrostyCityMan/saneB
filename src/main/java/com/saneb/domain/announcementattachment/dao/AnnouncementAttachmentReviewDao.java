package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentReviewRows;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementAttachmentReviewDao {
    AttachmentReviewRows.Confirmation selectIdempotentConfirmationDetails(@Param("key") UUID key);
    AttachmentReviewRows.Confirmation selectConfirmationDetails(@Param("sourceId") UUID sourceId,@Param("confirmationId") UUID confirmationId);
    AttachmentReviewRows.RestoredBinding selectRestoredBindingDetails(@Param("sourceId") UUID sourceId,@Param("confirmationId") UUID confirmationId);
    boolean selectActiveNormalJobExists(@Param("sourceId") UUID sourceId);
    List<String> selectEnabledTargetCodeList(@Param("codes") List<String> codes);
    List<String> selectEnabledSupportCodeList(@Param("codes") List<String> codes);
    int updateConfirmationsStale(@Param("sourceId") UUID sourceId);
    int updateSourceVersion(@Param("sourceId") UUID sourceId,@Param("sourceVersion") int sourceVersion,
                            @Param("attachmentVersion") int attachmentVersion,@Param("evaluationId") UUID evaluationId);
    int insertConfirmation(AttachmentReviewRows.ConfirmationInsert command);
    int insertConfirmedTarget(AttachmentReviewRows.Tag command);
    int insertConfirmedSupport(AttachmentReviewRows.Tag command);
    List<String> selectConfirmedTargetCodeList(@Param("confirmationId") UUID confirmationId);
    List<String> selectConfirmedSupportCodeList(@Param("confirmationId") UUID confirmationId);
    AttachmentReviewRows.Link selectConversionLinkDetails(@Param("sourceId") UUID sourceId);
    int insertConversionLink(AttachmentReviewRows.LinkInsert command);
}
