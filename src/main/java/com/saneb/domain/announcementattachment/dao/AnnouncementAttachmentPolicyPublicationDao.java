package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublication.Receipt;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyPublicationRows.*;
import java.util.UUID;
import org.apache.ibatis.annotations.Param;

public interface AnnouncementAttachmentPolicyPublicationDao {
    Request selectRequestDetails(@Param("key") UUID key);
    String selectPublicationLock();
    int insertPublication(Insert command);
    int updatePreviousPolicyRetired(@Param("publicationId") UUID publicationId);
    int updatePolicyActive(@Param("publicationId") UUID publicationId);
    Receipt selectReceiptDetails(@Param("policyId") UUID policyId);
}
