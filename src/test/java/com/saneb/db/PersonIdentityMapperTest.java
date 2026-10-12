package com.saneb.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.saneb.config.typehandler.UuidTypeHandler;
import com.saneb.domain.consultation.dao.ConsultationDao;
import com.saneb.domain.consultation.vo.ConsultationPartnerLookupCondition;
import com.saneb.domain.consultation.vo.ConsultationReservationSearchCondition;
import com.saneb.domain.partnerverification.dao.PartnerVerificationDao;
import com.saneb.domain.partnerverification.vo.PartnerVerificationSearchCondition;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.util.UUID;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

/** 운영 연결 없이 임시 PostgreSQL에서 실제 이름·아이디 조인과 생성자 매핑을 검증합니다. */
@EnabledOnOs(OS.LINUX)
class PersonIdentityMapperTest {
    @Test void mapsConsultationAndVerificationNamesAndFiltersPagedAssigneeCandidates() throws Exception {
        try (var pg = EmbeddedPostgres.builder().setPort(0).setServerConfig("listen_addresses", "127.0.0.1").start()) {
            var ds = pg.getPostgresDatabase();
            Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
            var sql = new JdbcTemplate(ds);
            UUID member = UUID.randomUUID(), partner = UUID.randomUUID(), operator = UUID.randomUUID();
            for (var entry : java.util.Map.of(member, "member.one", partner, "partner.one", operator, "operator.two").entrySet()) {
                sql.update("INSERT INTO users(id,login_id,password_hash,name,status_code,password_reset_required) VALUES (?,?,'unused-fixture-hash','동명이인','ACTIVE',false)", entry.getKey(), entry.getValue());
            }
            sql.update("INSERT INTO user_roles(user_id,role_code) VALUES (?,'USER'),(?,'PARTNER'),(?,'OPERATOR'),(?,'ADMIN')", member, partner, operator, operator);
            UUID consultation = UUID.randomUUID(), unassigned = UUID.randomUUID(), verification = UUID.randomUUID();
            sql.update("INSERT INTO consultation_reservations(id,member_user_id,partner_user_id,request_note) VALUES (?,?,?,'가상 상담')", consultation, member, partner);
            sql.update("INSERT INTO consultation_reservations(id,member_user_id,request_note) VALUES (?,?,'미배정 가상 상담')", unassigned, member);
            sql.update("INSERT INTO partner_verifications(id,member_user_id,partner_user_id) VALUES (?,?,?)", verification, member, partner);
            var config = new Configuration(new Environment("isolated-identity-test", new JdbcTransactionFactory(), ds));
            config.getTypeHandlerRegistry().register(UUID.class, UuidTypeHandler.class);
            for (String resource : java.util.List.of("mapper/consultation/ConsultationMapper.xml", "mapper/partnerverification/PartnerVerificationMapper.xml")) {
                try (var input = new ClassPathResource(resource).getInputStream()) {
                    new XMLMapperBuilder(input, config, resource, config.getSqlFragments()).parse();
                }
            }
            try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
                var consultationDao = session.getMapper(ConsultationDao.class);
                var row = consultationDao.selectConsultationReservationDetails(consultation);
                assertThat(row.memberName()).isEqualTo("동명이인");
                assertThat(row.memberLoginId()).isEqualTo("member.one");
                assertThat(row.partnerLoginId()).isEqualTo("partner.one");
                assertThat(row.memberUserCode()).startsWith("USR-");
                assertThat(consultationDao.selectConsultationReservationDetails(unassigned).partnerName()).isNull();
                assertThat(consultationDao.selectConsultationReservationList(new ConsultationReservationSearchCondition(member,null,null,1,20,0)))
                        .hasSize(2).allSatisfy(item -> assertThat(item.memberLoginId()).isEqualTo("member.one"));
                var filter = new ConsultationPartnerLookupCondition("동명이인", 1, 0);
                assertThat(consultationDao.selectConsultationPartnerLookupCount(filter)).isEqualTo(2);
                var first = consultationDao.selectConsultationPartnerLookupList(filter);
                var second = consultationDao.selectConsultationPartnerLookupList(new ConsultationPartnerLookupCondition("동명이인",1,1));
                assertThat(first).hasSize(1); assertThat(second).hasSize(1);
                assertThat(first.getFirst().userId()).isNotEqualTo(second.getFirst().userId());
                assertThat(first.getFirst().userId()).isNotEqualTo(member);
                assertThat(consultationDao.selectConsultationPartnerLookupList(new ConsultationPartnerLookupCondition("PARTNER.ONE",10,0)))
                        .singleElement().satisfies(item -> assertThat(item.userId()).isEqualTo(partner));
                var verificationDao = session.getMapper(PartnerVerificationDao.class);
                var detail = verificationDao.selectPartnerVerificationDetails(verification);
                assertThat(detail.memberLoginId()).isEqualTo("member.one");
                assertThat(detail.partnerName()).isEqualTo("동명이인");
                assertThat(verificationDao.selectPartnerVerificationList(new PartnerVerificationSearchCondition(member,null,null,null,1,20,0)))
                        .singleElement().satisfies(item -> assertThat(item.partnerLoginId()).isEqualTo("partner.one"));
                session.clearCache();
                sql.update("UPDATE users SET status_code='LOCKED' WHERE id=?", partner);
                assertThat(consultationDao.selectConsultationPartnerLookupCount(filter)).isEqualTo(1);
                // 비활성 담당자의 기존 이력 표시는 지우지 않습니다.
                assertThat(consultationDao.selectConsultationReservationDetails(consultation).partnerLoginId()).isEqualTo("partner.one");
            }
        }
    }
}
