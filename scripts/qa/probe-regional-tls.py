"""고정 3기관의 TLS 연결만 진단한다. HTTP·첨부·DB 요청과 파일 저장은 없다."""
import ipaddress
import json
import socket
import ssl
import time
from datetime import datetime, timezone

TARGETS = {
    'SEONGNAM': ('LGS-000089', 'eminwon.seongnam.go.kr'),
    'SOKCHO': ('LGS-000122', 'www.sokcho.go.kr'),
    'UISEONG_FILE': ('LGS-000211', 'eminwon.uiseong.go.kr'),
}

# OpenSSL의 제한된 원인 코드만 보관한다. 예외 메시지·인증서 원문은 반환하지 않는다.
TLS_REASONS = frozenset({
    'CERTIFICATE_VERIFY_FAILED', 'UNSAFE_LEGACY_RENEGOTIATION_DISABLED',
    'DH_KEY_TOO_SMALL', 'EE_KEY_TOO_SMALL', 'CA_MD_TOO_WEAK',
    'SSLV3_ALERT_HANDSHAKE_FAILURE', 'SSLV3_ALERT_ILLEGAL_PARAMETER',
    'TLSV1_ALERT_PROTOCOL_VERSION', 'TLSV1_ALERT_INTERNAL_ERROR',
    'TLSV1_ALERT_DECODE_ERROR', 'TLSV1_UNRECOGNIZED_NAME',
    'UNSUPPORTED_PROTOCOL', 'NO_PROTOCOLS_AVAILABLE', 'WRONG_VERSION_NUMBER',
    'UNEXPECTED_EOF_WHILE_READING',
})


def probe(case_code, *, tls12=False):
    if case_code not in TARGETS:
        raise ValueError('FIXED_TARGET_REQUIRED')
    source, host = TARGETS[case_code]
    started = time.monotonic()
    report = dict(caseCode=case_code, sourceCode=source, phase='DNS', status='FAILED',
                  tlsMode='TLS12' if tls12 else 'DEFAULT',
                  maximumConnections=1, tcpConnections=0, httpRequests=0,
                  filesDownloaded=0, productionWrites=0, originalSaved=False,
                  isAttachmentCollectionVerified=False)
    try:
        addresses = sorted({r[4][0] for r in socket.getaddrinfo(
            host, 443, socket.AF_INET, socket.SOCK_STREAM)})
        if not addresses or any(not ipaddress.ip_address(a).is_global
                                or ipaddress.ip_address(a).is_multicast
                                or ipaddress.ip_address(a).is_reserved for a in addresses):
            raise ValueError('PUBLIC_ADDRESS_REQUIRED')
        report['phase'] = 'TCP'
        report['tcpConnections'] = 1
        with socket.create_connection((addresses[0], 443), timeout=3) as tcp:
            report['phase'] = 'TLS'
            # 시스템 신뢰 저장소·hostname 검증을 유지하며 선택한 공개 주소에 SNI를 보낸다.
            context = ssl.create_default_context()
            if tls12:
                context.minimum_version = ssl.TLSVersion.TLSv1_2
                context.maximum_version = ssl.TLSVersion.TLSv1_2
            with context.wrap_socket(tcp, server_hostname=host) as secure:
                report['tlsVersion'] = secure.version()
                report['cipher'] = secure.cipher()[0]
                report['status'] = 'TLS_CONNECTED_NOT_COLLECTION_VERIFIED'
    except Exception as failure:
        report['errorCode'] = ('PUBLIC_ADDRESS_REQUIRED' if type(failure) is ValueError
                               and str(failure) == 'PUBLIC_ADDRESS_REQUIRED'
                               else type(failure).__name__)
        if isinstance(failure, ssl.SSLCertVerificationError):
            code = getattr(failure, 'verify_code', None)
            if type(code) is int:
                report['tlsVerificationCode'] = code
        if isinstance(failure, ssl.SSLError):
            reason = getattr(failure, 'reason', None)
            report['tlsReasonCode'] = (reason if reason in TLS_REASONS
                                       else 'TLS_REASON_UNCLASSIFIED')
        # 인증서 본문·예외 메시지·DNS IP는 출력하지 않는다.
    report['elapsedSeconds'] = round(time.monotonic() - started, 3)
    return report


def main():
    import os
    import resource
    import signal
    mode = os.environ.get('SANEB_REGIONAL_TLS_MODE', 'DEFAULT')
    if mode not in ('DEFAULT', 'TLS12'):
        raise ValueError('TLS_MODE_INVALID')
    resource.setrlimit(resource.RLIMIT_AS, (128 * 1024 * 1024, 128 * 1024 * 1024))
    resource.setrlimit(resource.RLIMIT_CPU, (5, 5))
    os.sched_setaffinity(0, {min(os.sched_getaffinity(0))})

    def deadline(_signum, _frame):
        raise TimeoutError('DEADLINE')

    signal.signal(signal.SIGALRM, deadline)
    reports = []
    for case_code in TARGETS:
        signal.alarm(10)
        reports.append(probe(case_code, tls12=mode == 'TLS12'))
        signal.alarm(0)
    print(json.dumps(dict(kind='REGIONAL_TLS_ONLY_DIAGNOSTIC', reports=reports,
                         tlsMode=mode,
                         observedAt=datetime.now(timezone.utc).isoformat(),
                         opensslVersion=ssl.OPENSSL_VERSION,
                         maximumConnections=3, maximumSeconds=30, maximumMemoryMiB=128,
                         httpRequests=0, filesDownloaded=0, productionWrites=0,
                         originalSaved=False, isAttachmentCollectionVerified=False)))


if __name__ == '__main__':
    main()
