"""Fixed-service read-only probe; never prints credentials, raw SQL errors or file contents."""
import hashlib
import json
import pathlib
import re
import subprocess
import urllib.parse
import zipfile

SQL = """
BEGIN READ ONLY;
SELECT json_build_object('readOnly',current_setting('transaction_read_only'),
 'migration',(SELECT max(version::integer) FROM flyway_schema_history WHERE success AND version ~ '^[0-9]+$'),
 'migrationFailures',(SELECT count(*) FROM flyway_schema_history WHERE NOT success),
 'activePolicies',(SELECT coalesce(json_agg(p),'[]'::json) FROM (
   SELECT id,version_no,mode_code,policy_hash,row_version FROM announcement_attachment_policies
   WHERE policy_status_code='ACTIVE' ORDER BY id LIMIT 10) p),
 'activeJobs',(SELECT count(*) FROM announcement_attachment_jobs WHERE job_status_code IN ('PENDING','RUNNING','RETRY_WAIT')),
 'waitingJobs',(SELECT count(*) FROM announcement_attachment_jobs WHERE job_status_code IN ('SCOPE_READY','PAUSED')),
 'activePolicyQa',(SELECT count(*) FROM announcement_attachment_policy_validation_runs WHERE run_status_code IN ('PENDING','RUNNING','CANCEL_REQUESTED')),
 'activeProviderQa',(SELECT count(*) FROM announcement_attachment_provider_qa_runs WHERE run_status_code IN ('BUILDING','READY','RUNNING','CANCEL_REQUESTED')),
 'queuedSchedules',(SELECT count(*) FROM announcement_source_schedule_executions WHERE execution_status_code='QUEUED'),
 'runningSchedules',(SELECT count(*) FROM announcement_source_schedule_executions WHERE execution_status_code='RUNNING'),
 'runningScheduleDetails',(SELECT coalesce(json_agg(details),'[]'::json) FROM (
   SELECT e.id AS execution_id,e.scheduled_for,e.updated_at,e.execution_status_code,
          r.public_code AS run_code,r.run_status_code,r.started_at,r.finished_at,
          r.total_count,r.collected_count,r.failed_count,
          (SELECT max(i.created_at) FROM announcement_source_collection_run_items i WHERE i.run_id=r.id) AS last_item_at
   FROM announcement_source_schedule_executions e
   LEFT JOIN announcement_source_collection_runs r ON r.id=e.run_id
   WHERE e.execution_status_code='RUNNING' ORDER BY e.scheduled_for LIMIT 10
 ) details));
ROLLBACK;
"""

def jar_metadata(path):
    if not path.is_file() or path.is_symlink():
        return {'exists': False}
    if path.stat().st_size > 256 * 1024 * 1024:
        raise ValueError('JAR_SIZE_LIMIT')
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1048576), b''):
            digest.update(chunk)
    with zipfile.ZipFile(path) as archive:
        versions = [int(m.group(1)) for name in archive.namelist()
                    if (m := re.search(r'/db/migration/V(\d+)__', name))]
    return {'exists': True, 'sha256': digest.hexdigest(), 'migration': max(versions, default=0)}

def pg_environment(values):
    url = values.get('DB_URL', '')
    if not url.startswith('jdbc:postgresql://'):
        raise ValueError('UNSUPPORTED_DATABASE_URL')
    parsed = urllib.parse.urlparse(url[5:])
    if not parsed.hostname or not parsed.path.strip('/'):
        raise ValueError('INVALID_DATABASE_URL')
    env = {'PATH': '/usr/bin:/bin', 'PGHOST': parsed.hostname,
           'PGPORT': str(parsed.port or 5432), 'PGDATABASE': urllib.parse.unquote(parsed.path.lstrip('/')),
           'PGUSER': values['DB_USERNAME'], 'PGPASSWORD': values['DB_PASSWORD'],
           'PGCONNECT_TIMEOUT': '5', 'PGAPPNAME': 'saneb-deployment-readonly',
           'PGOPTIONS': '-c default_transaction_read_only=on -c statement_timeout=8000 -c lock_timeout=2000'}
    mode = dict(urllib.parse.parse_qsl(parsed.query)).get('sslmode')
    if mode:
        env['PGSSLMODE'] = mode
    return env


def deployment_jar_metadata(deployment):
    if not isinstance(deployment, str) or not re.fullmatch(r'd-[A-Z0-9]+', deployment):
        return {'verified': False}
    paths = list(pathlib.Path('/opt/codedeploy-agent/deployment-root').glob(
        '*/' + deployment + '/deployment-archive/app.jar'))
    if len(paths) != 1:
        return {'verified': False}
    metadata = jar_metadata(paths[0])
    return {'verified': metadata.get('exists', False), **metadata}

def public_flags(values):
    # 값 원문과 다른 환경변수는 출력하지 않는다. UNSET은 유효 설정 false의 증거가 아니다.
    flags = ['SANEB_ANNOUNCEMENT_ATTACHMENT_WORKER_ENABLED',
             'SANEB_ANNOUNCEMENT_SOURCE_BATCH_ENABLED',
             'GOV24_PUBLIC_SERVICE_DETAIL_BODY_ENABLED']
    result = {}
    for key in flags:
        if key not in values:
            result[key] = 'UNSET'
        else:
            value = values[key].strip().lower()
            result[key] = value if value in ('true', 'false') else 'UNVERIFIED'
    return result


def main():
    result = subprocess.run(['systemctl', 'show', 'saneb.service', '--property=MainPID', '--value'],
                            capture_output=True, text=True, timeout=5, check=True)
    pid = result.stdout.strip()
    if not pid.isdigit() or int(pid) == 0:
        raise ValueError('SERVICE_NOT_RUNNING')
    values = {}
    for item in pathlib.Path('/proc', pid, 'environ').read_bytes().split(b'\0'):
        key, sep, value = item.partition(b'=')
        if sep:
            values[key.decode()] = value.decode()
    env = pg_environment(values)
    result = subprocess.run(['psql', '-X', '-q', '-A', '-t', '-v', 'ON_ERROR_STOP=1'],
                            input=SQL, capture_output=True, text=True, env=env, timeout=35)
    if result.returncode:
        raise ValueError('DATABASE_READ_FAILED')
    rows = [json.loads(line) for line in result.stdout.splitlines() if line.startswith('{')]
    if len(rows) != 1 or rows[0].get('readOnly') != 'on':
        raise ValueError('READ_ONLY_RESULT_INVALID')
    started = subprocess.run(['systemctl', 'show', 'saneb.service', '--property=ActiveEnterTimestamp', '--value'],
                             capture_output=True, text=True, timeout=5, check=True).stdout.strip()
    print(json.dumps({'kind': 'DEPLOYMENT_PREFLIGHT', 'database': rows[0],
        'serviceStartedAt': started,
        'deploymentBundle': deployment_jar_metadata(globals().get('DEPLOYMENT_ID')),
        'installed': jar_metadata(pathlib.Path('/home/ubuntu/app/app.jar')),
        'previous': jar_metadata(pathlib.Path('/home/ubuntu/app/app.jar.previous')),
        'databaseEndpointSha256': hashlib.sha256(env['PGHOST'].lower().encode()).hexdigest(),
        'flags': public_flags(values),
        'writes': 0, 'transaction': 'ROLLED_BACK'}))

if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        print(json.dumps({'kind': 'DEPLOYMENT_PREFLIGHT', 'status': 'FAILED', 'errorType': type(error).__name__}))
        raise SystemExit(1)
