"""고정 함안 관측의 기존 서버 보고 계약·현재 실행 시각·JUnit 누락을 검증한다."""
import datetime
import json
import pathlib
import runpy
import sys
import time
import xml.etree.ElementTree as ET


def validate(report, junit, started_ms, ended_ms):
    if type(started_ms) is not int or type(ended_ms) is not int or not 0 < started_ms <= ended_ms:
        raise ValueError('HAMAN_EXECUTION_TIME_INVALID')
    observed = report.get('observedAt')
    if not isinstance(observed, str):
        raise ValueError('HAMAN_OBSERVATION_TIME_INVALID')
    try:
        observed_at = datetime.datetime.fromisoformat(observed.replace('Z', '+00:00'))
        if observed_at.tzinfo is None or not started_ms <= observed_at.timestamp() * 1000 <= ended_ms:
            raise ValueError('HAMAN_OBSERVATION_TIME_INVALID')
    except (ValueError, OverflowError):
        raise ValueError('HAMAN_OBSERVATION_TIME_INVALID') from None
    if (junit.tag != 'testsuite' or junit.get('name') != 'com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest'
            or any(junit.get(key) != value for key, value in [('tests', '1'), ('failures', '0'), ('errors', '0'), ('skipped', '0')])
            or len(junit.findall('testcase')) != 1
            or not any('HAMAN-41306' in case.get('name', '') for case in junit.findall('testcase'))
            or any(junit.findall('.//' + tag) for tag in ('failure', 'error', 'skipped'))):
        raise ValueError('HAMAN_JUNIT_INCOMPLETE')
    runner = runpy.run_path(str(pathlib.Path(__file__).with_name('run-temporary-bbs-observation.py')))
    unit = {'__name__': 'report_validation'}
    # 서버에서 사용하는 동일 계약만 호출한다. main/download/외부 요청은 실행하지 않는다.
    source = runner['UNIT_CODE'].replace('cfg=json.loads(sys.argv[1])', 'cfg={}', 1)
    exec(compile(source, '<haman-report-contract>', 'exec'), unit)
    unit['validate_probe_scope'](dict(kind='BBS_OBSERVATION_PROBE', verificationMode='HAMAN_OBSERVATION', status='PASSED',
        productionDatabaseUsed=False, isPolicyQaPassed=False, isExpectationApproved=False, reports=[report]), 'HAMAN_OBSERVATION')
    return dict(kind='HAMAN_OBSERVATION_VALIDATION', status='PASSED', caseCode='HAMAN-41306',
        requestReservations=report['requestReservationsIncludingBodyUpperBound'], reservedBytes=report['reservedBytesIncludingBodyUpperBound'],
        quality=report['files'][0]['quality'], decisionStatus=report['decisionStatus'],
        isPolicyQaPassed=False, isExpectationApproved=False, productionDatabaseUsed=False)


if __name__ == '__main__':
    try:
        if len(sys.argv) != 2:
            raise ValueError('HAMAN_ARGUMENTS_INVALID')
        report_path = pathlib.Path('build/reports/attachment-bbs-official-observation/HAMAN-41306.json')
        junit_path = pathlib.Path('build/test-results/attachmentBbsOfficialFileObservation/TEST-com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest.xml')
        if report_path.stat().st_size > 65536 or junit_path.stat().st_size > 1048576:
            raise ValueError('HAMAN_REPORT_SIZE_LIMIT')
        print(json.dumps(validate(json.loads(report_path.read_text(encoding='utf-8')), ET.parse(junit_path).getroot(),
            int(sys.argv[1]), time.time_ns() // 1000000), separators=(',', ':')))
    except Exception:
        print('{"kind":"HAMAN_OBSERVATION_VALIDATION","status":"INCOMPLETE"}')
        raise SystemExit(1)
