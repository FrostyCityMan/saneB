"""임시 서버 관측 실행기의 네트워크 없는 회귀 검사."""
import contextlib
import io
import json
import pathlib
import runpy
import subprocess
import unittest
from unittest.mock import patch


class TemporaryBbsObservationTest(unittest.TestCase):
    def setUp(self):
        self.runner = runpy.run_path(str(pathlib.Path(__file__).with_name('run-temporary-bbs-observation.py')))
        self.unit = {'__name__': 'unit_test'}
        with patch('sys.argv', ['probe', '{}']):
            exec(compile(self.runner['UNIT_CODE'], '<temporary-unit>', 'exec'), self.unit)

    def test_system_aws_location_is_supported(self):
        with patch('pathlib.Path.is_file', lambda p: p.as_posix() == '/usr/bin/aws'), patch('os.access', return_value=True):
            self.assertEqual('/usr/bin/aws', self.unit['aws_binary']())

    def test_unknown_mode_is_rejected_before_resource_or_network_access(self):
        self.unit['cfg']={'verificationMode':'OTHER_PROVIDER'}
        with patch('pathlib.Path.read_text',side_effect=AssertionError('resource access')), patch('subprocess.run',side_effect=AssertionError('process start')):
            with self.assertRaisesRegex(ValueError,'^VERIFICATION_MODE_INVALID$'):
                self.unit['main']()
        self.assertFalse(self.unit['source_work_started'])

    def test_modes_cannot_select_arbitrary_groups_and_preserve_default(self):
        self.assertEqual([], self.unit['select_probe_arguments']('OBSERVATION'))
        self.assertEqual(['FIXED'], self.unit['select_probe_arguments']('FIXED'))
        self.assertEqual(['OKCHEON'], self.unit['select_probe_arguments']('OKCHEON'))
        self.assertEqual(['BOEUN'], self.unit['select_probe_arguments']('BOEUN'))
        for mode in ('', 'JECHEON', 'TAEBAEK_HWP', 'OKCHEON;echo unsafe'):
            with self.assertRaisesRegex(ValueError, '^VERIFICATION_MODE_INVALID$'):
                self.unit['select_probe_arguments'](mode)

    def test_okcheon_scope_is_exact_three_cases_with_unchanged_per_case_limits(self):
        scopes = self.runner['SCOPES']
        self.assertEqual(scopes, self.unit['SCOPES'])
        self.assertEqual(('OKCHEON-THREE-NOTICES', ['OKCHEON-193369', 'OKCHEON-193297', 'OKCHEON-193187'], 3*44, 3*80*1024*1024), scopes['OKCHEON'])
        self.assertEqual((44, 83886080), scopes['OBSERVATION'][2:])
        self.assertEqual((39, 81508141), scopes['FIXED'][2:])

    def test_manifest_cannot_reuse_taebaek_package_or_drop_negative_sample(self):
        self.unit['cfg'] = {'codeHash': 'a'*64}
        original = {'schemaVersion': 1, 'caseCode': 'TAEBAEK-184816', 'executionCodeHash': 'a'*64}
        self.unit['validate_manifest_scope'](original, 'OBSERVATION')
        self.unit['validate_manifest_scope'](original, 'FIXED')
        with self.assertRaisesRegex(ValueError, '^MANIFEST_SCOPE_INVALID$'):
            self.unit['validate_manifest_scope'](original, 'OKCHEON')
        manifest = dict(original, caseCode='OKCHEON-THREE-NOTICES', verificationMode='OKCHEON', caseCodes=self.runner['SCOPES']['OKCHEON'][1])
        self.unit['validate_manifest_scope'](manifest, 'OKCHEON')
        for mode in ('OBSERVATION', 'FIXED'):
            with self.assertRaisesRegex(ValueError, '^MANIFEST_SCOPE_INVALID$'):
                self.unit['validate_manifest_scope'](manifest, mode)
        for key, value in [('caseCodes', manifest['caseCodes'][:2]), ('caseCodes', ['OKCHEON-193369']*3),
                           ('verificationMode', 'FIXED'), ('executionCodeHash', 'b'*64), ('caseCode', 'OTHER')]:
            with self.assertRaisesRegex(ValueError, '^MANIFEST_SCOPE_INVALID$'):
                self.unit['validate_manifest_scope'](dict(manifest, **{key: value}), 'OKCHEON')

    def test_local_install_location_is_supported(self):
        with patch('pathlib.Path.is_file', lambda p: p.as_posix() == '/usr/local/bin/aws'), patch('os.access', return_value=True):
            self.assertEqual('/usr/local/bin/aws', self.unit['aws_binary']())

    def test_diagnostic_modes_keep_cases_but_reduce_remaining_approved_budget(self):
        self.unit['cfg']={'codeHash':'a'*64}
        for mode, original, used_requests, used_bytes in [('BOEUN_DIAGNOSTIC','BOEUN_OBSERVATION',12,7391673),
                                                       ('OKCHEON_DIAGNOSTIC','OKCHEON',8,5630612)]:
            scope=self.runner['SCOPES'][mode]
            self.assertEqual(self.runner['SCOPES'][original][:2],scope[:2])
            self.assertEqual((60,100663296),scope[2:])
            self.assertLessEqual(used_requests+scope[2],132)
            self.assertLessEqual(used_bytes+scope[3],251658240)
            self.assertEqual([mode],self.unit['select_probe_arguments'](mode))
            self.assertEqual(pathlib.Path('/tmp/package/qa'),self.unit['select_qa_distribution'](pathlib.Path('/tmp/package'),mode))
            manifest={'schemaVersion':1,'caseCode':scope[0],'caseCodes':scope[1],'verificationMode':mode,'executionCodeHash':'a'*64}
            self.unit['validate_manifest_scope'](manifest,mode)
            with self.assertRaisesRegex(ValueError,'^MANIFEST_SCOPE_INVALID$'):
                self.unit['validate_manifest_scope'](dict(manifest,verificationMode=original),mode)

    def test_boeun_segment_uses_latest_package_and_rejects_old_worker_report(self):
        mode='BOEUN_SEGMENT'
        scope=self.runner['SCOPES'][mode]
        self.assertEqual(self.runner['SCOPES']['BOEUN'][:2],scope[:2])
        self.assertEqual((42,75497472),scope[2:])
        self.assertLessEqual(24+scope[2],132)
        self.assertLessEqual(14783346+scope[3],251658240)
        with patch('zipfile.ZipFile',side_effect=AssertionError('operating installation accessed')):
            self.assertEqual(pathlib.Path('/tmp/package/qa'),self.unit['select_qa_distribution'](pathlib.Path('/tmp/package'),mode))
        self.assertEqual([mode],self.unit['select_probe_arguments'](mode))
        self.unit['cfg']={'codeHash':'a'*64}
        manifest={'schemaVersion':1,'caseCode':scope[0],'caseCodes':scope[1],'verificationMode':mode,'executionCodeHash':'a'*64}
        self.unit['validate_manifest_scope'](manifest,mode)
        with self.assertRaisesRegex(ValueError,'^MANIFEST_SCOPE_INVALID$'):
            self.unit['validate_manifest_scope'](dict(manifest,verificationMode='BOEUN'),mode)
        cases=[{'caseCode':code,'scope':'OFFICIAL_WORKER_EPHEMERAL_DB_API_V1','engineVersion':'attachment-segment-1.0.0',
                'segmentDatabaseApiVerified':True,'segmentReviewContextVerified':True,'manualSourceCheckRequired':True,
                'productionWriteCount':0,'isPolicyQaPassed':False,
                'maximumRequestReservations':14,'maximumReservedBytes':25165824,
                'requestReservationsIncludingBodyUpperBound':4,'reservedBytesIncludingBodyUpperBound':2400000} for code in scope[1]]
        report={'kind':'OFFICIAL_WORKER_PROBE','caseGroup':mode,'productionDatabaseUsed':False,
                'isPolicyQaPassed':False,'isAuthenticatedBrowserE2e':False,'status':'PASSED','cases':cases}
        self.unit['validate_probe_scope'](report,mode)
        for key,value in [('engineVersion','attachment-1.0.0'),('segmentDatabaseApiVerified',False),
                          ('segmentReviewContextVerified',False),('segmentReviewContextVerified','true'),
                          ('manualSourceCheckRequired',None),('manualSourceCheckRequired','true'),
                          ('maximumRequestReservations',44),('maximumReservedBytes',83886080),
                          ('requestReservationsIncludingBodyUpperBound',15),('requestReservationsIncludingBodyUpperBound',True),
                          ('reservedBytesIncludingBodyUpperBound',25165825)]:
            invalid=dict(report,cases=[dict(cases[0],**{key:value}),*cases[1:]])
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):
            self.unit['validate_probe_scope'](dict(report,caseGroup='BOEUN'),mode)

    def test_boeun_reads_only_pinned_installed_qa_and_checks_code_hash(self):
        import hashlib
        import zipfile
        from unittest.mock import MagicMock
        self.unit['cfg']={'installedJarSha256':'b'*64,'codeHash':hashlib.sha256(b'catalog').hexdigest()}
        expected=pathlib.Path('/opt/saneb/attachment-contract-qa-releases', 'b'*64)
        archive=MagicMock()
        archive.__enter__.return_value=archive
        archive.getinfo.return_value.file_size=7
        archive.read.return_value=b'catalog'
        with patch('pathlib.Path.glob',return_value=[expected/'lib/saneb-attachment-contract-qa-1.0.jar']), \
                patch('pathlib.Path.is_symlink',return_value=False),patch('pathlib.Path.is_file',return_value=True), \
                patch('zipfile.ZipFile',return_value=archive):
            self.assertEqual(expected,self.unit['select_qa_distribution'](pathlib.Path('/tmp/package'),'BOEUN'))
            self.unit['cfg']['codeHash']='c'*64
            with self.assertRaisesRegex(ValueError,'^INSTALLED_QA_CODE_CHANGED$'):
                self.unit['select_qa_distribution'](pathlib.Path('/tmp/package'),'BOEUN')
        self.assertEqual(pathlib.Path('/tmp/package/qa'),self.unit['select_qa_distribution'](pathlib.Path('/tmp/package'),'OBSERVATION'))

    def test_boeun_requires_exact_scope_and_worker_kind_without_production_authority(self):
        cases=['BOEUN-221499','BOEUN-221497','BOEUN-218812']
        self.assertEqual(('BOEUN-THREE-NOTICES',cases,132,251658240),self.runner['SCOPES']['BOEUN'])
        self.unit['cfg']={'codeHash':'a'*64}
        manifest={'schemaVersion':1,'caseCode':'BOEUN-THREE-NOTICES','caseCodes':cases,'verificationMode':'BOEUN','executionCodeHash':'a'*64}
        self.unit['validate_manifest_scope'](manifest,'BOEUN')
        for field,value in [('caseCodes',cases[:2]),('caseCodes',cases[::-1]),('verificationMode','OKCHEON'),('caseCode','TAEBAEK-184816')]:
            with self.assertRaisesRegex(ValueError,'^MANIFEST_SCOPE_INVALID$'):
                self.unit['validate_manifest_scope'](dict(manifest,**{field:value}),'BOEUN')
        report={'kind':'OFFICIAL_WORKER_PROBE','caseGroup':'BOEUN','productionDatabaseUsed':False,
                'isPolicyQaPassed':False,'isAuthenticatedBrowserE2e':False,'status':'PASSED','cases':[{'caseCode':c} for c in cases]}
        self.unit['validate_probe_scope'](report,'BOEUN')
        for field,value in [('kind','BBS_OBSERVATION_PROBE'),('caseGroup','OKCHEON'),('productionDatabaseUsed',True),
                            ('isPolicyQaPassed',True),('isAuthenticatedBrowserE2e',True),('cases',report['cases'][:2]),
                            ('cases',[report['cases'][0]]*3),('cases',[{'caseCode':'OTHER'}])]:
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):
                self.unit['validate_probe_scope'](dict(report,**{field:value}),'BOEUN')
        self.unit['validate_probe_scope'](dict(report,status='INCOMPLETE',cases=[]),'BOEUN')

    def test_boeun_observation_uses_new_temporary_package_not_installed_worker(self):
        mode='BOEUN_OBSERVATION'
        self.assertEqual(self.runner['SCOPES']['BOEUN'],self.runner['SCOPES'][mode])
        self.assertEqual([mode],self.unit['select_probe_arguments'](mode))
        with patch('zipfile.ZipFile',side_effect=AssertionError('operating installation accessed')):
            self.assertEqual(pathlib.Path('/tmp/package/qa'),self.unit['select_qa_distribution'](pathlib.Path('/tmp/package'),mode))
        self.unit['cfg']={'codeHash':'a'*64}
        manifest={'schemaVersion':1,'caseCode':'BOEUN-THREE-NOTICES','caseCodes':self.runner['SCOPES'][mode][1],
                  'verificationMode':mode,'executionCodeHash':'a'*64}
        self.unit['validate_manifest_scope'](manifest,mode)
        with self.assertRaisesRegex(ValueError,'^MANIFEST_SCOPE_INVALID$'):
            self.unit['validate_manifest_scope'](dict(manifest,verificationMode='BOEUN'),mode)
        report={'kind':'BBS_OBSERVATION_PROBE','verificationMode':mode}
        self.unit['validate_probe_scope'](report,mode)
        for changed in [dict(report,kind='OFFICIAL_WORKER_PROBE'),dict(report,verificationMode='BOEUN')]:
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](changed,mode)

    def test_untrusted_path_is_not_searched(self):
        with patch('pathlib.Path.is_file', return_value=False), patch('shutil.which', side_effect=AssertionError('untrusted PATH')):
            with self.assertRaisesRegex(ValueError, '^AWS_EXECUTABLE_MISSING$'):
                self.unit['aws_binary']()

    def test_nonexecutable_files_are_rejected(self):
        with patch('pathlib.Path.is_file', return_value=True), patch('os.access', return_value=False):
            with self.assertRaisesRegex(ValueError, '^AWS_EXECUTABLE_MISSING$'):
                self.unit['aws_binary']()

    def test_download_uses_v1_v2_common_arguments_and_no_user_credentials(self):
        self.unit['cfg'] = {'bucket': 'example-qa', 'key': 'qa/example/package.zip'}
        self.unit['aws_binary'] = lambda: '/usr/bin/aws'
        with patch('subprocess.run', return_value=subprocess.CompletedProcess([], 0, b'', b'')) as run:
            self.unit['download_package'](pathlib.Path('/tmp/package.zip'))
        args, kwargs = run.call_args
        self.assertEqual(['/usr/bin/aws', 's3api', 'get-object'], args[0][:3])
        self.assertNotIn('--no-cli-pager', args[0])
        self.assertEqual('', kwargs['env']['AWS_PAGER'])
        self.assertEqual('/nonexistent', kwargs['env']['HOME'])
        self.assertEqual(180, kwargs['timeout'])
        self.assertEqual({'PATH', 'LANG', 'HOME', 'AWS_MAX_ATTEMPTS', 'AWS_PAGER'}, set(kwargs['env']))

    def test_download_errors_are_fixed_codes_without_stderr_disclosure(self):
        self.unit['cfg'] = {'bucket': 'example-qa', 'key': 'qa/example/package.zip'}
        self.unit['aws_binary'] = lambda: '/usr/bin/aws'
        for stderr, code in [(b'AccessDenied private-data', 'PACKAGE_ACCESS_DENIED'),
                             (b'Unable to locate credentials', 'PACKAGE_CREDENTIALS_UNAVAILABLE'),
                             (b'SSL validation failed secret-url', 'PACKAGE_CERTIFICATE_FAILED'),
                             (b'private-unexpected-message', 'PACKAGE_DOWNLOAD_FAILED')]:
            with patch('subprocess.run', return_value=subprocess.CompletedProcess([], 1, b'', stderr)):
                with self.assertRaisesRegex(ValueError, '^' + code + '$'):
                    self.unit['download_package'](pathlib.Path('/tmp/package.zip'))

    def test_failure_records_stage_without_raw_paths_or_messages(self):
        def fail():
            raise FileNotFoundError('private-original-path-must-not-appear')
        self.unit['main'] = fail
        self.unit['phase'] = 'PACKAGE_DOWNLOAD'
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            self.assertEqual(1, self.unit['execute']())
        report = json.loads(output.getvalue())
        self.assertEqual('FileNotFoundError', report['errorType'])
        self.assertEqual('PACKAGE_DOWNLOAD', report['failureStage'])
        self.assertFalse(report['sourceWorkStarted'])
        self.assertNotIn('private-original', output.getvalue())

    def test_source_work_is_not_claimed_absent_after_launch(self):
        def fail():
            raise ValueError('PROBE_PROCESS_TIMEOUT')
        self.unit['main'] = fail
        self.unit['phase'] = 'SOURCE_PROBE'
        self.unit['source_work_started'] = True
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            self.assertEqual(1, self.unit['execute']())
        report = json.loads(output.getvalue())
        self.assertTrue(report['sourceWorkStarted'])
        self.assertEqual('PROBE_PROCESS_TIMEOUT', report['failureCode'])

    def test_success_requires_passed_status(self):
        for state, expected in [('PASSED', 0), ('INCOMPLETE', 1)]:
            self.unit['main'] = lambda: {'status': state}
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                self.assertEqual(expected, self.unit['execute']())
            self.assertTrue(json.loads(output.getvalue())['transportTemporaryFilesRemoved'])


if __name__ == '__main__':
    unittest.main()
