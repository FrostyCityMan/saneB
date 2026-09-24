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
    'BOEUN': ('BOEUN-THREE-NOTICES', ['BOEUN-221499', 'BOEUN-221497', 'BOEUN-218812'], 132, 251658240),
    'BOEUN_OBSERVATION': ('BOEUN-THREE-NOTICES', ['BOEUN-221499', 'BOEUN-221497', 'BOEUN-218812'], 132, 251658240),
    # 9/24 선행 관측 사용량을 차감한 잔여보다 작은 재진단 상한. 기존 모드 한도를 바꾸지 않는다.
    'BOEUN_DIAGNOSTIC': ('BOEUN-THREE-NOTICES', ['BOEUN-221499', 'BOEUN-221497', 'BOEUN-218812'], 60, 100663296),
    'OKCHEON_DIAGNOSTIC': ('OKCHEON-THREE-NOTICES', ['OKCHEON-193369', 'OKCHEON-193297', 'OKCHEON-193187'], 60, 100663296),
    'BOEUN_SEGMENT': ('BOEUN-THREE-NOTICES', ['BOEUN-221499', 'BOEUN-221497', 'BOEUN-218812'], 15, 75497472),
    # 후보 대조가 아닌 명시1.0.3 저장 검증. 누적 잔여를 초과하면 별도 승인이 필요하다.
    'BOEUN_STRUCTURAL': ('BOEUN-THREE-NOTICES', ['BOEUN-221499', 'BOEUN-221497', 'BOEUN-218812'], 15, 75497472),
    'BOEUN_LONG_FORM': ('BOEUN-221497', ['BOEUN-221497'], 5, 25165824),
    'NAMGU_OBSERVATION': ('NAMGU-THREE-NOTICES', ['NAMGU-44466', 'NAMGU-44381', 'NAMGU-42871'], 15, 75497472),
    'NAMGU_STRUCTURE': ('NAMGU-44381', ['NAMGU-44381'], 5, 25165824),
    'DALSEONG_OBSERVATION': ('DALSEONG-THREE-NOTICES', ['DALSEONG-51022', 'DALSEONG-52145', 'DALSEONG-51075'], 18, 72351744),
    'DALSEONG_HEADER': ('DALSEONG-51022', ['DALSEONG-51022'], 6, 24117248),
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
    if mode in ('OKCHEON','BOEUN','BOEUN_OBSERVATION','BOEUN_DIAGNOSTIC','OKCHEON_DIAGNOSTIC','BOEUN_SEGMENT','BOEUN_STRUCTURAL','BOEUN_LONG_FORM','NAMGU_OBSERVATION','NAMGU_STRUCTURE','DALSEONG_OBSERVATION','DALSEONG_HEADER') and (manifest.get('verificationMode')!=mode or manifest.get('caseCodes')!=SCOPES[mode][1]):raise ValueError('MANIFEST_SCOPE_INVALID')
def select_probe_arguments(mode):
    if mode not in SCOPES:raise ValueError('VERIFICATION_MODE_INVALID')
    return [] if mode=='OBSERVATION' else [mode]

def select_qa_distribution(package,mode):
    if mode!='BOEUN':return package/'qa'
    # 운영 설치물을 복사/수정하지 않고 JAR 지문별 불변 QA를 그대로 읽는다.
    root=pathlib.Path('/opt/saneb/attachment-contract-qa-releases',cfg['installedJarSha256'])
    jars=list((root/'lib').glob('saneb-attachment-contract-qa-*.jar'))
    if root.is_symlink() or len(jars)!=1 or jars[0].is_symlink():raise ValueError('INSTALLED_QA_INVALID')
    with zipfile.ZipFile(jars[0]) as jar:
        info=jar.getinfo('attachment-qa-code/catalog.json')
        if info.file_size>2097152 or hashlib.sha256(jar.read(info)).hexdigest()!=cfg['codeHash']:raise ValueError('INSTALLED_QA_CODE_CHANGED')
    if not (root/'extractor/bin/attachment-extractor').is_file():raise ValueError('INSTALLED_EXTRACTOR_MISSING')
    return root

