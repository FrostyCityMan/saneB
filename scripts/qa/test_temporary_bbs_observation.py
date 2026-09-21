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

    def test_local_install_location_is_supported(self):
        with patch('pathlib.Path.is_file', lambda p: p.as_posix() == '/usr/local/bin/aws'), patch('os.access', return_value=True):
            self.assertEqual('/usr/local/bin/aws', self.unit['aws_binary']())

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
