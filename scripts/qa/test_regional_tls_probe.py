import importlib.util
import contextlib
import io
import json
import os
import pathlib
import signal
import socket
import ssl
import sys
import unittest
from unittest.mock import MagicMock, patch

spec = importlib.util.spec_from_file_location('regional_tls_probe', pathlib.Path(__file__).with_name('probe-regional-tls.py'))
probe = importlib.util.module_from_spec(spec)
spec.loader.exec_module(probe)


class RegionalTlsProbeTest(unittest.TestCase):
    def test_fixed_scope_rejects_arbitrary_targets(self):
        self.assertEqual(set(probe.TARGETS), {'SEONGNAM', 'SOKCHO', 'UISEONG_FILE'})
        with self.assertRaisesRegex(ValueError, 'FIXED_TARGET_REQUIRED'):
            probe.probe('localhost')

    @patch.object(probe.socket, 'create_connection')
    @patch.object(probe.socket, 'getaddrinfo')
    def test_private_or_mixed_addresses_never_connect(self, resolve, connect):
        for addresses in [[], ['127.0.0.1'], ['169.254.169.254'], ['10.0.0.1'], ['224.0.0.1'], ['240.0.0.1'], ['8.8.8.8', '192.168.1.1']]:
            resolve.return_value = [(socket.AF_INET, socket.SOCK_STREAM, 6, '', (a, 443)) for a in addresses]
            result = probe.probe('SEONGNAM')
            self.assertEqual(result['errorCode'], 'PUBLIC_ADDRESS_REQUIRED')
            self.assertEqual(result['httpRequests'], 0)
        connect.assert_not_called()

    @patch.object(probe.socket, 'getaddrinfo', side_effect=socket.gaierror('private diagnostic text'))
    def test_dns_failure_is_redacted(self, _resolve):
        result = probe.probe('SOKCHO')
        self.assertEqual(result['phase'], 'DNS')
        self.assertEqual(result['errorCode'], 'gaierror')
        self.assertNotIn('private diagnostic text', json.dumps(result))

    @patch.object(probe.ssl, 'create_default_context')
    @patch.object(probe.socket, 'create_connection')
    @patch.object(probe.socket, 'getaddrinfo')
    def test_connects_once_with_sni_and_never_sends_http(self, resolve, connect, context):
        resolve.return_value = [(socket.AF_INET, socket.SOCK_STREAM, 6, '', ('8.8.8.8', 443))]
        tcp = connect.return_value.__enter__.return_value
        secure = context.return_value.wrap_socket.return_value.__enter__.return_value
        secure.version.return_value = 'TLSv1.3'
        secure.cipher.return_value = ('TLS_AES_256_GCM_SHA384', 'TLSv1.3', 256)
        result = probe.probe('UISEONG_FILE')
        context.assert_called_once_with()
        context.return_value.wrap_socket.assert_called_once_with(tcp, server_hostname='eminwon.uiseong.go.kr')
        connect.assert_called_once_with(('8.8.8.8', 443), timeout=3)
        secure.sendall.assert_not_called()
        self.assertEqual(result['status'], 'TLS_CONNECTED_NOT_COLLECTION_VERIFIED')
        self.assertFalse(result['isAttachmentCollectionVerified'])
        self.assertFalse(result['originalSaved'])
        self.assertEqual(result['filesDownloaded'], 0)

    @patch.object(probe.ssl, 'create_default_context')
    @patch.object(probe.socket, 'create_connection')
    @patch.object(probe.socket, 'getaddrinfo')
    def test_certificate_failure_does_not_disable_validation(self, resolve, _connect, context):
        resolve.return_value = [(socket.AF_INET, socket.SOCK_STREAM, 6, '', ('8.8.8.8', 443))]
        failure = ssl.SSLCertVerificationError('do not print certificate')
        failure.verify_code = 20
        context.return_value.wrap_socket.side_effect = failure
        result = probe.probe('SEONGNAM')
        self.assertEqual(result['errorCode'], 'SSLCertVerificationError')
        self.assertEqual(result['tlsVerificationCode'], 20)
        self.assertNotIn('do not print', json.dumps(result))
        self.assertEqual(result['httpRequests'], 0)

    @patch.object(probe.socket, 'create_connection', side_effect=TimeoutError('private timeout'))
    @patch.object(probe.socket, 'getaddrinfo')
    def test_timeout_is_separate_without_retry(self, resolve, connect):
        resolve.return_value = [(socket.AF_INET, socket.SOCK_STREAM, 6, '', ('8.8.8.8', 443))]
        result = probe.probe('SOKCHO')
        self.assertEqual(result['phase'], 'TCP')
        self.assertEqual(result['errorCode'], 'TimeoutError')
        self.assertEqual(connect.call_count, 1)

    @patch.object(probe.ssl, 'create_default_context')
    @patch.object(probe.socket, 'create_connection')
    @patch.object(probe.socket, 'getaddrinfo')
    def test_tls_reason_is_allowlisted_without_exposing_exception(self, resolve, connect, context):
        resolve.return_value = [(socket.AF_INET, socket.SOCK_STREAM, 6, '', ('8.8.8.8', 443))]
        for reason in ['DH_KEY_TOO_SMALL', 'UNSAFE_LEGACY_RENEGOTIATION_DISABLED',
                       'CERTIFICATE_VERIFY_FAILED', 'PRIVATE_CANARY', None]:
            failure = ssl.SSLError('PRIVATE_CANARY')
            failure.reason = reason
            context.return_value.wrap_socket.side_effect = failure
            result = probe.probe('SEONGNAM')
            self.assertEqual(result['phase'], 'TLS')
            self.assertEqual(result['tlsReasonCode'], reason if reason in probe.TLS_REASONS
                             else 'TLS_REASON_UNCLASSIFIED')
            self.assertNotIn('PRIVATE_CANARY', json.dumps(result))
            self.assertEqual(result['tcpConnections'], 1)
            self.assertEqual(result['httpRequests'], 0)


    def test_main_pins_cpu_memory_deadline_and_reports_no_collection(self):
        resource = MagicMock()
        output = io.StringIO()
        with patch.dict(sys.modules, {'resource': resource}), \
                patch.object(os, 'sched_getaffinity', return_value={1, 3}, create=True), \
                patch.object(os, 'sched_setaffinity', create=True) as affinity, \
                patch.object(signal, 'SIGALRM', 14, create=True), \
                patch.object(signal, 'signal') as handler, \
                patch.object(signal, 'alarm', create=True) as alarm, \
                patch.object(probe, 'probe', side_effect=lambda code: {'caseCode': code, 'status': 'FAILED'}) as run, \
                contextlib.redirect_stdout(output):
            probe.main()
        resource.setrlimit.assert_any_call(resource.RLIMIT_AS, (134217728, 134217728))
        resource.setrlimit.assert_any_call(resource.RLIMIT_CPU, (5, 5))
        affinity.assert_called_once_with(0, {1})
        self.assertEqual([call.args[0] for call in run.call_args_list], list(probe.TARGETS))
        self.assertEqual([call.args[0] for call in alarm.call_args_list], [10, 0, 10, 0, 10, 0])
        with self.assertRaises(TimeoutError):
            handler.call_args.args[1](None, None)
        result = json.loads(output.getvalue())
        self.assertEqual(result['maximumConnections'], 3)
        self.assertRegex(result['observedAt'], r'^\d{4}-\d{2}-\d{2}T.*\+00:00$')
        self.assertEqual(result['opensslVersion'], ssl.OPENSSL_VERSION)
        self.assertEqual(result['maximumSeconds'], 30)
        self.assertEqual(result['httpRequests'], 0)
        self.assertEqual(result['productionWrites'], 0)
        self.assertFalse(result['isAttachmentCollectionVerified'])


if __name__ == '__main__':
    unittest.main()
