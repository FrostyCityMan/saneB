"""Run only the reviewed fixed probe on the current saneb deployment's one instance."""
import base64
import json
import os
import pathlib
import re
import subprocess
import time

class InvocationNotReady(Exception):
    pass

def aws(*args):
    result = subprocess.run(['aws', *args, '--region', 'ap-northeast-2', '--output', 'json', '--no-cli-pager'],
                            capture_output=True, text=True, timeout=45)
    if result.returncode:
        if args[:2] == ('ssm', 'get-command-invocation') and '(InvocationDoesNotExist)' in result.stderr:
            raise InvocationNotReady()
        code = next((code for code in ('AccessDeniedException', 'AccessDenied', 'UnauthorizedOperation',
                     'DeploymentDoesNotExistException', 'InvalidInstanceId', 'ExpiredToken')
                     if '(' + code + ')' in result.stderr), 'AWS_CALL_FAILED')
        print(json.dumps({'kind': 'PREFLIGHT_AWS_ERROR', 'operation': '/'.join(args[:2]), 'code': code}))
        raise ValueError('AWS_CALL_FAILED')
    return json.loads(result.stdout)

def main():
    deployment = os.environ['DEPLOYMENT_ID']
    if not re.fullmatch(r'd-[A-Z0-9]+', deployment):
        raise ValueError('INVALID_DEPLOYMENT')
    data = aws('deploy', 'get-deployment', '--deployment-id', deployment)['deploymentInfo']
    if (data['applicationName'], data['deploymentGroupName'], data['status']) != ('saneb', 'saneb-dev', 'Succeeded'):
        raise ValueError('DEPLOYMENT_TARGET_MISMATCH')
    group = aws('deploy', 'get-deployment-group', '--application-name', 'saneb', '--deployment-group-name', 'saneb-dev')['deploymentGroupInfo']
    if group.get('lastSuccessfulDeployment', {}).get('deploymentId') != deployment:
        raise ValueError('DEPLOYMENT_IS_NOT_CURRENT')
    if group.get('lastAttemptedDeployment', {}).get('status') in ('Created', 'Queued', 'InProgress', 'Ready', 'Baking'):
        raise ValueError('DEPLOYMENT_IN_PROGRESS')
    instances = aws('deploy', 'list-deployment-instances', '--deployment-id', deployment)['instancesList']
    if len(instances) != 1 or not re.fullmatch(r'i-[a-f0-9]+', instances[0]):
        raise ValueError('INSTANCE_SCOPE_INVALID')
    probe = pathlib.Path('scripts/qa/deployment-preflight-readonly.py').read_bytes()
    encoded = base64.b64encode(probe).decode()
    command = "python3 -c \"import base64;exec(compile(base64.b64decode('" + encoded + "'),'<preflight>','exec'))\""
    sent = aws('ssm', 'send-command', '--instance-ids', instances[0], '--document-name', 'AWS-RunShellScript',
               '--comment', 'saneB approved read-only deployment preflight', '--parameters',
               json.dumps({'commands': [command], 'executionTimeout': ['90']}))
    command_id = sent['Command']['CommandId']
    print('PREFLIGHT_COMMAND_ID=' + command_id, flush=True)
    for attempt in range(24):
        time.sleep(5)
        try:
            value = aws('ssm', 'get-command-invocation', '--command-id', command_id, '--instance-id', instances[0])
        except InvocationNotReady:
            continue
        if value['Status'] in ('Pending', 'InProgress', 'Delayed'):
            continue
        lines = value.get('StandardOutputContent', '').splitlines()
        reports = [json.loads(line) for line in lines if line.startswith('{')]
        for report in reports:
            if report.get('kind') == 'DEPLOYMENT_PREFLIGHT':
                print(json.dumps(report))
        if (value['Status'] != 'Success' or len(reports) != 1
                or reports[0].get('kind') != 'DEPLOYMENT_PREFLIGHT'
                or reports[0].get('writes') != 0 or reports[0].get('transaction') != 'ROLLED_BACK'
                or reports[0].get('database', {}).get('readOnly') != 'on'):
            raise ValueError('REMOTE_PREFLIGHT_FAILED')
        return
    raise TimeoutError('PREFLIGHT_RESULT_PENDING_DO_NOT_RESUBMIT')

if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        allowed = {'AWS_CALL_FAILED', 'INVALID_DEPLOYMENT', 'DEPLOYMENT_TARGET_MISMATCH',
                   'DEPLOYMENT_IS_NOT_CURRENT', 'DEPLOYMENT_IN_PROGRESS', 'INSTANCE_SCOPE_INVALID',
                   'REMOTE_PREFLIGHT_FAILED', 'PREFLIGHT_RESULT_PENDING_DO_NOT_RESUBMIT'}
        print(json.dumps({'kind': 'PREFLIGHT_DISPATCH', 'status': 'FAILED', 'errorType': type(error).__name__,
                          'code': str(error) if str(error) in allowed else 'UNEXPECTED_ERROR'}))
        raise SystemExit(1)
