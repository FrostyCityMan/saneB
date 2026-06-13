package com.saneb.domain.consultation.dao;

import com.saneb.domain.consultation.vo.ApplicationProgressOwnerRow;
import com.saneb.domain.consultation.vo.AuditLogCommand;
import com.saneb.domain.consultation.vo.ConsultationHistoryCommand;
import com.saneb.domain.consultation.vo.ConsultationReservationInsertCommand;
import com.saneb.domain.consultation.vo.ConsultationReservationRow;
import com.saneb.domain.consultation.vo.ConsultationReservationSearchCondition;
import com.saneb.domain.consultation.vo.ConsultationReservationStatusCommand;
import com.saneb.domain.consultation.vo.ConsultationSlotInsertCommand;
import com.saneb.domain.consultation.vo.ConsultationSlotRow;
import com.saneb.domain.consultation.vo.ConsultationSlotSearchCondition;
import com.saneb.domain.consultation.vo.ConsultationSlotStatusCommand;
import com.saneb.domain.consultation.vo.PartnerVerificationOwnerRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConsultationDao {

    long selectUserCount(@Param("userId") UUID userId);

    UUID selectUserIdByPublicCode(@Param("publicCode") String publicCode);

    List<ConsultationSlotRow> selectConsultationSlotList(ConsultationSlotSearchCondition condition);

    long selectConsultationSlotCount(ConsultationSlotSearchCondition condition);

    ConsultationSlotRow selectConsultationSlotDetails(@Param("slotId") UUID slotId);

    void insertConsultationSlot(ConsultationSlotInsertCommand command);

    int updateConsultationSlotStatus(ConsultationSlotStatusCommand command);

    List<ConsultationReservationRow> selectConsultationReservationList(
            ConsultationReservationSearchCondition condition
    );

    long selectConsultationReservationCount(ConsultationReservationSearchCondition condition);

    ConsultationReservationRow selectConsultationReservationDetails(@Param("reservationId") UUID reservationId);

    void insertConsultationReservation(ConsultationReservationInsertCommand command);

    int updateConsultationReservationStatus(ConsultationReservationStatusCommand command);

    void insertConsultationHistory(ConsultationHistoryCommand command);

    ApplicationProgressOwnerRow selectApplicationProgressOwner(@Param("progressId") UUID progressId);

    PartnerVerificationOwnerRow selectPartnerVerificationOwner(@Param("verificationId") UUID verificationId);

    void insertAuditLog(AuditLogCommand command);
}
