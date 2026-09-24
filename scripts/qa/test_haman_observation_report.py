"""실사이트 요청 없이 보고서 시각·전체 파일·JUnit 및 관측/승인을 분리한다."""
import copy
import pathlib
import runpy
import unittest
import xml.etree.ElementTree as ET


class HamanObservationReportTest(unittest.TestCase):
    def setUp(self):
        self.validate = runpy.run_path(str(pathlib.Path(__file__).with_name('validate-haman-observation.py')))['validate']
        self.junit = ET.fromstring('<testsuite name="com.saneb.domain.announcementattachment.qa.AnnouncementAttachmentBbsOfficialObservationTest" tests="1" failures="0" errors="0" skipped="0"><testcase name="HAMAN-41306"/></testsuite>')
        self.start = 1790254800000
        self.row = dict(caseCode='HAMAN-41306', observedAt='2026-09-24T13:00:01Z', scope='OFFICIAL_THREE_STAGE_OBSERVATION_V1',
            profileCode='LOCAL_HAMAN_GET_V1', status='OBSERVED_NOT_VALIDATED', productionWriteCount=0,
            isPolicyQaPassed=False, isExpectationApproved=False, originalFilesRemoved=True, requiresFinalAdminVerification=True,
            bodyStageComplete=True, bodyStatus='AVAILABLE', discoveryStatus='FOUND', discoveryComplete=True,
            expectedListedFileCount=1, discoveredFileCount=1, maximumRequestReservations=6, maximumReservedBytes=24117248,
            requestReservationsIncludingBodyUpperBound=4, reservedBytesIncludingBodyUpperBound=2300000,
            decisionStatus='REVIEW_REQUIRED', isWholeTextAnalysisComplete=False,
            files=[dict(binaryHash='c8d37ea0142d19f7270c8231cde80028e8e40a3b1a73d02038dca01207a5bb97',
                status='OBSERVED', quality='PARTIAL_TEXT', format='HWP')])

    def test_partial_observation_is_not_approval(self):
        result = self.validate(self.row, self.junit, self.start, self.start + 10000)
        self.assertEqual('PASSED', result['status'])
        self.assertEqual('PARTIAL_TEXT', result['quality'])
        self.assertFalse(result['isPolicyQaPassed'])
        self.assertFalse(result['productionDatabaseUsed'])

    def test_stale_future_and_timezone_missing_are_rejected(self):
        for observed in ('2026-09-24T12:59:59Z', '2026-09-24T13:00:11Z', '2026-09-24T13:00:01', 'bad'):
            row = dict(self.row, observedAt=observed)
            with self.assertRaises(ValueError): self.validate(row, self.junit, self.start, self.start + 10000)

    def test_missing_skipped_failed_or_extra_junit_case_is_rejected(self):
        for key, value in [('tests', '0'), ('tests', '2'), ('skipped', '1'), ('failures', '1'), ('errors', '1'), ('name', 'other')]:
            junit = copy.deepcopy(self.junit); junit.set(key, value)
            with self.assertRaises(ValueError): self.validate(self.row, junit, self.start, self.start + 10000)
        junit = copy.deepcopy(self.junit); ET.SubElement(junit, 'testcase')
        with self.assertRaises(ValueError): self.validate(self.row, junit, self.start, self.start + 10000)
        for tag in ('failure', 'error', 'skipped'):
            junit = copy.deepcopy(self.junit); ET.SubElement(junit.find('testcase'), tag)
            with self.assertRaises(ValueError): self.validate(self.row, junit, self.start, self.start + 10000)
        junit = copy.deepcopy(self.junit); junit.find('testcase').set('name', '다른 공고')
        with self.assertRaises(ValueError): self.validate(self.row, junit, self.start, self.start + 10000)

    def test_wrong_file_scope_budget_and_partial_promotion_are_rejected(self):
        for field, value in [('caseCode', 'HAMAN-43065'), ('requestReservationsIncludingBodyUpperBound', 7),
                ('reservedBytesIncludingBodyUpperBound', 24117249), ('isWholeTextAnalysisComplete', True),
                ('isPolicyQaPassed', True), ('decisionStatus', 'ACCEPTED')]:
            row = dict(self.row); row[field] = value
            with self.assertRaises(ValueError): self.validate(row, self.junit, self.start, self.start + 10000)
        row = copy.deepcopy(self.row); row['files'][0]['binaryHash'] = 'a' * 64
        with self.assertRaises(ValueError): self.validate(row, self.junit, self.start, self.start + 10000)
