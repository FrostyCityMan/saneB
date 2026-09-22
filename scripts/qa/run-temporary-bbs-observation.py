import hashlib
import json
import pathlib
import re
import subprocess
import sys
import urllib.request

CONFIG = json.loads(sys.argv[1]) if __name__ == '__main__' else {}

# 코드의 지원 범위이며 실행 승인 자체가 아니다. 다른 기관/표본/예산은 받지 않는다.
SCOPES = {
    'OBSERVATION': ('TAEBAEK-184816', ['TAEBAEK-184816'], 44, 83886080),
    'FIXED': ('TAEBAEK-184816', ['TAEBAEK-184816'], 39, 81508141),
    'OKCHEON': ('OKCHEON-THREE-NOTICES', ['OKCHEON-193369', 'OKCHEON-193297', 'OKCHEON-193187'], 132, 251658240),
}

UNIT_CODE = 'SCOPES = ' + repr(SCOPES) + '\n' + r'''
import hashlib,json,os,pathlib,re,shutil,signal,stat,subprocess,sys,tempfile,time,zipfile
cfg=json.loads(sys.argv[1])
phase='RESOURCE_LIMITS'
source_work_started=False
def aws_binary():
    # 설치 위치는 다를 수 있지만 호출 대상은 관리되는 두 시스템 경로로 제한한다.
    for candidate in ('/usr/bin/aws','/usr/local/bin/aws'):
        if pathlib.Path(candidate).is_file() and os.access(candidate,os.X_OK):return candidate
    raise ValueError('AWS_EXECUTABLE_MISSING')
def download_package(archive):
    # AWS CLI v1에는 --no-cli-pager가 없다. v1/v2 공통 인자와 환경변수를 사용한다.
    env={'PATH':'/usr/local/bin:/usr/bin:/bin','LANG':'C.UTF-8','HOME':'/nonexistent','AWS_MAX_ATTEMPTS':'1','AWS_PAGER':''}
    download=subprocess.run([aws_binary(),'s3api','get-object','--region','ap-northeast-2','--bucket',cfg['bucket'],'--key',cfg['key'],str(archive)],env=env,capture_output=True,timeout=180)
    if download.returncode!=0:
        failure=download.stderr.decode('utf-8',errors='replace')
        if re.search('AccessDenied|Forbidden',failure):raise ValueError('PACKAGE_ACCESS_DENIED')
        if 'Unable to locate credentials' in failure:raise ValueError('PACKAGE_CREDENTIALS_UNAVAILABLE')
        if re.search('CERTIFICATE_VERIFY_FAILED|SSL validation failed',failure):raise ValueError('PACKAGE_CERTIFICATE_FAILED')
        raise ValueError('PACKAGE_DOWNLOAD_FAILED')
def digest(path):
    h=hashlib.sha256()
    with path.open('rb') as stream:
        for b in iter(lambda:stream.read(1048576),b''):h.update(b)
    return h.hexdigest()
def validate_manifest_scope(manifest,mode):
    if manifest.get('schemaVersion')!=1 or manifest.get('caseCode')!=SCOPES[mode][0] or manifest.get('executionCodeHash')!=cfg['codeHash']:raise ValueError('MANIFEST_SCOPE_INVALID')
    if mode=='OKCHEON' and (manifest.get('verificationMode')!=mode or manifest.get('caseCodes')!=SCOPES[mode][1]):raise ValueError('MANIFEST_SCOPE_INVALID')
def select_probe_arguments(mode):
    if mode not in SCOPES:raise ValueError('VERIFICATION_MODE_INVALID')
    return [] if mode=='OBSERVATION' else [mode]
def main():
    global phase,source_work_started
    mode=cfg.get('verificationMode','OBSERVATION')
    if mode not in SCOPES:raise ValueError('VERIFICATION_MODE_INVALID')
    group=pathlib.Path('/sys/fs/cgroup',pathlib.Path('/proc/self/cgroup').read_text().strip().split('0::',1)[1].lstrip('/'))
    quota,period=(group/'cpu.max').read_text().split()
    memory=int((group/'memory.max').read_text())
    fs=os.statvfs('/tmp');space=fs.f_blocks*fs.f_frsize
    if quota=='max' or int(quota)>int(period) or memory>805306368 or space>1073741824:raise ValueError('RESOURCE_BOUNDARY_INVALID')
    result={'kind':'TEMPORARY_BBS_QA','verificationMode':mode,'cpuQuota':quota,'cpuPeriod':period,'memoryMaxBytes':memory,'temporarySpaceMaxBytes':space,'productionDatabaseUsed':False}
    parent=pathlib.Path(tempfile.mkdtemp(prefix='saneb-transfer-',dir='/tmp'))
    try:
        archive=parent/'package.zip'
        phase='PACKAGE_DOWNLOAD'
        download_package(archive)
        phase='PACKAGE_VALIDATION'
        if archive.stat().st_size!=cfg['archiveBytes'] or archive.stat().st_size>209715200 or digest(archive)!=cfg['archiveSha256']:raise ValueError('PACKAGE_IDENTITY_INVALID')
        package=parent/'package';package.mkdir(mode=0o755)
        with zipfile.ZipFile(archive) as z:
            names=z.namelist()
            if len(names)>201 or len(set(names))!=len(names) or names.count('manifest.json')!=1:raise ValueError('PACKAGE_ENTRIES_INVALID')
            if z.getinfo('manifest.json').file_size>65536:raise ValueError('MANIFEST_SIZE_LIMIT')
            m=json.loads(z.read('manifest.json'))
            validate_manifest_scope(m,mode)
            entries=m['files']
            if len(entries)>200 or len({e['path'] for e in entries})!=len(entries) or set(names)!={e['path'] for e in entries}|{'manifest.json'}:raise ValueError('MANIFEST_ENTRIES_INVALID')
            if sum(e['bytes'] for e in entries)>209715200:raise ValueError('PACKAGE_EXPANSION_LIMIT')
            for e in entries:
                name=e['path'];parts=pathlib.PurePosixPath(name).parts;info=z.getinfo(name)
                if not re.fullmatch(r'[A-Za-z0-9_./+-]+',name) or name.startswith('/') or any(p in ('','..','.') for p in name.split('/')) or chr(92) in name:raise ValueError('PACKAGE_PATH_INVALID')
                if not (name.startswith('qa/') or name in ('probe.jar','run.sh')) or stat.S_ISLNK(info.external_attr>>16) or info.is_dir() or info.file_size!=e['bytes']:raise ValueError('PACKAGE_FILE_INVALID')
                target=package/name;target.parent.mkdir(parents=True,exist_ok=True)
                h=hashlib.sha256()
                with z.open(info) as src,target.open('xb') as dst:
                    for b in iter(lambda:src.read(1048576),b''):h.update(b);dst.write(b)
                if h.hexdigest()!=e['sha256']:raise ValueError('PACKAGE_FILE_CHANGED')
                target.chmod(0o644)
        archive.unlink()
        parent.chmod(0o755)
        package.chmod(0o755)
        for directory in package.rglob('*'):
            if directory.is_dir():directory.chmod(0o755)
        result['packageFileCount']=len(entries);result['archiveSha256']=cfg['archiveSha256'];result['executionCodeHash']=cfg['codeHash']
        phase='SOURCE_PROBE'
        command=['/usr/sbin/runuser','-u','ubuntu','--','/usr/bin/env','-i','PATH=/usr/bin:/bin','LANG=C.UTF-8','/bin/bash',str(package/'run.sh'),str(package/'qa'),str(package/'probe.jar'),cfg['probeHash'],cfg['codeHash']]
        command.extend(select_probe_arguments(mode))
        started=time.monotonic()
        source_work_started=True
        proc=subprocess.Popen(command,stdout=subprocess.PIPE,stderr=subprocess.PIPE,start_new_session=True)
        try:out,err=proc.communicate(timeout=650)
        except subprocess.TimeoutExpired:
            os.killpg(proc.pid,signal.SIGTERM)
            try:out,err=proc.communicate(timeout=5)
            except subprocess.TimeoutExpired:os.killpg(proc.pid,signal.SIGKILL);out,err=proc.communicate()
            raise ValueError('PROBE_PROCESS_TIMEOUT')
        result['elapsedSeconds']=round(time.monotonic()-started,3);result['exitCode']=proc.returncode
        if len(out)>22000:raise ValueError('PROBE_OUTPUT_LIMIT')
        report=None
        for line in out.decode('utf-8').splitlines():
            if line.startswith('{'):
                report=json.loads(line)
                if report.get('kind')!='BBS_OBSERVATION_PROBE' or report.get('verificationMode')!=mode:raise ValueError('PROBE_OUTPUT_INVALID')
        result['probe']=report
        result['probeCleanupSucceeded']=b'BBS_OBSERVATION_PROBE_CLEANUP=SUCCEEDED' in out
        result['status']='PASSED' if proc.returncode==0 and result['probeCleanupSucceeded'] and report and report.get('status')=='PASSED' else 'INCOMPLETE'
        return result
    finally:
        if parent.parent!=pathlib.Path('/tmp') or not parent.name.startswith('saneb-transfer-') or parent.is_symlink():raise ValueError('CLEANUP_SCOPE_INVALID')
        shutil.rmtree(parent)
def execute():
    try:
        result=main()
        result['transportTemporaryFilesRemoved']=True
        print(json.dumps(result,ensure_ascii=False,separators=(',',':')))
        return 0 if result['status']=='PASSED' else 1
    except Exception as failure:
        print(json.dumps({'kind':'TEMPORARY_BBS_QA','status':'INCOMPLETE','errorType':type(failure).__name__,'failureCode':str(failure) if isinstance(failure,ValueError) and re.fullmatch('[A-Z_]+',str(failure)) else 'RUNNER_FAILED','failureStage':phase,'sourceWorkStarted':source_work_started}))
        return 1
if __name__=='__main__':
    raise SystemExit(execute())
'''

