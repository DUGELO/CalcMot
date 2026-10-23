// Offline assessment of sanitized real platform metrics. Does not generate traffic or credentials.
import {readFileSync} from 'node:fs';
import {pathToFileURL} from 'node:url';
import {resolve} from 'node:path';
export function assessAdminCpu(input) {
  const reasons=[];
  if(input?.source!=='CLOUDFLARE_PLATFORM_METRICS'||input?.environment!=='PROD'||input?.synthetic!==false)
    reasons.push('REAL_PLATFORM_METRICS_REQUIRED');
  const workers=['calcmot-admin-gateway-prod','calcmot-admin-private-prod'];
  const summaries=[];
  for(const worker of workers) {
    const windows=Array.isArray(input?.windows)?input.windows.filter(x=>x&&x.worker===worker):[];
    const unique=new Set();
    if(windows.length<3)reasons.push(worker+':THREE_WINDOWS_REQUIRED');
    for(const row of windows) {
      const start=Date.parse(row.start),end=Date.parse(row.end);
      if(!Number.isFinite(start)||!Number.isFinite(end)||end<=start||end-start>10*60000||unique.has(row.start))
        reasons.push(worker+':INVALID_OR_DUPLICATE_WINDOW');
      unique.add(row.start);
      if(!Number.isInteger(row.authenticatedReads)||row.authenticatedReads<10||row.jwtAndD1Verified!==true)
        reasons.push(worker+':AUTHENTICATED_D1_SAMPLE_REQUIRED');
      if(!Number.isInteger(row.errors)||row.errors!==0)reasons.push(worker+':ERRORS_OR_MISSING_ERRORS');
      if(!Number.isFinite(row.cpuP50Ms)||row.cpuP50Ms<0||!Number.isFinite(row.cpuP99Ms)||row.cpuP99Ms<row.cpuP50Ms)
        reasons.push(worker+':CPU_UNITS_OR_VALUES_INVALID');
      else if(row.cpuP99Ms>=8)reasons.push(worker+':CPU_MARGIN_NOT_MET');
      if(row.resourceLimitErrors!==0)reasons.push(worker+':RESOURCE_LIMIT_ERRORS_OR_UNKNOWN');
      if(row.cpuMaxMs!==null&&(!Number.isFinite(row.cpuMaxMs)||row.cpuMaxMs>=10||row.cpuMaxMs<row.cpuP99Ms))
        reasons.push(worker+':CPU_MAX_NOT_MET');
    }
    summaries.push({worker,windows:windows.length,
      cpuP99WorstMs:windows.length?Math.max(...windows.map(x=>x.cpuP99Ms)):null,
      maxAvailable:windows.length>0&&windows.every(x=>x.cpuMaxMs!==null&&Number.isFinite(x.cpuMaxMs))});
  }
  return {cpuGate:reasons.length?'HOLD':'PASS',reasons:[...new Set(reasons)],workers:summaries,
    limitMs:10,p99HeadroomThresholdMs:8,notAnAvailabilityGuarantee:true};
}
if(process.argv[1]&&import.meta.url===pathToFileURL(resolve(process.argv[1])).href) {
  if(process.argv.length!==3)throw Error('SANITIZED_METRICS_JSON_REQUIRED');
  const result=assessAdminCpu(JSON.parse(readFileSync(process.argv[2],'utf8')));
  console.log(JSON.stringify(result,null,2));if(result.cpuGate!=='PASS')process.exitCode=1;
}
