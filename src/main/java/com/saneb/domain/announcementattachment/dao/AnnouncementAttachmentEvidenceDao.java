package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.vo.AttachmentEvidenceCommands;
import com.saneb.domain.announcementattachment.vo.AttachmentSetRow;
import com.saneb.domain.announcementattachment.vo.AttachmentFileSummaryRow;
import com.saneb.domain.announcementattachment.vo.AttachmentBlockRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementAttachmentEvidenceDao {
    String selectFileCheckpoint(@Param("jobId") UUID jobId,@Param("leaseToken") UUID leaseToken,@Param("locatorHash") String locatorHash);
    int insertFileCheckpoint(@Param("jobId") UUID jobId,@Param("leaseToken") UUID leaseToken,
                             @Param("locatorHash") String locatorHash,@Param("fileJson") String fileJson);
    int deleteFileCheckpoints(@Param("jobId") UUID jobId);
    long selectSetCount(@Param("sourceId") UUID sourceId);
    List<AttachmentSetRow> selectSetList(@Param("sourceId") UUID sourceId, @Param("offset") int offset, @Param("size") int size);
    long selectFileCount(@Param("sourceId") UUID sourceId, @Param("setId") UUID setId);
    List<AttachmentFileSummaryRow> selectFileList(@Param("sourceId") UUID sourceId, @Param("setId") UUID setId,
                                               @Param("offset") int offset, @Param("size") int size);
    Long selectExtractionBlockCount(@Param("sourceId") UUID sourceId, @Param("extractionId") UUID extractionId);
    List<AttachmentBlockRow> selectBlockList(@Param("sourceId") UUID sourceId, @Param("extractionId") UUID extractionId,
                                           @Param("offset") int offset, @Param("size") int size,
                                           @Param("textOffset") int textOffset, @Param("textLimit") int textLimit);
    AttachmentSetRow selectSetDetails(@Param("sourceId") UUID sourceId, @Param("setId") UUID setId);
    int insertSet(AttachmentEvidenceCommands.SetInsert command);
    int insertFile(AttachmentEvidenceCommands.FileInsert command);
    int insertExtraction(AttachmentEvidenceCommands.ExtractionInsert command);
    int updateSetSealed(@Param("setId") UUID setId, @Param("manifestHash") String manifestHash,
                        @Param("discoveryStatus") String discoveryStatus, @Param("complete") boolean complete,
                        @Param("count") int count, @Param("warningsJson") String warningsJson);
    int updateJobSet(@Param("jobId") UUID jobId, @Param("leaseToken") UUID leaseToken, @Param("setId") UUID setId);
}
