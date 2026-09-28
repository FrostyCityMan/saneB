param([Parameter(Mandatory)][string]$PlanPath,[switch]$CheckOnly)
$ErrorActionPreference='Stop'
$qaRoot=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$qaPlanFile=(Resolve-Path -LiteralPath $PlanPath).Path
if(-not $qaPlanFile.StartsWith(($qaRoot+'\build\temporary-bbs-qa-'),[StringComparison]::OrdinalIgnoreCase) -or [IO.Path]::GetFileName($qaPlanFile) -ne 'plan.json'){throw 'HWACHEON_PLAN_PATH_INVALID'}
$qaPlan=Get-Content -LiteralPath $qaPlanFile -Raw|ConvertFrom-Json
if($qaPlan.schemaVersion -ne 1 -or $qaPlan.verificationMode -ne 'HWACHEON_SEGMENT' -or $qaPlan.caseCode -ne 'HWACHEON-32258' -or
 ($qaPlan.caseCodes -join ',') -ne 'HWACHEON-32258' -or $qaPlan.executionId -notmatch '^[a-f0-9]{32}$' -or
 [IO.Path]::GetFileName([IO.Path]::GetDirectoryName($qaPlanFile)) -ne ('temporary-bbs-qa-'+$qaPlan.executionId) -or $qaPlan.uploaded -or $qaPlan.commandId){throw 'HWACHEON_PLAN_SCOPE_INVALID'}
$qaReceiptPath=Join-Path $qaRoot 'build/qa-results/hwacheon-support-32258-result.json'
$qaExpectedReceipt='6a22c45345e5a8580b04ed04efaeef9eea6ac92281f1fb697a6f9890779fd1cd'
if((Get-FileHash -LiteralPath $qaReceiptPath -Algorithm SHA256).Hash.ToLowerInvariant() -ne $qaExpectedReceipt){throw 'HWACHEON_PRIOR_RECEIPT_CHANGED'}
$qaReceipt=Get-Content -LiteralPath $qaReceiptPath -Raw|ConvertFrom-Json
if($qaReceipt.status -ne 'DISCOVERY_DOWNLOAD_SIGNATURE_PASSED' -or $qaReceipt.caseCode -ne 'HWACHEON-32258' -or
 $qaReceipt.requestReservations -ne 2 -or $qaReceipt.reservedBytes -ne 92910 -or $qaReceipt.temporaryOriginalRemoved -ne $true -or
 $qaReceipt.productionWriteCount -ne 0 -or $qaReceipt.extractionExecuted -ne $false -or
 $qaReceipt.files.Count -ne 1 -or $qaReceipt.files[0].binaryHash -ne 'dbeba265406d420c2a396e6f7d40368f499ab5ec25bc510004e4f27b964d3ea0'){throw 'HWACHEON_PRIOR_SCOPE_CHANGED'}
$qaExisting=@(Get-ChildItem -Path (Join-Path $qaRoot 'build/temporary-bbs-qa-*/plan.json') -File|Where-Object FullName -ne $qaPlanFile|ForEach-Object {
 Get-Content -LiteralPath $_.FullName -Raw|ConvertFrom-Json
}|Where-Object {($_.caseCodes -contains 'HWACHEON-32258') -and ($_.uploaded -or $_.commandId)})
if($qaExisting.Count -ne 0){throw 'HWACHEON_PRIOR_WORKER_REQUIRES_REVIEW'}
$qaReservation=Join-Path $qaRoot 'build/qa-results/hwacheon-worker-1.0.15-reservation.json'
if(Test-Path -LiteralPath $qaReservation){throw 'HWACHEON_WORKER_ALREADY_RESERVED'}
$qaRecord=[ordered]@{caseCode='HWACHEON-32258';executionId=$qaPlan.executionId;mode='HWACHEON_SEGMENT';priorReceiptSha256=$qaExpectedReceipt;priorRequests=2;priorBytes=92910;maximumAdditionalRequests=5;maximumAdditionalBytes=25165824;reservedCumulativeRequests=7;reservedCumulativeBytes=25258734;maximumSeconds=1200;productionWrites=0}
if(-not $CheckOnly){
 $qaBytes=[Text.UTF8Encoding]::new($false).GetBytes(($qaRecord|ConvertTo-Json -Compress))
 $qaStream=[IO.File]::Open($qaReservation,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
 try{$qaStream.Write($qaBytes,0,$qaBytes.Length)}finally{$qaStream.Dispose()}
}
[pscustomobject]@{kind='HWACHEON_FIXED_WORKER_BUDGET';checkOnly=[bool]$CheckOnly;reservedRequests=7;reservedBytes=25258734;maximumAdditionalRequests=5;maximumAdditionalBytes=25165824}|ConvertTo-Json -Compress
