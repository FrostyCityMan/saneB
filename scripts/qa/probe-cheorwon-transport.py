"""철원 고정 상세 1건의 연결/HTML 구조 진단. 첨부나 원문은 저장하지 않는다."""
import http.client
import ipaddress
import json
import socket
import ssl
import time
from html.parser import HTMLParser

HOST = 'www.cwg.go.kr'
PATH = '/www/selectBbsNttView.do?key=1226&bbsNo=25&nttNo=288915'
MAX_BYTES = 1048576
SAFE_ERRORS = {'PUBLIC_ADDRESS_REQUIRED', 'HTTP_STATUS_NOT_OK', 'CONTENT_ENCODING_NOT_SUPPORTED',
               'CONTENT_TYPE_NOT_HTML', 'RESPONSE_LIMIT_REACHED', 'CHARSET_NOT_SUPPORTED'}


class StructureCounter(HTMLParser):
    def __init__(self):
        super().__init__()
        self.counts = dict(viewWrappers=0, tables=0, titles=0, bodyCells=0,
                           attachmentLists=0, attachmentLinks=0)

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        classes = set(attrs.get('class', '').split())
        predicates = {
            'viewWrappers': tag == 'div' and {'p-wrap', 'bbs', 'bbs__view'} <= classes,
            'tables': tag == 'table' and {'p-table', 'block'} <= classes,
            'titles': tag == 'span' and 'p-table__subject_text' in classes,
            'bodyCells': tag == 'td' and attrs.get('title') == '내용',
            'attachmentLists': tag == 'ul' and 'p-attach' in classes,
            'attachmentLinks': tag == 'a' and 'p-attach__link' in classes,
        }
        for key, present in predicates.items():
            self.counts[key] += int(present)


def select_public_ipv4():
    addresses = sorted({row[4][0] for row in socket.getaddrinfo(
        HOST, 443, socket.AF_INET, socket.SOCK_STREAM)})
    if not addresses or any(not ipaddress.ip_address(address).is_global for address in addresses):
        raise ValueError('PUBLIC_ADDRESS_REQUIRED')
    return addresses[0]


def probe():
    started = time.monotonic()
    report = dict(kind='CHEORWON_TRANSPORT_DIAGNOSTIC', caseCode='CHEORWON-288915',
                  status='FAILED', phase='DNS', requestReservations=1, httpRequests=0,
                  maximumRequests=1, maximumResponseBytes=MAX_BYTES, responseBytes=0,
                  filesDownloaded=0, productionWrites=0, originalSaved=False,
                  isBodyCollectionVerified=False, isAttachmentCollectionVerified=False)
    try:
        address = select_public_ipv4()
        report['phase'] = 'TCP'
        with socket.create_connection((address, 443), timeout=3) as tcp:
            report['phase'] = 'TLS'
            with ssl.create_default_context().wrap_socket(tcp, server_hostname=HOST) as secure:
                secure.settimeout(5)
                report['phase'] = 'HTTP'
                # 일부 전송 뒤 오류도 요청 한 회로 보수적으로 집계한다.
                report['httpRequests'] = 1
                secure.sendall(('GET ' + PATH + ' HTTP/1.1\r\nHost: ' + HOST
                                + '\r\nUser-Agent: saneB-isolated-QA/1.0\r\nAccept: text/html'
                                + '\r\nAccept-Encoding: identity\r\nConnection: close\r\n\r\n').encode('ascii'))
                with http.client.HTTPResponse(secure) as response:
                    response.begin()
                    report['httpStatus'] = response.status
                    if response.status != 200:
                        raise ValueError('HTTP_STATUS_NOT_OK')
                    if response.getheader('Content-Encoding', 'identity').lower() not in ('identity', ''):
                        raise ValueError('CONTENT_ENCODING_NOT_SUPPORTED')
                    if response.headers.get_content_type() != 'text/html':
                        raise ValueError('CONTENT_TYPE_NOT_HTML')
                    # 상한을 넘는 원문을 버퍼에 넣거나 압축을 풀지 않는다.
                    payload = response.read(MAX_BYTES)
                    report['responseBytes'] = len(payload)
                    if len(payload) == MAX_BYTES:
                        raise ValueError('RESPONSE_LIMIT_REACHED')
                    charset = response.headers.get_content_charset() or 'utf-8'
                    if charset.lower() not in ('utf-8', 'euc-kr', 'cp949'):
                        raise ValueError('CHARSET_NOT_SUPPORTED')
                    report['phase'] = 'HTML_STRUCTURE'
                    parser = StructureCounter()
                    parser.feed(payload.decode(charset, errors='strict'))
                    parser.close()
                    report['structureCounts'] = parser.counts
                    report['status'] = 'OBSERVED_NOT_VALIDATED'
                    # 개수만으로 DOM 소속/공고 본문/전체 첨부의 실제 성공을 판정하지 않는다.
    except Exception as failure:
        report['errorCode'] = str(failure) if type(failure) is ValueError and str(failure) in SAFE_ERRORS else type(failure).__name__
        if isinstance(failure, ssl.SSLCertVerificationError):
            # 인증서/예외 원문 대신 OpenSSL의 정수 검증 코드만 반환한다.
            code = getattr(failure, 'verify_code', None)
            if type(code) is int:
                report['tlsVerificationCode'] = code
    report['elapsedSeconds'] = round(time.monotonic() - started, 3)
    return report


if __name__ == '__main__':
    import os
    import resource
    import signal

    resource.setrlimit(resource.RLIMIT_AS, (128 * 1024 * 1024, 128 * 1024 * 1024))
    resource.setrlimit(resource.RLIMIT_CPU, (5, 5))
    os.sched_setaffinity(0, {min(os.sched_getaffinity(0))})

    def deadline(_signum, _frame):
        raise TimeoutError('DEADLINE')

    signal.signal(signal.SIGALRM, deadline)
    signal.alarm(15)
    result = probe()
    signal.alarm(0)
    print(json.dumps(result))
