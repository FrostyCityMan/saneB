import importlib.util
import pathlib
import unittest
from unittest.mock import MagicMock, patch

spec = importlib.util.spec_from_file_location('probe', pathlib.Path(__file__).with_name('probe-cheorwon-transport.py'))
probe = importlib.util.module_from_spec(spec)
spec.loader.exec_module(probe)


class CheorwonTransportTest(unittest.TestCase):
    def test_counts_structure_without_retaining_text_or_links(self):
        parser = probe.StructureCounter()
        parser.feed('<div class="bbs p-wrap bbs__view"><table class="block p-table">'
                    '<span class="p-table__subject_text">private title</span><td title="내용">private body</td>'
                    '<ul class="p-attach"><a class="p-attach__link" href="private-url">private file</a></ul></table></div>')
        self.assertEqual(dict.fromkeys(parser.counts, 1), parser.counts)
        self.assertNotIn('private', str(parser.counts))

    def test_unrelated_links_and_partial_classes_do_not_count(self):
        parser = probe.StructureCounter()
        parser.feed('<a href="/file.pdf">file</a><table class="p-table"><td>body</td></table>')
        self.assertEqual(dict.fromkeys(parser.counts, 0), parser.counts)

    def test_any_private_dns_answer_rejects_before_tcp(self):
        with patch.object(probe.socket, 'getaddrinfo', return_value=[(0, 0, 0, '', ('127.0.0.1', 443))]), \
                patch.object(probe.socket, 'create_connection') as connect:
            report = probe.probe()
        connect.assert_not_called()
        self.assertEqual('PUBLIC_ADDRESS_REQUIRED', report['errorCode'])
        self.assertEqual(0, report['httpRequests'])
        self.assertFalse(report['isBodyCollectionVerified'])

    def test_tcp_failure_is_not_html_failure_and_does_not_leak_exception(self):
        with patch.object(probe, 'select_public_ipv4', return_value='8.8.8.8'), \
                patch.object(probe.socket, 'create_connection', side_effect=TimeoutError('private detail')):
            report = probe.probe()
        self.assertEqual('TCP', report['phase'])
        self.assertEqual('TimeoutError', report['errorCode'])
        self.assertNotIn('private', str(report))
        self.assertEqual(1, report['requestReservations'])
        self.assertEqual(0, report['httpRequests'])
        self.assertFalse(report['isAttachmentCollectionVerified'])

    def select_http_report(self, status=200, payload=b'<html></html>', encoding='identity'):
        response = MagicMock()
        response.__enter__.return_value = response
        response.status = status
        response.getheader.return_value = encoding
        response.headers.get_content_type.return_value = 'text/html'
        response.headers.get_content_charset.return_value = 'utf-8'
        response.read.return_value = payload
        with patch.object(probe, 'select_public_ipv4', return_value='8.8.8.8'), \
                patch.object(probe.socket, 'create_connection') as connect, \
                patch.object(probe.ssl, 'create_default_context') as context, \
                patch.object(probe.http.client, 'HTTPResponse', return_value=response):
            result = probe.probe()
        connect.assert_called_once_with(('8.8.8.8', 443), timeout=3)
        self.assertEqual(probe.HOST, context.return_value.wrap_socket.call_args.kwargs['server_hostname'])
        return result, response

    def test_html_observation_does_not_claim_collection_success(self):
        result, response = self.select_http_report()
        self.assertEqual('OBSERVED_NOT_VALIDATED', result['status'])
        response.read.assert_called_once_with(probe.MAX_BYTES)
        self.assertEqual(1, result['httpRequests'])
        for key in ('originalSaved', 'isBodyCollectionVerified', 'isAttachmentCollectionVerified'):
            self.assertFalse(result[key])

    def test_tls_failure_returns_numeric_code_without_certificate_or_raw_error(self):
        failure = probe.ssl.SSLCertVerificationError(1, 'private certificate details')
        failure.verify_code = 10
        with patch.object(probe, 'select_public_ipv4', return_value='8.8.8.8'), \
                patch.object(probe.socket, 'create_connection'), \
                patch.object(probe.ssl, 'create_default_context') as context:
            context.return_value.wrap_socket.side_effect = failure
            result = probe.probe()
        self.assertEqual('TLS', result['phase'])
        self.assertEqual(10, result['tlsVerificationCode'])
        self.assertEqual(0, result['httpRequests'])
        self.assertNotIn('private', str(result))

    def test_redirect_is_not_followed_and_compression_is_not_expanded(self):
        for arguments, expected in [({'status': 302}, 'HTTP_STATUS_NOT_OK'),
                                    ({'encoding': 'gzip'}, 'CONTENT_ENCODING_NOT_SUPPORTED')]:
            result, response = self.select_http_report(**arguments)
            self.assertEqual(expected, result['errorCode'])
            response.read.assert_not_called()
            self.assertEqual(1, result['httpRequests'])

    def test_truncation_and_bad_encoding_are_not_success_or_raw_output(self):
        result, response = self.select_http_report(payload=b'a' * probe.MAX_BYTES)
        self.assertEqual('RESPONSE_LIMIT_REACHED', result['errorCode'])
        response.read.assert_called_once_with(probe.MAX_BYTES)
        result, _ = self.select_http_report(payload=b'private\xff')
        self.assertEqual('UnicodeDecodeError', result['errorCode'])
        self.assertNotIn('private', str(result))


if __name__ == '__main__':
    unittest.main()
