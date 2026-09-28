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

    def gangbuk_report(self):
        locators=['abef5eff5d1f72128387e8bc15bc114a2a94bf2d22b1bd2ebd9cae04bc51bf4f','20d878c543d793289cbf7845a07cf4bd9c60c20618a6df1a08d3c3e1e3d70672',
            '00cbd4c75b4c32b3a21a206cdfb39dd5b50928b6501d13faf0bcefee7af868f3','894fb3c93a8a1eabfd630e31c1062875c3075a3895aa72e51f8411223394d40d']
        row=dict(caseCode='GANGBUK-179490',scope='OFFICIAL_THREE_STAGE_OBSERVATION_V1',profileCode='LOCAL_GANGBUK_LEGAL_GET_V1',
            profileHash='6aa8ef570fdaf1dcc94e6e66b85d326a197de1057b6564368e97dc87ae781158',status='OBSERVED_NOT_VALIDATED',
            titleStage='COMBINATION_MATCHED',titleInputSource='FIXED_OFFICIAL_SAMPLE',rulesSource='EPHEMERAL_DB_DRAFT_SEED',rulesHash='a'*64,
            bodyStatus='AVAILABLE',bodyHash='b'*64,bodyCharacterCount=100,bodyStageComplete=True,discoveryStatus='FOUND',discoveryComplete=True,
            originalFilesRemoved=True,requiresFinalAdminVerification=True,isPolicyQaPassed=False,isExpectationApproved=False,
            productionWriteCount=0,expectedListedFileCount=4,discoveredFileCount=4,maximumRequestReservations=20,maximumReservedBytes=33554432,
            requestReservationsIncludingBodyUpperBound=15,reservedBytesIncludingBodyUpperBound=3000000,isWholeTextAnalysisComplete=False,decisionStatus='REVIEW_REQUIRED',
            files=[dict(locatorHash=locators[i],format=fmt,formatHint=fmt,status='OBSERVED',downloadAllowed=True,extractorVersion='1.0.14',bytes=100,
                binaryHash='a'*64,quality='PARTIAL_TEXT') for i,fmt in enumerate(['HWPX','HWP','HWPX','HWPX'])])
        return dict(kind='BBS_OBSERVATION_PROBE',verificationMode='GANGBUK_OBSERVATION',status='PASSED',productionDatabaseUsed=False,
            isPolicyQaPassed=False,isExpectationApproved=False,found=1,passed=1,failed=0,skipped=0,aborted=0,failedContainers=0,reports=[row])

    def test_gangbuk_explicit_single_scope_preserves_all_four_locators_and_own_distribution(self):
        mode='GANGBUK_OBSERVATION';scope=self.runner['SCOPES'][mode]
        self.assertEqual(('GANGBUK-179490',['GANGBUK-179490'],20,33554432),scope)
        self.unit['cfg']={'codeHash':'a'*64}
        manifest=dict(schemaVersion=1,caseCode=scope[0],caseCodes=scope[1],verificationMode=mode,executionCodeHash='a'*64)
        self.unit['validate_manifest_scope'](manifest,mode)
        with self.assertRaisesRegex(ValueError,'^MANIFEST_SCOPE_INVALID$'):
            self.unit['validate_manifest_scope'](dict(manifest,caseCodes=['GANGBUK-184761']),mode)
        with patch('zipfile.ZipFile',side_effect=AssertionError('operating installation accessed')):
            self.assertEqual(pathlib.Path('/package/qa'),self.unit['select_qa_distribution'](pathlib.Path('/package'),mode))
        self.unit['validate_probe_scope'](self.gangbuk_report(),mode)

    def test_gangbuk_rejects_partial_false_success_missing_files_and_budget_type_changes(self):
        import copy
        valid=self.gangbuk_report();mode='GANGBUK_OBSERVATION'
        for key,value in [('isWholeTextAnalysisComplete',True),('decisionStatus','ACCEPTED'),('bodyStatus','FAILED'),
                ('expectedListedFileCount',3),('maximumRequestReservations',21),('requestReservationsIncludingBodyUpperBound',21),
                ('reservedBytesIncludingBodyUpperBound',33554433),('productionWriteCount',True),('isExpectationApproved',True),('originalFilesRemoved','true')]:
            invalid=copy.deepcopy(valid);invalid['reports'][0][key]=value
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        for key,value in [('locatorHash','a'*64),('format','PDF'),('binaryHash','bad'),('bytes',True),('quality','FAILED'),('downloadAllowed','true')]:
            invalid=copy.deepcopy(valid);invalid['reports'][0]['files'][0][key]=value
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        invalid=copy.deepcopy(valid);invalid['reports'][0]['files'].pop()
        with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        for file in valid['reports'][0]['files']:
            file.update(quality='COMPLETE_TEXT',characterCount=100,blockCount=1,segmentSummary={'segmentCount':1},segmentAnalysisHash='b'*64)
        valid['reports'][0]['isWholeTextAnalysisComplete']=True
        self.unit['validate_probe_scope'](valid,mode)
        del valid['reports'][0]['files'][0]['segmentSummary']
        with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](valid,mode)

    def junggu_report(self):
        hashes=[['4a544f3c98451eb7c002c626157c2b92468962c14756f7b38e3e218954b1b554'],
                ['ae74fb4881522239bcb91a6f64dc137e1af55a207050ecfdc7e5a01f2a7026d6','6a57302609860d5332ccf95650cd754999270d6e6308acae19f06bd74104326c'],[]]
        rows=[]
        for index,code in enumerate(self.runner['SCOPES']['JUNGGU_OBSERVATION'][1]):
            count=2 if index==1 else 1
            row=dict(caseCode=code,scope='OFFICIAL_THREE_STAGE_OBSERVATION_V1',profileCode='LOCAL_DAEGU_JUNGGU_GET_V1',
                profileHash='e648e332e85e22fd2a6818eaf48b1a5d7ae58d4cad50b9e9ee0da847d73541ef',
                rulesSource='EPHEMERAL_DB_DRAFT_SEED',rulesHash='a'*64,titleInputSource='FIXED_OFFICIAL_SAMPLE',
                productionWriteCount=0,isPolicyQaPassed=False,isExpectationApproved=False,originalFilesRemoved=True,
                expectedListedFileCount=count,maximumRequestReservations=6,maximumReservedBytes=24117248)
            if index==2:
                row.update(status='TITLE_NOT_ELIGIBLE_NOT_FETCHED',titleStage='COMBINATION_NOT_MATCHED',titleReason='TITLE_COMBINATION_NOT_MATCHED',
                    requiresFinalAdminVerification=False,isWholeTextAnalysisComplete=False,requestReservationsIncludingBodyUpperBound=0,reservedBytesIncludingBodyUpperBound=0,files=[])
            else:
                row.update(status='OBSERVED_NOT_VALIDATED',titleStage='COMBINATION_MATCHED',requiresFinalAdminVerification=True,
                    bodyStatus='AVAILABLE',bodyStageComplete=True,discoveryStatus='FOUND',discoveryComplete=True,discoveredFileCount=count,
                    requestReservationsIncludingBodyUpperBound=count+3,reservedBytesIncludingBodyUpperBound=3000000,
                    isWholeTextAnalysisComplete=True,decisionStatus='REVIEW_REQUIRED',
                    files=[dict(binaryHash=value,status='OBSERVED',quality='COMPLETE_TEXT',format='HWPX' if index==0 else 'HWP' if number==0 else 'PDF') for number,value in enumerate(hashes[index])])
            rows.append(row)
        return dict(kind='BBS_OBSERVATION_PROBE',verificationMode='JUNGGU_OBSERVATION',status='PASSED',productionDatabaseUsed=False,
            isPolicyQaPassed=False,isExpectationApproved=False,found=3,passed=3,failed=0,skipped=0,aborted=0,failedContainers=0,reports=rows)

    def test_junggu_scope_keeps_all_references_and_prior_budget_without_operating_installation(self):
        mode='JUNGGU_OBSERVATION';scope=self.runner['SCOPES'][mode]
        self.assertEqual(('JUNGGU-THREE-NOTICES',['JUNGGU-34196','JUNGGU-33626','JUNGGU-33315'],12,48234496),scope)
        self.assertLessEqual(13+scope[2],30);self.assertLessEqual(6908001+scope[3],78*1024*1024)
        self.unit['cfg']={'codeHash':'a'*64}
        manifest=dict(schemaVersion=1,caseCode=scope[0],caseCodes=scope[1],verificationMode=mode,executionCodeHash='a'*64)
        self.unit['validate_manifest_scope'](manifest,mode)
        with self.assertRaisesRegex(ValueError,'^MANIFEST_SCOPE_INVALID$'):
            self.unit['validate_manifest_scope'](dict(manifest,caseCodes=scope[1][:2]),mode)
        with patch('zipfile.ZipFile',side_effect=AssertionError('operating installation accessed')):
            self.assertEqual(pathlib.Path('/package/qa'),self.unit['select_qa_distribution'](pathlib.Path('/package'),mode))
        self.assertEqual([mode],self.unit['select_probe_arguments'](mode))
        self.unit['validate_probe_scope'](self.junggu_report(),mode)

    def test_junggu_negative_rejects_network_budget_files_or_downstream_fields(self):
        for key,value in [('requestReservationsIncludingBodyUpperBound',1),('reservedBytesIncludingBodyUpperBound',1),
                ('productionWriteCount',True),('bodyStatus',None),('bodyHash',None),('bodyStageComplete',False),('discoveryStatus','FOUND'),
                ('discoveredFileCount',0),('decisionStatus','ACCEPTED'),('files',[{}]),('isWholeTextAnalysisComplete',True),
                ('requiresFinalAdminVerification',True),('isExpectationApproved',True),('rulesHash','bad')]:
            report=self.junggu_report();report['reports'][2][key]=value
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](report,'JUNGGU_OBSERVATION')

    def test_single_junggu_pdf_keeps_both_files_numeric_diagnostics_and_remaining_budget(self):
        import copy
        mode='JUNGGU_PDF';scope=self.runner['SCOPES'][mode]
        self.assertEqual(('JUNGGU-33626',['JUNGGU-33626'],6,24117248),scope)
        self.assertLessEqual(22+scope[2],30);self.assertLessEqual(11611889+scope[3],81788928)
        self.unit['cfg']={'codeHash':'a'*64}
        manifest=dict(schemaVersion=1,caseCode=scope[0],caseCodes=scope[1],verificationMode=mode,executionCodeHash='a'*64)
        self.unit['validate_manifest_scope'](manifest,mode)
        for codes in ([],['JUNGGU-34196'],self.runner['SCOPES']['JUNGGU_OBSERVATION'][1]):
            with self.assertRaisesRegex(ValueError,'^MANIFEST_SCOPE_INVALID$'):self.unit['validate_manifest_scope'](dict(manifest,caseCodes=codes),mode)
        report=self.junggu_report();report.update(verificationMode=mode,found=1,passed=1,reports=[report['reports'][1]])
        row=report['reports'][0];row['isWholeTextAnalysisComplete']=False
        pdf=row['files'][1];pdf.update(quality='PARTIAL_TEXT',textHash='a'*64,characterCount=4241,blockCount=5,
            pdfStructure=dict(pageCount=5,reliablePageCount=0,externalObjectInvocationCount=1,inlineImageInvocationCount=0,blankPageCount=0,replacementCharacterCount=0))
        self.unit['validate_probe_scope'](report,mode)
        for key in pdf['pdfStructure']:
            for value in (-1,'1',True,40000001):
                invalid=copy.deepcopy(report);invalid['reports'][0]['files'][1]['pdfStructure'][key]=value
                with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        invalid=copy.deepcopy(report);invalid['reports'][0]['files'][1]['pdfStructure']['raw']='PRIVATE_CANARY'
        with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        invalid=copy.deepcopy(report);invalid['reports'][0]['files'].pop(0)
        with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        for key,value in [('caseCode','JUNGGU-34196'),('requestReservationsIncludingBodyUpperBound',7),('decisionStatus','ACCEPTED')]:
            invalid=copy.deepcopy(report);invalid['reports'][0][key]=value
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        for key,value in [('quality','COMPLETE_TEXT'),('textHash','bad'),('pdfStructure',{}),('characterCount',True)]:
            invalid=copy.deepcopy(report);invalid['reports'][0]['files'][1][key]=value
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)

    def test_junggu_whole_set_rejects_wrong_fingerprint_missing_file_and_false_success(self):
        for key,value in [('binaryHash','a'*64),('format','HWP'),('quality','FAILED'),('status','FAILED')]:
            report=self.junggu_report();report['reports'][1]['files'][1][key]=value
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](report,'JUNGGU_OBSERVATION')
        for key,value in [('found',2),('passed',True),('skipped',1),('productionDatabaseUsed',True)]:
            report=self.junggu_report();report[key]=value
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](report,'JUNGGU_OBSERVATION')
        report=self.junggu_report();report['reports'][1]['files'].pop()
        with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](report,'JUNGGU_OBSERVATION')
        report=self.junggu_report();report['reports'][0]['files'][0]['quality']='PARTIAL_TEXT';report['reports'][0]['isWholeTextAnalysisComplete']=False
        self.unit['validate_probe_scope'](report,'JUNGGU_OBSERVATION')
        report['reports'][0]['decisionStatus']='ACCEPTED'
        with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](report,'JUNGGU_OBSERVATION')

    def test_junggu_segment_requires_both_files_and_stored_partial_review(self):
        import copy
        mode='JUNGGU_SEGMENT';scope=self.runner['SCOPES'][mode]
        self.assertEqual(('JUNGGU-33626',['JUNGGU-33626'],5,25165824),scope)
        self.assertEqual(32,27+scope[2])
        self.unit['cfg']={'codeHash':'a'*64}
        manifest=dict(schemaVersion=1,caseCode=scope[0],caseCodes=scope[1],verificationMode=mode,executionCodeHash='a'*64)
        self.unit['validate_manifest_scope'](manifest,mode)
        with self.assertRaisesRegex(ValueError,'^MANIFEST_SCOPE_INVALID$'):
            self.unit['validate_manifest_scope'](dict(manifest,caseCodes=['JUNGGU-34196']),mode)
        self.assertEqual([mode],self.unit['select_probe_arguments'](mode))
        case=dict(caseCode='JUNGGU-33626',scope='OFFICIAL_WORKER_EPHEMERAL_DB_API_V1',status='WORKER_DB_API_OBSERVED_NOT_APPROVED',
            profileCode='LOCAL_DAEGU_JUNGGU_GET_V1',profileHash='e648e332e85e22fd2a6818eaf48b1a5d7ae58d4cad50b9e9ee0da847d73541ef',
            engineVersion='attachment-segment-1.0.0',segmentRuleVersion='segment-role-1.0.4',
            segmentRulesHash='27dfa69f39bea3c47e1bf40b20bf01471f2432bc08ee143355e4e849089406fe',extractorVersion='1.0.14',
            workerStatus='EVALUATED',bodyStatus='AVAILABLE',decisionStatus='REVIEW_REQUIRED',decisionReason='ATTACHMENT_INCOMPLETE',
            productionWriteCount=0,remainingResourceLeases=0,discoveredFileCount=2,processedFileCount=2,extractorCalls=2,
            maximumRequestReservations=5,maximumReservedBytes=25165824,requestReservationsIncludingBodyUpperBound=5,reservedBytesIncludingBodyUpperBound=2439945)
        for key in ('bodyStageComplete','discoveryComplete','segmentDatabaseApiVerified','segmentReviewContextVerified','manualSourceCheckRequired','requiresFinalAdminVerification','originalFilesRemoved'):case[key]=True
        for key in ('isWholeTextAnalysisComplete','isPolicyQaPassed','isExpectationApproved','isAuthenticatedBrowserE2e'):case[key]=False
        files=[]
        for pdf in (False,True):
            file=dict(format='PDF' if pdf else 'HWP',downloadStatus='SUCCEEDED',quality='PARTIAL_TEXT' if pdf else 'COMPLETE_TEXT',
                binaryHash='6a57302609860d5332ccf95650cd754999270d6e6308acae19f06bd74104326c' if pdf else 'ae74fb4881522239bcb91a6f64dc137e1af55a207050ecfdc7e5a01f2a7026d6',
                textHash='9f6ed99df2287fe3e4b1e5546fcd44a045eb6c7434aaa3a65566e8b0aa2938cf' if pdf else '76a66ee11c3a7e729f0777e19b7b17fef9de708ce893a199b381f603ca5a45bd',
                bytes=209769 if pdf else 127488,characterCount=4241 if pdf else 3120,blockCount=5 if pdf else 178,
                segmentAnalysisHash='a'*64,segmentCount=1 if pdf else 4,unknownSegmentCount=1,noticeSegmentCount=0)
            for key in ('segmentEvaluationInputBound','segmentApiProjectionMatched','legacyDefaultReadOnlyVerified','evaluationBoundApiVerified','otherVersionReadOnlyVerified','pinnedInputAndCoverageVerified'):file[key]=True
            if pdf:file.update(partialFullCoverageVerified=True,segmentReason='COMPLETE_TEXT_REQUIRED',pdfStructure=dict(pageCount=5,reliablePageCount=0,externalObjectInvocationCount=1,inlineImageInvocationCount=0,blankPageCount=0,replacementCharacterCount=0))
            files.append(file)
        case['files']=files
        report=dict(kind='OFFICIAL_WORKER_PROBE',caseGroup=mode,status='PASSED',productionDatabaseUsed=False,isPolicyQaPassed=False,isAuthenticatedBrowserE2e=False,
            found=1,passed=1,failed=0,skipped=0,aborted=0,failedContainers=0,cases=[case])
        self.unit['validate_probe_scope'](report,mode)
        valid=copy.deepcopy(report);valid['cases'][0]['files'].reverse();self.unit['validate_probe_scope'](valid,mode)
        for key,value in [('extractorVersion','1.0.13'),('decisionStatus','ACCEPTED'),('segmentRuleVersion','segment-role-1.0.0'),('profileHash','a'*64),
                ('requestReservationsIncludingBodyUpperBound',6),('isWholeTextAnalysisComplete',True),('segmentDatabaseApiVerified','true'),('productionWriteCount',True)]:
            invalid=copy.deepcopy(report);invalid['cases'][0][key]=value
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        for index in (0,1):
            for key,value in [('binaryHash','a'*64),('textHash','b'*64),('segmentCount',True),('bytes',0),('pinnedInputAndCoverageVerified',False),('longFormCandidate',{})]:
                invalid=copy.deepcopy(report);invalid['cases'][0]['files'][index][key]=value
                with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        for key,value in [('quality','COMPLETE_TEXT'),('partialFullCoverageVerified',False),('unknownSegmentCount',0),('pdfStructure',{}),('segmentReason','ROLE_TEXT_STRUCTURE_MATCHED')]:
            invalid=copy.deepcopy(report);invalid['cases'][0]['files'][1][key]=value
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        for key,value in [('passed',True),('skipped',1),('found',2)]:
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](dict(report,**{key:value}),mode)
        invalid=copy.deepcopy(report);invalid['cases'][0]['files'].pop()
        with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)

    def test_haman_segment_worker_keeps_partial_review_and_strict_stored_evidence(self):
        import copy
        mode='HAMAN_SEGMENT';scope=self.runner['SCOPES'][mode]
        self.assertEqual(('HAMAN-41306',['HAMAN-41306'],5,25165824),scope)
        self.assertLessEqual(19+scope[2],60);self.assertLessEqual(12906174+scope[3],100663296)
        self.unit['cfg']={'codeHash':'a'*64}
        manifest=dict(schemaVersion=1,caseCode=scope[0],caseCodes=scope[1],verificationMode=mode,executionCodeHash='a'*64)
        self.unit['validate_manifest_scope'](manifest,mode)
        for cases in ([],['HAMAN-41307'],['HAMAN-41306','HAMAN-41306']):
            with self.assertRaisesRegex(ValueError,'^MANIFEST_SCOPE_INVALID$'):
                self.unit['validate_manifest_scope'](dict(manifest,caseCodes=cases),mode)
        self.assertEqual([mode],self.unit['select_probe_arguments'](mode))
        with patch('zipfile.ZipFile',side_effect=AssertionError('operating installation accessed')):
            self.assertEqual(pathlib.Path('/package/qa'),self.unit['select_qa_distribution'](pathlib.Path('/package'),mode))
        file=dict(format='HWP',quality='PARTIAL_TEXT',segmentReason='COMPLETE_TEXT_REQUIRED',bytes=101888,characterCount=4644,blockCount=213,
            binaryHash='c8d37ea0142d19f7270c8231cde80028e8e40a3b1a73d02038dca01207a5bb97',textHash='ea24e32e9c049cf8a2a7d3ffae300ffdee864ac33939a78ef520cbfa334fb5f7',
            segmentAnalysisHash='b'*64,segmentCount=1,unknownSegmentCount=1,noticeSegmentCount=0,
            segmentEvaluationInputBound=True,segmentApiProjectionMatched=True,legacyDefaultReadOnlyVerified=True,
            evaluationBoundApiVerified=True,otherVersionReadOnlyVerified=True,partialFullCoverageVerified=True)
        case=dict(caseCode='HAMAN-41306',scope='OFFICIAL_WORKER_EPHEMERAL_DB_API_V1',status='WORKER_DB_API_OBSERVED_NOT_APPROVED',
            engineVersion='attachment-segment-1.0.0',segmentRuleVersion='segment-role-1.0.4',segmentRulesHash='27dfa69f39bea3c47e1bf40b20bf01471f2432bc08ee143355e4e849089406fe',
            extractorVersion='1.0.12',profileCode='LOCAL_HAMAN_GET_V1',workerStatus='EVALUATED',bodyStatus='AVAILABLE',decisionStatus='REVIEW_REQUIRED',
            bodyStageComplete=True,discoveryComplete=True,segmentDatabaseApiVerified=True,segmentReviewContextVerified=True,
            manualSourceCheckRequired=True,requiresFinalAdminVerification=True,originalFilesRemoved=True,
            isWholeTextAnalysisComplete=False,isPolicyQaPassed=False,isExpectationApproved=False,isAuthenticatedBrowserE2e=False,
            productionWriteCount=0,remainingResourceLeases=0,discoveredFileCount=1,processedFileCount=1,maximumRequestReservations=5,maximumReservedBytes=25165824,
            requestReservationsIncludingBodyUpperBound=4,reservedBytesIncludingBodyUpperBound=2204906,files=[file])
        report=dict(kind='OFFICIAL_WORKER_PROBE',caseGroup=mode,productionDatabaseUsed=False,isPolicyQaPassed=False,isAuthenticatedBrowserE2e=False,
            status='PASSED',found=1,passed=1,failed=0,skipped=0,aborted=0,failedContainers=0,cases=[case])
        self.unit['validate_probe_scope'](report,mode)
        for key,value in [('caseCode','HAMAN-41307'),('workerStatus','PENDING'),('decisionStatus','ACCEPTED'),('extractorVersion','1.0.11'),
                ('segmentRuleVersion','segment-role-1.0.3'),('manualSourceCheckRequired',False),('isWholeTextAnalysisComplete',True),
                ('isExpectationApproved',True),('segmentDatabaseApiVerified','true'),('remainingResourceLeases',True),
                ('requestReservationsIncludingBodyUpperBound',6),('reservedBytesIncludingBodyUpperBound',25165825)]:
            invalid=copy.deepcopy(report);invalid['cases'][0][key]=value
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        for key,value in [('binaryHash','a'*64),('textHash','a'*64),('quality','COMPLETE_TEXT'),('segmentReason','ROLE_TEXT_STRUCTURE_MATCHED'),
                ('bytes','101888'),('segmentCount',True),('unknownSegmentCount',0),('segmentAnalysisHash','bad'),('partialFullCoverageVerified','true'),('longFormObservedHashMatched',True)]:
            invalid=copy.deepcopy(report);invalid['cases'][0]['files'][0][key]=value
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        for key,value in [('passed',True),('found',2),('skipped',1),('failed',1),('aborted',1),('failedContainers',1)]:
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](dict(report,**{key:value}),mode)
        for key in ('partialFullCoverageVerified','segmentEvaluationInputBound','segmentApiProjectionMatched','evaluationBoundApiVerified','otherVersionReadOnlyVerified','legacyDefaultReadOnlyVerified'):
            invalid=copy.deepcopy(report);del invalid['cases'][0]['files'][0][key]
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)

    def test_haman_one_notice_uses_existing_budget_and_rejects_changed_file_or_false_completion(self):
        import copy
        mode='HAMAN_OBSERVATION';scope=self.runner['SCOPES'][mode]
        self.assertEqual(('HAMAN-41306',['HAMAN-41306'],6,24117248),scope)
        self.assertLessEqual(8+scope[2],60);self.assertLessEqual(6399210+scope[3],100663296)
        self.assertEqual([mode],self.unit['select_probe_arguments'](mode))
        self.unit['cfg']={'codeHash':'a'*64}
        manifest=dict(schemaVersion=1,caseCode=scope[0],caseCodes=scope[1],verificationMode=mode,executionCodeHash='a'*64)
        self.unit['validate_manifest_scope'](manifest,mode)
        for cases in ([],['HAMAN-43065'],['HAMAN-41306','HAMAN-41306']):
            with self.assertRaisesRegex(ValueError,'^MANIFEST_SCOPE_INVALID$'):
                self.unit['validate_manifest_scope'](dict(manifest,caseCodes=cases),mode)
        row=dict(caseCode='HAMAN-41306',scope='OFFICIAL_THREE_STAGE_OBSERVATION_V1',profileCode='LOCAL_HAMAN_GET_V1',
            status='OBSERVED_NOT_VALIDATED',productionWriteCount=0,isPolicyQaPassed=False,isExpectationApproved=False,originalFilesRemoved=True,
            bodyStageComplete=True,bodyStatus='AVAILABLE',discoveryStatus='FOUND',discoveryComplete=True,requiresFinalAdminVerification=True,
            expectedListedFileCount=1,discoveredFileCount=1,maximumRequestReservations=6,maximumReservedBytes=24117248,
            requestReservationsIncludingBodyUpperBound=4,reservedBytesIncludingBodyUpperBound=2300000,
            decisionStatus='REVIEW_REQUIRED',isWholeTextAnalysisComplete=False,
            files=[dict(binaryHash='c8d37ea0142d19f7270c8231cde80028e8e40a3b1a73d02038dca01207a5bb97',status='OBSERVED',quality='PARTIAL_TEXT',format='HWP')])
        report=dict(kind='BBS_OBSERVATION_PROBE',verificationMode=mode,status='PASSED',productionDatabaseUsed=False,isPolicyQaPassed=False,isExpectationApproved=False,reports=[row])
        self.unit['validate_probe_scope'](report,mode)
        for field,value in [('caseCode','HAMAN-43065'),('profileCode','LOCAL_DAEGU_DALSEONG_GET_V1'),('productionWriteCount',True),
                ('isPolicyQaPassed',True),('isExpectationApproved',True),('originalFilesRemoved',False),('requiresFinalAdminVerification',False),
                ('maximumRequestReservations',44),('requestReservationsIncludingBodyUpperBound',7),('reservedBytesIncludingBodyUpperBound',24117249),
                ('discoveredFileCount',2),('bodyStatus','UNAVAILABLE'),('decisionStatus','ACCEPTED'),('isWholeTextAnalysisComplete',True)]:
            invalid=copy.deepcopy(report);invalid['reports'][0][field]=value
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        for field,value in [('binaryHash','a'*64),('format','PDF'),('status','FAILED')]:
            invalid=copy.deepcopy(report);invalid['reports'][0]['files'][0][field]=value
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        invalid=copy.deepcopy(report);invalid['reports'].append(copy.deepcopy(row))
        with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)

    def test_dalseong_mode_pins_all_four_files_and_preserves_cumulative_budget(self):
        import copy
        mode='DALSEONG_OBSERVATION';scope=self.runner['SCOPES'][mode]
        self.assertEqual(('DALSEONG-THREE-NOTICES',['DALSEONG-51022','DALSEONG-52145','DALSEONG-51075'],18,72351744),scope)
        self.assertLessEqual(29+scope[2],60);self.assertLessEqual(25588962+scope[3],100663296)
        self.unit['cfg']={'codeHash':'a'*64}
        manifest=dict(schemaVersion=1,caseCode=scope[0],caseCodes=scope[1],verificationMode=mode,executionCodeHash='a'*64)
        self.unit['validate_manifest_scope'](manifest,mode)
        with self.assertRaisesRegex(ValueError,'^MANIFEST_SCOPE_INVALID$'):
            self.unit['validate_manifest_scope'](dict(manifest,caseCodes=scope[1][1:]),mode)
        hashes=[['1dc0fd0deeb1d892bb125ec567c71bc3111fb9ecf861f52e7107c6877ec88fdb','6dd8d582a0c8d6cbe2f22376002d737eb15aab98a28ac0e8f612bdc1e1837fb4'],
                ['c3488feba7f7b3addafb0e6037be47f1e34193e8137769b514ed2453bd27e98c'],['f400d97b469c0473a78d10a91ec0d9bc2956d0ecbba2df6546a8191564b729ad']]
        rows=[]
        for index,code in enumerate(scope[1]):
            count=len(hashes[index])
            rows.append(dict(caseCode=code,scope='OFFICIAL_THREE_STAGE_OBSERVATION_V1',profileCode='LOCAL_DAEGU_DALSEONG_GET_V1',
                status='OBSERVED_NOT_VALIDATED',productionWriteCount=0,isPolicyQaPassed=False,isExpectationApproved=False,originalFilesRemoved=True,
                bodyStageComplete=True,bodyStatus='AVAILABLE',discoveryStatus='FOUND',discoveryComplete=True,requiresFinalAdminVerification=True,
                expectedListedFileCount=count,discoveredFileCount=count,maximumRequestReservations=6,maximumReservedBytes=24117248,
                requestReservationsIncludingBodyUpperBound=count+3,reservedBytesIncludingBodyUpperBound=3000000,
                decisionStatus='REVIEW_REQUIRED',isWholeTextAnalysisComplete=True,
                files=[dict(binaryHash=value,status='OBSERVED',quality='COMPLETE_TEXT',format='PDF' if index==0 and j==0 else 'HWP') for j,value in enumerate(hashes[index])]))
        report=dict(kind='BBS_OBSERVATION_PROBE',verificationMode=mode,status='PASSED',productionDatabaseUsed=False,isPolicyQaPassed=False,isExpectationApproved=False,reports=rows)
        self.unit['validate_probe_scope'](report,mode)
        single=copy.deepcopy(report);single['verificationMode']='DALSEONG_HEADER';single['reports']=single['reports'][:1]
        self.assertEqual(('DALSEONG-51022',['DALSEONG-51022'],6,24117248),self.runner['SCOPES']['DALSEONG_HEADER'])
        self.assertLessEqual(42+6,60);self.assertLessEqual(33664851+24117248,100663296)
        with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](single,'DALSEONG_HEADER')
        single['reports'][0]['files'][1]['hwpStructure']={'controlHeaders':[{'kind':'TABLE','bytes':54,'shape':'EXTRA_ZERO','tailBytes':8,'count':2}]}
        self.unit['validate_probe_scope'](single,'DALSEONG_HEADER')
        single['reports'].append(copy.deepcopy(single['reports'][0]))
        with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](single,'DALSEONG_HEADER')
        for field,value in [('productionWriteCount',True),('maximumRequestReservations',44),('requiresFinalAdminVerification',False),('discoveredFileCount',1),('bodyStatus','UNAVAILABLE')]:
            invalid=copy.deepcopy(report);invalid['reports'][0][field]=value
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        for field,value in [('binaryHash','a'*64),('format','HWPX'),('status','FAILED')]:
            invalid=copy.deepcopy(report);invalid['reports'][0]['files'][0][field]=value
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        invalid=copy.deepcopy(report);invalid['reports'][0]['files'].pop()
        with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        rows[0]['files'][0]['quality']='PARTIAL_TEXT';rows[0]['isWholeTextAnalysisComplete']=False
        self.unit['validate_probe_scope'](report,mode)
        rows[0]['decisionStatus']='ACCEPTED'
        with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](report,mode)

    def test_long_form_mode_pins_one_file_and_stored_proof_within_cumulative_budget(self):
        import copy
        mode='BOEUN_LONG_FORM';scope=self.runner['SCOPES'][mode]
        self.assertEqual(('BOEUN-221497',['BOEUN-221497'],5,25165824),scope)
        self.assertLessEqual(156+scope[2],162);self.assertLessEqual(106474227+scope[3],251658240)
        self.assertEqual([mode],self.unit['select_probe_arguments'](mode))
        self.unit['cfg']={'codeHash':'a'*64}
        manifest=dict(schemaVersion=1,caseCode=scope[0],caseCodes=scope[1],verificationMode=mode,executionCodeHash='a'*64)
        self.unit['validate_manifest_scope'](manifest,mode)
        for cases in ([],['BOEUN-221499'],self.runner['SCOPES']['BOEUN'][1]):
            with self.assertRaisesRegex(ValueError,'^MANIFEST_SCOPE_INVALID$'):
                self.unit['validate_manifest_scope'](dict(manifest,caseCodes=cases),mode)
        flags=('longFormObservedHashMatched','evaluationBoundApiVerified','otherVersionReadOnlyVerified',
               'legacyDefaultReadOnlyVerified','segmentEvaluationInputBound','segmentApiProjectionMatched')
        file=dict(quality='COMPLETE_TEXT',format='HWPX',segmentCount=4,unknownSegmentCount=1,noticeSegmentCount=1,
                  binaryHash='6bf01402eaeedc655d89ef45cf4a3afb01dd3953e60685c7954a43a9faa9bf04',
                  textHash='ff601753dc73037f6287d69b5fd261976b14f4e210377db81085bd5917fc2066',
                  segmentAnalysisHash='9ba9e2ea3391599cb34de6b3dd8eeb394ef3a35d23954f52e16a6ac2061d1d9f',**dict.fromkeys(flags,True))
        case=dict(caseCode=scope[0],scope='OFFICIAL_WORKER_EPHEMERAL_DB_API_V1',engineVersion='attachment-segment-1.0.0',
                  segmentRuleVersion='segment-role-1.0.4',segmentRulesHash='27dfa69f39bea3c47e1bf40b20bf01471f2432bc08ee143355e4e849089406fe',
                  segmentDatabaseApiVerified=True,segmentReviewContextVerified=True,manualSourceCheckRequired=True,requiresFinalAdminVerification=True,
                  productionWriteCount=0,isPolicyQaPassed=False,decisionStatus='REVIEW_REQUIRED',files=[file],
                  maximumRequestReservations=5,maximumReservedBytes=25165824,requestReservationsIncludingBodyUpperBound=4,reservedBytesIncludingBodyUpperBound=2400000)
        report=dict(kind='OFFICIAL_WORKER_PROBE',caseGroup=mode,productionDatabaseUsed=False,
                    isPolicyQaPassed=False,isAuthenticatedBrowserE2e=False,status='PASSED',cases=[case])
        self.unit['validate_probe_scope'](report,mode)
        for key,value in [('segmentRuleVersion','segment-role-1.0.3'),('requiresFinalAdminVerification',False),
                          ('manualSourceCheckRequired',False),('decisionStatus','ACCEPTED'),('caseCode','BOEUN-221499')]:
            invalid=copy.deepcopy(report);invalid['cases'][0][key]=value
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        for key,value in [('binaryHash','a'*64),('textHash','a'*64),('segmentAnalysisHash','a'*64),('unknownSegmentCount',0),
                          ('segmentCount',True),('longFormCandidate',{}),('format','PDF'),*[(flag,'true') for flag in flags]]:
            invalid=copy.deepcopy(report);invalid['cases'][0]['files'][0][key]=value
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        with patch('zipfile.ZipFile',side_effect=AssertionError('operating installation accessed')):
            self.assertEqual(pathlib.Path('/package/qa'),self.unit['select_qa_distribution'](pathlib.Path('/package'),mode))

    def test_unknown_mode_is_rejected_before_resource_or_network_access(self):
        self.unit['cfg']={'verificationMode':'OTHER_PROVIDER'}
        with patch('pathlib.Path.read_text',side_effect=AssertionError('resource access')), patch('subprocess.run',side_effect=AssertionError('process start')):
            with self.assertRaisesRegex(ValueError,'^VERIFICATION_MODE_INVALID$'):
                self.unit['main']()
        self.assertFalse(self.unit['source_work_started'])

    def test_modes_cannot_select_arbitrary_groups_and_preserve_default(self):
        self.assertEqual([], self.unit['select_probe_arguments']('OBSERVATION'))
        self.assertEqual(['FIXED'], self.unit['select_probe_arguments']('FIXED'))
        self.assertEqual(['OKCHEON'], self.unit['select_probe_arguments']('OKCHEON'))
        self.assertEqual(['BOEUN'], self.unit['select_probe_arguments']('BOEUN'))
        for mode in ('', 'JECHEON', 'TAEBAEK_HWP', 'OKCHEON;echo unsafe'):
            with self.assertRaisesRegex(ValueError, '^VERIFICATION_MODE_INVALID$'):
                self.unit['select_probe_arguments'](mode)

    def test_okcheon_scope_is_exact_three_cases_with_unchanged_per_case_limits(self):
        scopes = self.runner['SCOPES']
        self.assertEqual(scopes, self.unit['SCOPES'])
        self.assertEqual(('OKCHEON-THREE-NOTICES', ['OKCHEON-193369', 'OKCHEON-193297', 'OKCHEON-193187'], 3*44, 3*80*1024*1024), scopes['OKCHEON'])
        self.assertEqual((44, 83886080), scopes['OBSERVATION'][2:])
        self.assertEqual((39, 81508141), scopes['FIXED'][2:])

    def test_namgu_scope_is_fixed_and_smaller_than_generic_three_case_limits(self):
        mode = 'NAMGU_OBSERVATION'
        scope = self.runner['SCOPES'][mode]
        self.assertEqual(('NAMGU-THREE-NOTICES', ['NAMGU-44466', 'NAMGU-44381', 'NAMGU-42871'], 15, 75497472), scope)
        self.assertEqual([mode], self.unit['select_probe_arguments'](mode))
        self.unit['cfg'] = {'codeHash': 'a'*64}
        manifest = {'schemaVersion': 1, 'caseCode': scope[0], 'caseCodes': scope[1],
                    'verificationMode': mode, 'executionCodeHash': 'a'*64}
        self.unit['validate_manifest_scope'](manifest, mode)
        with self.assertRaisesRegex(ValueError, '^MANIFEST_SCOPE_INVALID$'):
            self.unit['validate_manifest_scope'](dict(manifest, caseCodes=scope[1][:2]), mode)
        with self.assertRaisesRegex(ValueError, '^MANIFEST_SCOPE_INVALID$'):
            self.unit['validate_manifest_scope'](dict(manifest, caseCodes=['NAMGU-46034']), mode)
        with patch('pathlib.Path.is_file', side_effect=AssertionError('installed files read')):
            self.assertEqual(pathlib.Path('/package/qa'), self.unit['select_qa_distribution'](pathlib.Path('/package'), mode))

    def test_namgu_transport_rejects_wrong_scope_missing_cases_and_policy_claims(self):
        mode='NAMGU_OBSERVATION'
        rows=[dict(caseCode=case,scope='OFFICIAL_THREE_STAGE_OBSERVATION_V1',profileCode='LOCAL_BUSAN_NAMGU_GET_V1',
                   isPolicyQaPassed=False,isExpectationApproved=False,productionWriteCount=0,originalFilesRemoved=True,
                   maximumRequestReservations=5,maximumReservedBytes=25165824,status='OBSERVED_NOT_VALIDATED',
                   requestReservationsIncludingBodyUpperBound=4,reservedBytesIncludingBodyUpperBound=2200000)
              for case in self.runner['SCOPES'][mode][1]]
        report=dict(kind='BBS_OBSERVATION_PROBE',verificationMode=mode,status='PASSED',reports=rows)
        self.unit['validate_probe_scope'](report,mode)
        for field,value in [('caseCode','NAMGU-46034'),('isPolicyQaPassed',True),('isExpectationApproved',True),
                            ('originalFilesRemoved',False),('requestReservationsIncludingBodyUpperBound',6),
                            ('reservedBytesIncludingBodyUpperBound',25165825),('maximumRequestReservations',20),
                            ('maximumReservedBytes',33554432)]:
            changed=json.loads(json.dumps(report));changed['reports'][0][field]=value
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):
                self.unit['validate_probe_scope'](changed,mode)
        with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):
            self.unit['validate_probe_scope'](dict(report,reports=rows[:2]),mode)

    def test_manifest_cannot_reuse_taebaek_package_or_drop_negative_sample(self):
        self.unit['cfg'] = {'codeHash': 'a'*64}
        original = {'schemaVersion': 1, 'caseCode': 'TAEBAEK-184816', 'executionCodeHash': 'a'*64}
        self.unit['validate_manifest_scope'](original, 'OBSERVATION')
        self.unit['validate_manifest_scope'](original, 'FIXED')
        with self.assertRaisesRegex(ValueError, '^MANIFEST_SCOPE_INVALID$'):
            self.unit['validate_manifest_scope'](original, 'OKCHEON')
        manifest = dict(original, caseCode='OKCHEON-THREE-NOTICES', verificationMode='OKCHEON', caseCodes=self.runner['SCOPES']['OKCHEON'][1])
        self.unit['validate_manifest_scope'](manifest, 'OKCHEON')
        for mode in ('OBSERVATION', 'FIXED'):
            with self.assertRaisesRegex(ValueError, '^MANIFEST_SCOPE_INVALID$'):
                self.unit['validate_manifest_scope'](manifest, mode)
        for key, value in [('caseCodes', manifest['caseCodes'][:2]), ('caseCodes', ['OKCHEON-193369']*3),
                           ('verificationMode', 'FIXED'), ('executionCodeHash', 'b'*64), ('caseCode', 'OTHER')]:
            with self.assertRaisesRegex(ValueError, '^MANIFEST_SCOPE_INVALID$'):
                self.unit['validate_manifest_scope'](dict(manifest, **{key: value}), 'OKCHEON')

    def test_namgu_structure_is_one_pinned_file_within_remaining_budget(self):
        mode='NAMGU_STRUCTURE';scope=self.runner['SCOPES'][mode]
        self.assertEqual(('NAMGU-44381',['NAMGU-44381'],5,25165824),scope)
        self.assertLessEqual(54+scope[2],60);self.assertLessEqual(26409829+scope[3],100663296)
        self.unit['cfg']={'codeHash':'a'*64}
        manifest=dict(schemaVersion=1,caseCode=scope[0],caseCodes=scope[1],verificationMode=mode,executionCodeHash='a'*64)
        self.unit['validate_manifest_scope'](manifest,mode)
        for cases in ([],['NAMGU-44466'],self.runner['SCOPES']['NAMGU_OBSERVATION'][1]):
            with self.assertRaisesRegex(ValueError,'^MANIFEST_SCOPE_INVALID$'):
                self.unit['validate_manifest_scope'](dict(manifest,caseCodes=cases),mode)
        row=dict(caseCode='NAMGU-44381',scope='OFFICIAL_THREE_STAGE_OBSERVATION_V1',profileCode='LOCAL_BUSAN_NAMGU_GET_V1',
            isPolicyQaPassed=False,isExpectationApproved=False,productionWriteCount=0,originalFilesRemoved=True,
            maximumRequestReservations=5,maximumReservedBytes=25165824,status='OBSERVED_NOT_VALIDATED',
            requestReservationsIncludingBodyUpperBound=4,reservedBytesIncludingBodyUpperBound=2200000,
            files=[dict(quality='COMPLETE_TEXT',structureSummary={},binaryHash='69f7738308da99a68f528d2c08dae175e9880c0bb0764d6c5bcffd6e333548c8',
                        textHash='7181d23cb9973622cf14e2a6edfbed42424b06ad13a91eb53ed55d158d77161a')])
        report=dict(kind='BBS_OBSERVATION_PROBE',verificationMode=mode,status='PASSED',reports=[row])
        self.unit['validate_probe_scope'](report,mode)
        for field,value in [('quality','PARTIAL_TEXT'),('binaryHash','a'*64),('textHash','a'*64),('structureSummary',None)]:
            changed=json.loads(json.dumps(report));changed['reports'][0]['files'][0][field]=value
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](changed,mode)

    def test_local_install_location_is_supported(self):
        with patch('pathlib.Path.is_file', lambda p: p.as_posix() == '/usr/local/bin/aws'), patch('os.access', return_value=True):
            self.assertEqual('/usr/local/bin/aws', self.unit['aws_binary']())

    def test_diagnostic_modes_keep_cases_but_reduce_remaining_approved_budget(self):
        self.unit['cfg']={'codeHash':'a'*64}
        for mode, original, used_requests, used_bytes in [('BOEUN_DIAGNOSTIC','BOEUN_OBSERVATION',12,7391673),
                                                       ('OKCHEON_DIAGNOSTIC','OKCHEON',8,5630612)]:
            scope=self.runner['SCOPES'][mode]
            self.assertEqual(self.runner['SCOPES'][original][:2],scope[:2])
            self.assertEqual((60,100663296),scope[2:])
            self.assertLessEqual(used_requests+scope[2],132)
            self.assertLessEqual(used_bytes+scope[3],251658240)
            self.assertEqual([mode],self.unit['select_probe_arguments'](mode))
            self.assertEqual(pathlib.Path('/tmp/package/qa'),self.unit['select_qa_distribution'](pathlib.Path('/tmp/package'),mode))
            manifest={'schemaVersion':1,'caseCode':scope[0],'caseCodes':scope[1],'verificationMode':mode,'executionCodeHash':'a'*64}
            self.unit['validate_manifest_scope'](manifest,mode)
            with self.assertRaisesRegex(ValueError,'^MANIFEST_SCOPE_INVALID$'):
                self.unit['validate_manifest_scope'](dict(manifest,verificationMode=original),mode)

    def test_boeun_segment_uses_latest_package_and_rejects_old_worker_report(self):
        mode='BOEUN_SEGMENT'
        scope=self.runner['SCOPES'][mode]
        self.assertEqual(self.runner['SCOPES']['BOEUN'][:2],scope[:2])
        self.assertEqual((15,75497472),scope[2:])
        self.assertLessEqual(116+scope[2],132)
        self.assertLessEqual(80104904+scope[3],251658240)
        with patch('zipfile.ZipFile',side_effect=AssertionError('operating installation accessed')):
            self.assertEqual(pathlib.Path('/tmp/package/qa'),self.unit['select_qa_distribution'](pathlib.Path('/tmp/package'),mode))
        self.assertEqual([mode],self.unit['select_probe_arguments'](mode))
        self.unit['cfg']={'codeHash':'a'*64}
        manifest={'schemaVersion':1,'caseCode':scope[0],'caseCodes':scope[1],'verificationMode':mode,'executionCodeHash':'a'*64}
        self.unit['validate_manifest_scope'](manifest,mode)
        with self.assertRaisesRegex(ValueError,'^MANIFEST_SCOPE_INVALID$'):
            self.unit['validate_manifest_scope'](dict(manifest,verificationMode='BOEUN'),mode)
        cases=[{'caseCode':code,'scope':'OFFICIAL_WORKER_EPHEMERAL_DB_API_V1','engineVersion':'attachment-segment-1.0.0',
                'segmentRuleVersion':'segment-role-1.0.2','segmentRulesHash':'2f02f48368ce3f42557dd62094dec8e6b99d44e27d0f51f265a2fd737aabdd82',
                'segmentDatabaseApiVerified':True,'segmentReviewContextVerified':True,'manualSourceCheckRequired':True,
                'productionWriteCount':0,'isPolicyQaPassed':False,
                'maximumRequestReservations':5,'maximumReservedBytes':25165824,
                'requestReservationsIncludingBodyUpperBound':4,'reservedBytesIncludingBodyUpperBound':2400000} for code in scope[1]]
        report={'kind':'OFFICIAL_WORKER_PROBE','caseGroup':mode,'productionDatabaseUsed':False,
                'isPolicyQaPassed':False,'isAuthenticatedBrowserE2e':False,'status':'PASSED','cases':cases}
        self.unit['validate_probe_scope'](report,mode)
        for key,value in [('engineVersion','attachment-1.0.0'),('segmentDatabaseApiVerified',False),
                          ('segmentRuleVersion','segment-role-1.0.0'),('segmentRulesHash','a'*64),
                          ('segmentReviewContextVerified',False),('segmentReviewContextVerified','true'),
                          ('manualSourceCheckRequired',None),('manualSourceCheckRequired','true'),
                          ('maximumRequestReservations',44),('maximumReservedBytes',83886080),
                          ('requestReservationsIncludingBodyUpperBound',6),('requestReservationsIncludingBodyUpperBound',True),
                          ('reservedBytesIncludingBodyUpperBound',25165825)]:
            invalid=dict(report,cases=[dict(cases[0],**{key:value}),*cases[1:]])
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):
            self.unit['validate_probe_scope'](dict(report,caseGroup='BOEUN'),mode)

    def test_boeun_reads_only_pinned_installed_qa_and_checks_code_hash(self):
        import hashlib
        import zipfile
        from unittest.mock import MagicMock
        self.unit['cfg']={'installedJarSha256':'b'*64,'codeHash':hashlib.sha256(b'catalog').hexdigest()}
        expected=pathlib.Path('/opt/saneb/attachment-contract-qa-releases', 'b'*64)
        archive=MagicMock()
        archive.__enter__.return_value=archive
        archive.getinfo.return_value.file_size=7
        archive.read.return_value=b'catalog'
        with patch('pathlib.Path.glob',return_value=[expected/'lib/saneb-attachment-contract-qa-1.0.jar']), \
                patch('pathlib.Path.is_symlink',return_value=False),patch('pathlib.Path.is_file',return_value=True), \
                patch('zipfile.ZipFile',return_value=archive):
            self.assertEqual(expected,self.unit['select_qa_distribution'](pathlib.Path('/tmp/package'),'BOEUN'))
            self.unit['cfg']['codeHash']='c'*64
            with self.assertRaisesRegex(ValueError,'^INSTALLED_QA_CODE_CHANGED$'):
                self.unit['select_qa_distribution'](pathlib.Path('/tmp/package'),'BOEUN')
        self.assertEqual(pathlib.Path('/tmp/package/qa'),self.unit['select_qa_distribution'](pathlib.Path('/tmp/package'),'OBSERVATION'))

    def test_structural_storage_proof_is_separate_from_candidate_and_keeps_review_required(self):
        import copy
        mode='BOEUN_STRUCTURAL'
        scope=self.runner['SCOPES'][mode]
        self.assertEqual(self.runner['SCOPES']['BOEUN_SEGMENT'],scope)
        # 누적128회 이후15회 실행은 기존132회 승인 안에 들어가지 않는다.
        self.assertGreater(128+scope[2],132)
        self.assertLessEqual(87496577+scope[3],251658240)
        self.assertEqual([mode],self.unit['select_probe_arguments'](mode))
        with patch('zipfile.ZipFile',side_effect=AssertionError('operating installation accessed')):
            self.assertEqual(pathlib.Path('/tmp/package/qa'),self.unit['select_qa_distribution'](pathlib.Path('/tmp/package'),mode))
        self.unit['cfg']={'codeHash':'a'*64}
        manifest={'schemaVersion':1,'caseCode':scope[0],'caseCodes':scope[1],'verificationMode':mode,'executionCodeHash':'a'*64}
        self.unit['validate_manifest_scope'](manifest,mode)
        with self.assertRaisesRegex(ValueError,'^MANIFEST_SCOPE_INVALID$'):
            self.unit['validate_manifest_scope'](dict(manifest,verificationMode='BOEUN_SEGMENT'),mode)
        expected=[('22f9d58ac8c6a8c76fe3508e58bf367358453b88684af5710437a5133e687827',6,3,1),
                  ('3594adc138acae4211de819c3803295704410a8e8484a2070323042f9f3bda3a',4,2,1),
                  ('f823ab78281f55cb16c634bde50bbc6fc84175c1058c90355c1845ea50aa3673',1,1,0)]
        flags=('structuralObservedHashMatched','evaluationBoundApiVerified','otherVersionReadOnlyVerified',
               'legacyDefaultReadOnlyVerified','segmentEvaluationInputBound','segmentApiProjectionMatched')
        cases=[]
        for code,e in zip(scope[1],expected):
            file=dict(zip(('segmentAnalysisHash','segmentCount','unknownSegmentCount','noticeSegmentCount'),e),quality='COMPLETE_TEXT',**dict.fromkeys(flags,True))
            cases.append({'caseCode':code,'scope':'OFFICIAL_WORKER_EPHEMERAL_DB_API_V1','engineVersion':'attachment-segment-1.0.0',
                          'segmentRuleVersion':'segment-role-1.0.3','segmentRulesHash':'8b9fdd872f3eb9890146d6e360408204ff285f4b23977e07693834aceec66d43',
                          'segmentDatabaseApiVerified':True,'segmentReviewContextVerified':True,'manualSourceCheckRequired':True,
                          'productionWriteCount':0,'isPolicyQaPassed':False,'decisionStatus':'REVIEW_REQUIRED','files':[file],
                          'maximumRequestReservations':5,'maximumReservedBytes':25165824,
                          'requestReservationsIncludingBodyUpperBound':4,'reservedBytesIncludingBodyUpperBound':2400000})
        report={'kind':'OFFICIAL_WORKER_PROBE','caseGroup':mode,'productionDatabaseUsed':False,
                'isPolicyQaPassed':False,'isAuthenticatedBrowserE2e':False,'status':'PASSED','cases':cases}
        self.unit['validate_probe_scope'](report,mode)
        for index in range(3):
            for key,value in [('quality','PARTIAL_TEXT'),('segmentAnalysisHash','a'*64),('segmentCount',0),
                              ('unknownSegmentCount',0),('noticeSegmentCount',True),('structuralCandidate',{}),
                              *((flag,value) for flag in flags for value in (False,'true',None))]:
                invalid=copy.deepcopy(report);invalid['cases'][index]['files'][0][key]=value
                with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
            for key,value in [('segmentRuleVersion','segment-role-1.0.2'),('segmentRulesHash','b'*64),
                              ('decisionStatus','ACCEPTED'),('manualSourceCheckRequired',False),('files',[])]:
                invalid=copy.deepcopy(report);invalid['cases'][index][key]=value
                with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](invalid,mode)
        with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):
            self.unit['validate_probe_scope'](dict(report,caseGroup='BOEUN_SEGMENT'),'BOEUN_SEGMENT')

    def test_boeun_requires_exact_scope_and_worker_kind_without_production_authority(self):
        cases=['BOEUN-221499','BOEUN-221497','BOEUN-218812']
        self.assertEqual(('BOEUN-THREE-NOTICES',cases,132,251658240),self.runner['SCOPES']['BOEUN'])
        self.unit['cfg']={'codeHash':'a'*64}
        manifest={'schemaVersion':1,'caseCode':'BOEUN-THREE-NOTICES','caseCodes':cases,'verificationMode':'BOEUN','executionCodeHash':'a'*64}
        self.unit['validate_manifest_scope'](manifest,'BOEUN')
        for field,value in [('caseCodes',cases[:2]),('caseCodes',cases[::-1]),('verificationMode','OKCHEON'),('caseCode','TAEBAEK-184816')]:
            with self.assertRaisesRegex(ValueError,'^MANIFEST_SCOPE_INVALID$'):
                self.unit['validate_manifest_scope'](dict(manifest,**{field:value}),'BOEUN')
        report={'kind':'OFFICIAL_WORKER_PROBE','caseGroup':'BOEUN','productionDatabaseUsed':False,
                'isPolicyQaPassed':False,'isAuthenticatedBrowserE2e':False,'status':'PASSED','cases':[{'caseCode':c} for c in cases]}
        self.unit['validate_probe_scope'](report,'BOEUN')
        for field,value in [('kind','BBS_OBSERVATION_PROBE'),('caseGroup','OKCHEON'),('productionDatabaseUsed',True),
                            ('isPolicyQaPassed',True),('isAuthenticatedBrowserE2e',True),('cases',report['cases'][:2]),
                            ('cases',[report['cases'][0]]*3),('cases',[{'caseCode':'OTHER'}])]:
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):
                self.unit['validate_probe_scope'](dict(report,**{field:value}),'BOEUN')
        self.unit['validate_probe_scope'](dict(report,status='INCOMPLETE',cases=[]),'BOEUN')

    def test_boeun_observation_uses_new_temporary_package_not_installed_worker(self):
        mode='BOEUN_OBSERVATION'
        self.assertEqual(self.runner['SCOPES']['BOEUN'],self.runner['SCOPES'][mode])
        self.assertEqual([mode],self.unit['select_probe_arguments'](mode))
        with patch('zipfile.ZipFile',side_effect=AssertionError('operating installation accessed')):
            self.assertEqual(pathlib.Path('/tmp/package/qa'),self.unit['select_qa_distribution'](pathlib.Path('/tmp/package'),mode))
        self.unit['cfg']={'codeHash':'a'*64}
        manifest={'schemaVersion':1,'caseCode':'BOEUN-THREE-NOTICES','caseCodes':self.runner['SCOPES'][mode][1],
                  'verificationMode':mode,'executionCodeHash':'a'*64}
        self.unit['validate_manifest_scope'](manifest,mode)
        with self.assertRaisesRegex(ValueError,'^MANIFEST_SCOPE_INVALID$'):
            self.unit['validate_manifest_scope'](dict(manifest,verificationMode='BOEUN'),mode)
        report={'kind':'BBS_OBSERVATION_PROBE','verificationMode':mode}
        self.unit['validate_probe_scope'](report,mode)
        for changed in [dict(report,kind='OFFICIAL_WORKER_PROBE'),dict(report,verificationMode='BOEUN')]:
            with self.assertRaisesRegex(ValueError,'^PROBE_OUTPUT_INVALID$'):self.unit['validate_probe_scope'](changed,mode)

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