def digest(path):
    h=hashlib.sha256()
    with path.open('rb') as stream:
        for part in iter(lambda:stream.read(1048576),b''):h.update(part)
    return h.hexdigest()

def health():
    try:
        with urllib.request.urlopen('http://127.0.0.1:8080/actuator/health',timeout=5) as r:
            return json.loads(r.read(4096)).get('status')=='UP'
    except Exception:return False

def main():
    if not re.fullmatch('[a-f0-9]{32}',CONFIG['executionId']):raise ValueError('EXECUTION_ID_INVALID')
    mode=CONFIG.get('verificationMode','OBSERVATION')
    if mode not in SCOPES:raise ValueError('VERIFICATION_MODE_INVALID')
    for key in ('archiveSha256','codeHash','probeHash','installedJarSha256'):
        if not re.fullmatch('[a-f0-9]{64}',CONFIG[key]):raise ValueError('IDENTITY_INVALID')
    if not re.fullmatch('[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]',CONFIG['bucket']) or CONFIG['key']!='qa/temporary-bbs/'+CONFIG['executionId']+'/package.zip':raise ValueError('OBJECT_SCOPE_INVALID')
    app=pathlib.Path('/home/ubuntu/app/app.jar')
    if digest(app)!=CONFIG['installedJarSha256'] or not health():raise ValueError('OPERATING_BASELINE_CHANGED')
    available=next(int(line.split()[1])//1024 for line in pathlib.Path('/proc/meminfo').read_text().splitlines() if line.startswith('MemAvailable:'))
    if available<900:raise ValueError('INSUFFICIENT_MEMORY_HEADROOM')
    unit='saneb-temp-bbs-qa-'+CONFIG['executionId']
    command=['systemd-run','--quiet','--wait','--collect','--pipe','--unit',unit,
             '--property=CPUQuota=100%','--property=MemoryMax=768M','--property=MemorySwapMax=0',
             '--property=TasksMax=128','--property=RuntimeMaxSec=1140','--property=TimeoutStopSec=10',
             '--property=KillMode=control-group','--property=ProtectSystem=strict','--property=ProtectHome=yes',
             '--property=PrivateMounts=yes','--property=TemporaryFileSystem=/tmp:rw,size=1G,mode=1777',
             '--property=NoNewPrivileges=yes','--property=UMask=0077',
             '/usr/bin/env','-i','PATH=/usr/local/bin:/usr/bin:/bin','LANG=C.UTF-8',
             '/usr/bin/python3','-c',UNIT_CODE,json.dumps(CONFIG,separators=(',',':'))]
    print(json.dumps({'kind':'TEMPORARY_QA_START','executionId':CONFIG['executionId'],'verificationMode':mode,'caseCodes':SCOPES[mode][1],'unit':unit,'maximumSeconds':1200,'maximumRequests':SCOPES[mode][2],'maximumSourceBytes':SCOPES[mode][3],'cpuQuotaPercent':100,'memoryMaxMiB':768,'temporarySpaceMaxMiB':1024,'operatingChangesRequested':False}),flush=True)
    try:
        p=subprocess.run(command,capture_output=True,timeout=1170)
        report=None
        if len(p.stdout)<23000:
            for line in p.stdout.decode('utf-8').splitlines():
                if line.startswith('{'):
                    candidate=json.loads(line)
                    if candidate.get('kind')=='TEMPORARY_BBS_QA':report=candidate
        unit_state=subprocess.run(['systemctl','is-active',unit],capture_output=True,text=True,timeout=5).stdout.strip()
        if unit_state in ('active','activating','deactivating'):raise ValueError('UNIT_STILL_ACTIVE')
        result={'kind':'TEMPORARY_QA_RESULT','executionId':CONFIG['executionId'],'exitCode':p.returncode,'unitInactive':True,'installedJarUnchanged':digest(app)==CONFIG['installedJarSha256'],'healthUp':health(),'report':report}
        print(json.dumps(result,ensure_ascii=False,separators=(',',':')),flush=True)
        return 0 if p.returncode==0 and report and report.get('status')=='PASSED' and result['installedJarUnchanged'] and result['healthUp'] else 1
    finally:
        # 자기 실행의 transient unit만 정리한다. 운영 서비스에는 명령을 보내지 않는다.
        subprocess.run(['systemctl','stop',unit],capture_output=True,timeout=15)
        subprocess.run(['systemctl','reset-failed',unit],capture_output=True,timeout=5)

if __name__=='__main__':
    try:
        raise SystemExit(main())
    except Exception as failure:
        print(json.dumps({'kind':'TEMPORARY_QA_FAILURE','status':'INCOMPLETE','errorType':type(failure).__name__,'failureCode':str(failure) if isinstance(failure,ValueError) and re.fullmatch('[A-Z_]+',str(failure)) else 'RUNNER_FAILED'}))
        raise SystemExit(1)