def validate_probe_scope(report,mode):
    if mode in ('BOEUN','BOEUN_SEGMENT','BOEUN_STRUCTURAL','BOEUN_LONG_FORM'):
        if (report.get('kind')!='OFFICIAL_WORKER_PROBE' or report.get('caseGroup')!=mode
                or report.get('productionDatabaseUsed') is not False or report.get('isPolicyQaPassed') is not False
                or report.get('isAuthenticatedBrowserE2e') is not False):raise ValueError('PROBE_OUTPUT_INVALID')
        cases=report.get('cases',[])
        if not isinstance(cases,list) or any(not isinstance(c,dict) for c in cases):raise ValueError('PROBE_OUTPUT_INVALID')
        codes=[c.get('caseCode') for c in cases]
        if len(codes)!=len(set(codes)) or any(c not in SCOPES[mode][1] for c in codes):raise ValueError('PROBE_OUTPUT_INVALID')
        if report.get('status')=='PASSED' and codes!=SCOPES[mode][1]:raise ValueError('PROBE_OUTPUT_INVALID')
        if mode in ('BOEUN_SEGMENT','BOEUN_STRUCTURAL','BOEUN_LONG_FORM') and report.get('status')=='PASSED':
            long_form=mode=='BOEUN_LONG_FORM'
            structural=mode=='BOEUN_STRUCTURAL' or long_form
            version='segment-role-1.0.4' if long_form else 'segment-role-1.0.3' if structural else 'segment-role-1.0.2'
            rules_hash='27dfa69f39bea3c47e1bf40b20bf01471f2432bc08ee143355e4e849089406fe' if long_form else '8b9fdd872f3eb9890146d6e360408204ff285f4b23977e07693834aceec66d43' if structural else '2f02f48368ce3f42557dd62094dec8e6b99d44e27d0f51f265a2fd737aabdd82'
            for case in cases:
                if (case.get('scope')!='OFFICIAL_WORKER_EPHEMERAL_DB_API_V1'
                        or case.get('engineVersion')!='attachment-segment-1.0.0'
                        or case.get('segmentRuleVersion')!=version
                        or case.get('segmentRulesHash')!=rules_hash
                        or case.get('segmentDatabaseApiVerified') is not True
                        or case.get('segmentReviewContextVerified') is not True
                        or type(case.get('manualSourceCheckRequired')) is not bool
                        or case.get('productionWriteCount')!=0 or case.get('isPolicyQaPassed') is not False
                        or case.get('maximumRequestReservations')!=5 or case.get('maximumReservedBytes')!=25165824):raise ValueError('PROBE_OUTPUT_INVALID')
                for key,lower,upper in [('requestReservationsIncludingBodyUpperBound',3,5),('reservedBytesIncludingBodyUpperBound',1,25165824)]:
                    value=case.get(key)
                    if type(value) is not int or not lower<=value<=upper:raise ValueError('PROBE_OUTPUT_INVALID')
                if structural:
                    expected={
                        'BOEUN-221499':('22f9d58ac8c6a8c76fe3508e58bf367358453b88684af5710437a5133e687827',6,3,1),
                        'BOEUN-221497':('3594adc138acae4211de819c3803295704410a8e8484a2070323042f9f3bda3a',4,2,1),
                        'BOEUN-218812':('f823ab78281f55cb16c634bde50bbc6fc84175c1058c90355c1845ea50aa3673',1,1,0)}[case['caseCode']]
                    if long_form:expected=('9ba9e2ea3391599cb34de6b3dd8eeb394ef3a35d23954f52e16a6ac2061d1d9f',4,1,1)
                    files=case.get('files')
                    if (case.get('decisionStatus')!='REVIEW_REQUIRED' or case.get('manualSourceCheckRequired') is not True
                            or not isinstance(files,list) or len(files)!=1 or not isinstance(files[0],dict)):raise ValueError('PROBE_OUTPUT_INVALID')
                    file=files[0]
                    if file.get('quality')!='COMPLETE_TEXT' or file.get('segmentAnalysisHash')!=expected[0] or 'structuralCandidate' in file:raise ValueError('PROBE_OUTPUT_INVALID')
                    for key,value in zip(('segmentCount','unknownSegmentCount','noticeSegmentCount'),expected[1:]):
                        if type(file.get(key)) is not int or file[key]!=value:raise ValueError('PROBE_OUTPUT_INVALID')
                    for key in ('longFormObservedHashMatched' if long_form else 'structuralObservedHashMatched','evaluationBoundApiVerified','otherVersionReadOnlyVerified',
                                'legacyDefaultReadOnlyVerified','segmentEvaluationInputBound','segmentApiProjectionMatched'):
                        if file.get(key) is not True:raise ValueError('PROBE_OUTPUT_INVALID')
                    if long_form and (file.get('format')!='HWPX' or 'longFormCandidate' in file
                            or file.get('binaryHash')!='6bf01402eaeedc655d89ef45cf4a3afb01dd3953e60685c7954a43a9faa9bf04'
                            or file.get('textHash')!='ff601753dc73037f6287d69b5fd261976b14f4e210377db81085bd5917fc2066'
                            or case.get('requiresFinalAdminVerification') is not True):raise ValueError('PROBE_OUTPUT_INVALID')
    elif report.get('kind')!='BBS_OBSERVATION_PROBE' or report.get('verificationMode')!=mode:
        raise ValueError('PROBE_OUTPUT_INVALID')
    if mode in ('NAMGU_OBSERVATION','NAMGU_STRUCTURE') and report.get('status')=='PASSED':
        rows=report.get('reports')
        if not isinstance(rows,list) or len(rows)!=len(SCOPES[mode][1]) or any(not isinstance(row,dict) for row in rows):raise ValueError('PROBE_OUTPUT_INVALID')
        if [row.get('caseCode') for row in rows]!=SCOPES[mode][1]:raise ValueError('PROBE_OUTPUT_INVALID')
        for row in rows:
            if (row.get('scope')!='OFFICIAL_THREE_STAGE_OBSERVATION_V1' or row.get('profileCode')!='LOCAL_BUSAN_NAMGU_GET_V1'
                    or row.get('isPolicyQaPassed') is not False or row.get('isExpectationApproved') is not False
                    or row.get('productionWriteCount')!=0 or row.get('originalFilesRemoved') is not True
                    or row.get('maximumRequestReservations')!=5 or row.get('maximumReservedBytes')!=25165824
                    or row.get('status')!='OBSERVED_NOT_VALIDATED'):raise ValueError('PROBE_OUTPUT_INVALID')
            for field,minimum,maximum in [('requestReservationsIncludingBodyUpperBound',3,5),('reservedBytesIncludingBodyUpperBound',1,25165824)]:
                value=row.get(field)
                if type(value) is not int or not minimum<=value<=maximum:raise ValueError('PROBE_OUTPUT_INVALID')
            if mode=='NAMGU_STRUCTURE':
                files=row.get('files')
                if (not isinstance(files,list) or len(files)!=1 or not isinstance(files[0],dict)
                        or files[0].get('quality')!='COMPLETE_TEXT' or not isinstance(files[0].get('structureSummary'),dict)
                        or files[0].get('binaryHash')!='69f7738308da99a68f528d2c08dae175e9880c0bb0764d6c5bcffd6e333548c8'
                        or files[0].get('textHash')!='7181d23cb9973622cf14e2a6edfbed42424b06ad13a91eb53ed55d158d77161a'):raise ValueError('PROBE_OUTPUT_INVALID')
    if mode in ('DALSEONG_OBSERVATION','DALSEONG_HEADER') and report.get('status')=='PASSED':
        rows=report.get('reports')
        if (report.get('productionDatabaseUsed') is not False or report.get('isPolicyQaPassed') is not False
                or report.get('isExpectationApproved') is not False or not isinstance(rows,list) or len(rows)!=len(SCOPES[mode][1])
                or any(not isinstance(row,dict) for row in rows)
                or [row.get('caseCode') for row in rows]!=SCOPES[mode][1]):raise ValueError('PROBE_OUTPUT_INVALID')
        expected=[['1dc0fd0deeb1d892bb125ec567c71bc3111fb9ecf861f52e7107c6877ec88fdb','6dd8d582a0c8d6cbe2f22376002d737eb15aab98a28ac0e8f612bdc1e1837fb4'],
                  ['c3488feba7f7b3addafb0e6037be47f1e34193e8137769b514ed2453bd27e98c'],['f400d97b469c0473a78d10a91ec0d9bc2956d0ecbba2df6546a8191564b729ad']]
        for index,row in enumerate(rows):
            count=len(expected[index]);files=row.get('files')
            if (row.get('scope')!='OFFICIAL_THREE_STAGE_OBSERVATION_V1' or row.get('profileCode')!='LOCAL_DAEGU_DALSEONG_GET_V1'
                    or row.get('isPolicyQaPassed') is not False or row.get('isExpectationApproved') is not False
                    or row.get('originalFilesRemoved') is not True or row.get('requiresFinalAdminVerification') is not True
                    or row.get('status')!='OBSERVED_NOT_VALIDATED' or row.get('bodyStageComplete') is not True
                    or row.get('bodyStatus')!='AVAILABLE' or row.get('discoveryStatus')!='FOUND' or row.get('discoveryComplete') is not True
                    or not isinstance(files,list) or len(files)!=count or any(not isinstance(file,dict) for file in files)):raise ValueError('PROBE_OUTPUT_INVALID')
            for field,minimum,maximum in [('productionWriteCount',0,0),('expectedListedFileCount',count,count),('discoveredFileCount',count,count),
                    ('maximumRequestReservations',6,6),('maximumReservedBytes',24117248,24117248),
                    ('requestReservationsIncludingBodyUpperBound',3+count,6),('reservedBytesIncludingBodyUpperBound',1,24117248)]:
                value=row.get(field)
                if type(value) is not int or not minimum<=value<=maximum:raise ValueError('PROBE_OUTPUT_INVALID')
            for file_index,file in enumerate(files):
                if (file.get('binaryHash')!=expected[index][file_index] or file.get('status')!='OBSERVED'
                        or file.get('format')!=('PDF' if index==0 and file_index==0 else 'HWP')
                        or file.get('quality') not in ('COMPLETE_TEXT','PARTIAL_TEXT','OCR_REQUIRED','ENCRYPTED','CORRUPT','UNSUPPORTED','LIMIT_EXCEEDED')):raise ValueError('PROBE_OUTPUT_INVALID')
            whole=all(file.get('quality')=='COMPLETE_TEXT' for file in files)
            if mode=='DALSEONG_HEADER':
                headers=files[1].get('hwpStructure',{}).get('controlHeaders')
                if not isinstance(headers,list) or not 1<=len(headers)<=128:raise ValueError('PROBE_OUTPUT_INVALID')
            if (row.get('isWholeTextAnalysisComplete') is not whole or row.get('decisionStatus') not in ('ACCEPTED','REVIEW_REQUIRED')
                    or (not whole and row.get('decisionStatus')!='REVIEW_REQUIRED')):raise ValueError('PROBE_OUTPUT_INVALID')

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
            if mode=='BOEUN' and {e['path'] for e in entries}!={'probe.jar','run.sh'}:raise ValueError('WORKER_PACKAGE_SCOPE_INVALID')
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
        qa_distribution=select_qa_distribution(package,mode)
        command=['/usr/sbin/runuser','-u','ubuntu','--','/usr/bin/env','-i','PATH=/usr/bin:/bin','LANG=C.UTF-8','/bin/bash',str(package/'run.sh'),str(qa_distribution),str(package/'probe.jar'),cfg['probeHash'],cfg['codeHash']]
        command.extend(select_probe_arguments(mode))
        started=time.monotonic()
        source_work_started=True
        proc=subprocess.Popen(command,stdout=subprocess.PIPE,stderr=subprocess.PIPE,start_new_session=True)
        try:out,err=proc.communicate(timeout=900 if mode in ('BOEUN','BOEUN_SEGMENT','BOEUN_STRUCTURAL','BOEUN_LONG_FORM') else 650)
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
                validate_probe_scope(report,mode)
        result['probe']=report
        cleanup_marker=b'OFFICIAL_WORKER_PROBE_CLEANUP=SUCCEEDED' if mode in ('BOEUN','BOEUN_SEGMENT','BOEUN_STRUCTURAL','BOEUN_LONG_FORM') else b'BBS_OBSERVATION_PROBE_CLEANUP=SUCCEEDED'
        result['probeCleanupSucceeded']=cleanup_marker in out
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
