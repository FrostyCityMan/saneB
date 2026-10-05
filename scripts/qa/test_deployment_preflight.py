"""Offline tests: no AWS calls, production DB access or server writes."""
import contextlib
import importlib.util
import io
import json
import pathlib
import tempfile
import unittest
import zipfile
from unittest.mock import patch


def load(name, filename):
    spec = importlib.util.spec_from_file_location(name, pathlib.Path(__file__).with_name(filename))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


probe = load('probe', 'deployment-preflight-readonly.py')
dispatch = load('dispatch', 'dispatch-deployment-preflight.py')


class PreflightTests(unittest.TestCase):
    def test_database_readonly_and_ssl(self):
        env = probe.pg_environment({'DB_URL': 'jdbc:postgresql://example.invalid/db?sslmode=verify-full',
                                    'DB_USERNAME': 'fixture', 'DB_PASSWORD': 'fixture-only'})
        self.assertIn('default_transaction_read_only=on', env['PGOPTIONS'])
        self.assertEqual('verify-full', env['PGSSLMODE'])
        self.assertIn('BEGIN READ ONLY;', probe.SQL)
        self.assertTrue(probe.SQL.strip().endswith('ROLLBACK;'))

    def test_invalid_database_url(self):
        with self.assertRaises(ValueError):
            probe.pg_environment({'DB_URL': 'jdbc:mysql://example.invalid/db'})

    def test_running_schedule_probe_is_bounded_and_metadata_only(self):
        self.assertIn('LIMIT 10', probe.SQL)
        self.assertIn('LEFT JOIN announcement_source_collection_runs', probe.SQL)
        self.assertNotIn('error_message', probe.SQL)
        self.assertNotIn('source_url', probe.SQL)
        self.assertNotIn('SELECT *', probe.SQL)

    def test_deployment_path_requires_fixed_id(self):
        for value in (None, '../other', 'd-ABC/../other', 'd-abc'):
            self.assertEqual({'verified': False}, probe.deployment_jar_metadata(value))

    def test_jar_metadata(self):
        with tempfile.TemporaryDirectory() as directory:
            path = pathlib.Path(directory, 'fixture.jar')
            self.assertEqual({'exists': False}, probe.jar_metadata(path))
            with zipfile.ZipFile(path, 'w') as archive:
                archive.writestr('BOOT-INF/classes/db/migration/V86__fixture.sql', '-- fixture')
            result = probe.jar_metadata(path)
            self.assertEqual(86, result['migration'])
            self.assertEqual(64, len(result['sha256']))

    def run_dispatch(self, responses):
        with patch.dict(dispatch.os.environ, {'DEPLOYMENT_ID': 'd-TEST'}), \
             patch.object(dispatch, 'aws', side_effect=responses) as calls, \
             patch.object(dispatch.time, 'sleep'), contextlib.redirect_stdout(io.StringIO()):
            dispatch.main()
            return calls.call_args_list

    def prefix(self, instances=None):
        return [{'deploymentInfo': {'applicationName': 'saneb', 'deploymentGroupName': 'saneb-dev', 'status': 'Succeeded'}},
                {'deploymentGroupInfo': {'lastSuccessfulDeployment': {'deploymentId': 'd-TEST'}}},
                {'instancesList': instances if instances is not None else ['i-abc123']}]

    def test_single_send_eventual_consistency(self):
        report = {'kind': 'DEPLOYMENT_PREFLIGHT', 'writes': 0, 'transaction': 'ROLLED_BACK', 'database': {'readOnly': 'on'}}
        calls = self.run_dispatch(self.prefix() + [{'Command': {'CommandId': 'fixture'}},
                    dispatch.InvocationNotReady(), {'Status': 'InProgress'},
                    {'Status': 'Success', 'StandardOutputContent': json.dumps(report)}])
        self.assertEqual(1, sum(call.args[:2] == ('ssm', 'send-command') for call in calls))

    def test_multiple_instances_rejected(self):
        with self.assertRaisesRegex(ValueError, 'INSTANCE_SCOPE_INVALID'):
            self.run_dispatch(self.prefix(['i-abc123', 'i-abc456']))

    def test_failed_remote_rejected(self):
        with self.assertRaisesRegex(ValueError, 'REMOTE_PREFLIGHT_FAILED'):
            self.run_dispatch(self.prefix() + [{'Command': {'CommandId': 'fixture'}},
                              {'Status': 'Failed', 'StandardOutputContent': ''}])

    def test_stale_deployment_rejected(self):
        responses = self.prefix()
        responses[1]['deploymentGroupInfo']['lastSuccessfulDeployment']['deploymentId'] = 'd-OTHER'
        with self.assertRaisesRegex(ValueError, 'DEPLOYMENT_IS_NOT_CURRENT'):
            self.run_dispatch(responses)

    def test_running_deployment_rejected(self):
        responses = self.prefix()
        responses[1]['deploymentGroupInfo']['lastAttemptedDeployment'] = {'status': 'InProgress'}
        with self.assertRaisesRegex(ValueError, 'DEPLOYMENT_IN_PROGRESS'):
            self.run_dispatch(responses)


if __name__ == '__main__':
    unittest.main()
