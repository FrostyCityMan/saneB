package com.saneb.db;

import static org.assertj.core.api.Assertions.*;

import com.saneb.config.typehandler.UuidTypeHandler;
import com.saneb.domain.auth.vo.AuthUserDetailsRow;
import com.saneb.domain.auth.vo.AuthenticatedUserDetails;
import com.saneb.domain.consultation.dao.ConsultationDao;
import com.saneb.domain.consultation.dto.ConsultationManagementStatusRequest;
import com.saneb.domain.consultation.service.impl.ConsultationServiceImpl;
import com.saneb.domain.consultation.vo.ConsultationReservationStatusCommand;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.support.TransactionTemplate;

/** 명시적으로 만든 loopback 임시 PostgreSQL만 사용합니다. 운영 연결값은 읽지 않습니다. */
@EnabledIfEnvironmentVariable(named = "SANEB_CONSULTATION_DB_QA_PORT", matches = "[0-9]{4,5}")
class ConsultationManagementDbTest {
    @Test void conditionalSaveAndHistoryFailureRollbackUseRealPostgres() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:"
                + System.getenv("SANEB_CONSULTATION_DB_QA_PORT") + "/consultation_qa", "postgres", "");
        var sql = new JdbcTemplate(ds);
        assertThat(sql.queryForObject("SELECT current_database()", String.class)).isEqualTo("consultation_qa");
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        UUID member = UUID.randomUUID(), partner = UUID.randomUUID(), reservation = UUID.randomUUID(), slot = UUID.randomUUID();
        sql.update("INSERT INTO users(id,login_id,password_hash,name,status_code,password_reset_required) VALUES (?,?,'unused-fixture','가상 회원','ACTIVE',false),(?,?,'unused-fixture','가상 담당','ACTIVE',false)", member, "qa." + member, partner, "qa." + partner);
        sql.update("INSERT INTO user_roles(user_id,role_code) VALUES (?,'USER'),(?,'ADMIN')", member, partner);
        sql.update("INSERT INTO partner_availability_slots(id,partner_user_id,start_at,end_at,status_code) VALUES (?,?,now()+interval '1 day',now()+interval '2 days','HELD')", slot, partner);
        sql.update("INSERT INTO consultation_reservations(id,member_user_id,partner_user_id,slot_id,status_code,request_note) VALUES (?,?,?,?,'CONFIRMED','가상 요청')", reservation, member, partner, slot);
        var config = new org.apache.ibatis.session.Configuration();
        config.getTypeHandlerRegistry().register(UUID.class, UuidTypeHandler.class);
        var factory = new SqlSessionFactoryBean(); factory.setDataSource(ds); factory.setConfiguration(config);
        factory.setMapperLocations(new ClassPathResource("mapper/consultation/ConsultationMapper.xml"));
        var session = new SqlSessionTemplate(factory.getObject()); var dao = session.getMapper(ConsultationDao.class);
        var service = new ConsultationServiceImpl(dao);
        var tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        var principal = new AuthenticatedUserDetails(new AuthUserDetailsRow(partner, "qa", "unused", "가상 담당", "ACTIVE", false, null, null, null), List.of("ADMIN"));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        var before = dao.selectConsultationReservationDetails(reservation);
        assertThat(dao.selectAssignablePartnerCount(member)).isZero();
        assertThat(dao.selectAssignablePartnerCount(partner)).isEqualTo(1);
        // 이력 기록에서 실패하게 하여 예약·slot 변경도 함께 rollback되는지 검증합니다.
        sql.execute("CREATE FUNCTION qa_reject_history() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'isolated rollback test'; END $$");
        sql.execute("CREATE TRIGGER qa_reject_history BEFORE INSERT ON consultation_histories FOR EACH ROW EXECUTE FUNCTION qa_reject_history()");
        var request = new ConsultationManagementStatusRequest("COMPLETED", null, "검증", before.updatedAt());
        assertThatThrownBy(() -> tx.execute(status -> service.updateConsultationManagementStatus(auth, reservation, request))).isInstanceOf(RuntimeException.class);
        assertThat(dao.selectConsultationReservationDetails(reservation).statusCode()).isEqualTo("CONFIRMED");
        assertThat(dao.selectConsultationReservationDetails(reservation).updatedAt()).isEqualTo(before.updatedAt());
        assertThat(sql.queryForObject("SELECT status_code FROM partner_availability_slots WHERE id=?", String.class, slot)).isEqualTo("HELD");
        assertThat(sql.queryForObject("SELECT count(1) FROM consultation_histories WHERE reservation_id=?", Long.class, reservation)).isZero();
        sql.execute("DROP TRIGGER qa_reject_history ON consultation_histories");
        sql.execute("DROP FUNCTION qa_reject_history()");
        var saved = tx.execute(status -> service.updateConsultationManagementStatus(auth, reservation, request));
        assertThat(saved.reservation().statusCode()).isEqualTo("COMPLETED");
        assertThat(saved.reservation().updatedAt()).isAfter(before.updatedAt());
        assertThat(saved.allowedNextStatusCodes()).isEmpty();
        assertThat(sql.queryForObject("SELECT status_code FROM partner_availability_slots WHERE id=?", String.class, slot)).isEqualTo("CLOSED");
        assertThat(sql.queryForObject("SELECT count(1) FROM consultation_histories WHERE reservation_id=?", Long.class, reservation)).isEqualTo(1);
        assertThatThrownBy(() -> tx.execute(status -> service.updateConsultationManagementStatus(auth, reservation, request))).hasMessageContaining("다른 사용자가");
        // 기존 호출자도 동일 snapshot을 재사용하면 실제 WHERE 버전 조건에서 0건입니다.
        assertThat(dao.updateConsultationReservationStatus(new ConsultationReservationStatusCommand(reservation, "CANCELED", null, null, null, partner, before.updatedAt()))).isZero();
        assertThat(sql.queryForObject("SELECT count(1) FROM consultation_histories WHERE reservation_id=?", Long.class, reservation)).isEqualTo(1);
        assertThat(dao.selectConsultationReservationDetails(reservation).statusCode()).isEqualTo("COMPLETED");
    }
}
