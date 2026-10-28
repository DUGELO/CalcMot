import {it,expect} from 'vitest';
// @ts-expect-error Offline operator tool, not part of the Worker runtime.
import {assessAdminCpu} from '../scripts/assess-admin-cpu.mjs';
const fixture=()=>({source:'CLOUDFLARE_PLATFORM_METRICS',environment:'PROD',synthetic:false,
  windows:['calcmot-admin-gateway-prod','calcmot-admin-private-prod'].flatMap(worker=>[0,1,2].map(i=>({
    worker,start:`2026-10-23T18:0${i}:00.000Z`,end:`2026-10-23T18:0${i}:30.000Z`,
    authenticatedReads:10,jwtAndD1Verified:true,errors:0,resourceLimitErrors:0,cpuP50Ms:1,cpuP99Ms:3,cpuMaxMs:null}))) });
it('assesses only complete sanitized windows; test values are synthetic fixtures, not measured CPU',()=>{
  expect(assessAdminCpu(fixture()).cpuGate).toBe('PASS');
  const empty={...fixture(),windows:[]};expect(assessAdminCpu(empty).cpuGate).toBe('HOLD');
  expect(assessAdminCpu({...fixture(),synthetic:true}).cpuGate).toBe('HOLD');
});
it('stops on CPU headroom, runtime errors, unverified reads and unknown metrics',()=>{
  for(const change of [{cpuP99Ms:8},{cpuMaxMs:10},{errors:1},{resourceLimitErrors:1},
    {jwtAndD1Verified:false},{authenticatedReads:0},{cpuP50Ms:null},{cpuP99Ms:undefined}]) {
    const data=fixture();Object.assign(data.windows[0],change);
    expect(assessAdminCpu(data).cpuGate).toBe('HOLD');
  }
  const duplicate=fixture();duplicate.windows[1].start=duplicate.windows[0].start;
  expect(assessAdminCpu(duplicate).cpuGate).toBe('HOLD');
});
