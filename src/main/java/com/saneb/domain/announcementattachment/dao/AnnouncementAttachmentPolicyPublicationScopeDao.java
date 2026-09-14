package com.saneb.domain.announcementattachment.dao;

import com.saneb.domain.announcementattachment.dto.AttachmentPolicyPublicationScope.*;
import com.saneb.domain.announcementattachment.vo.AttachmentPolicyPublicationScopeRows.*;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Param;

public interface AnnouncementAttachmentPolicyPublicationScopeDao {
    Request selectRequestDetails(@Param("key") UUID key);
    int insertScope(Insert command);
    long insertScopeMembers(@Param("scopeId") UUID scopeId,@Param("policyId") UUID policyId);
    int updateScopeSealed(@Param("scopeId") UUID scopeId);
    Summary selectScopeDetails(@Param("policyId") UUID policyId,@Param("scopeId") UUID scopeId);
    boolean selectScopeCurrent(@Param("scopeId") UUID scopeId);
    List<Summary> selectScopeList(Search search);
    long selectScopeCount(Search search);
    List<Item> selectItemList(Search search);
}
