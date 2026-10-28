import {test} from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import {createPolicyReceiver,allowedOrigin} from './prod-policy-session.mjs';
async function fixture(t,options={}){
  const receiver=createPolicyReceiver({nonce:'a'.repeat(64),apply:async()=>({policyOnly:true,grantsWritten:0}),...options});
  await new Promise(resolve=>receiver.server.listen(0,'127.0.0.1',resolve));
  t.after(()=>new Promise(resolve=>receiver.server.close(resolve)));
  const url=`http://127.0.0.1:${receiver.server.address().port}/bootstrap`;
  const post=(body={},headers={})=>fetch(url,{method:'POST',headers:{Origin:allowedOrigin,'Content-Type':'application/json',...headers},
    body:JSON.stringify({nonce:'a'.repeat(64),token:'synthetic-test-token-not-a-credential',...body})});
  return {receiver,url,post};
}
test('one-use explicit handoff returns only the operation receipt',async t=>{
  let calls=0;const {post}=await fixture(t,{apply:async()=>{calls++;return {policyOnly:true,grantsWritten:0};}});
  const response=await post();assert.equal(response.status,200);assert.equal(response.headers.get('Cache-Control'),'no-store');
  assert.deepEqual(await response.json(),{policyOnly:true,grantsWritten:0});
  assert.equal((await post()).status,409);assert.equal(calls,1);
});
test('wrong origin, missing nonce and declared UID cannot reach apply',async t=>{
  let calls=0;const {post}=await fixture(t,{apply:async()=>{calls++;}});
  assert.equal((await post({}, {Origin:'https://attacker.invalid'})).status,403);
  assert.equal((await post({nonce:'wrong'})).status,403);
  assert.equal((await post({uid:'declared'})).status,403);assert.equal(calls,0);
});
test('rejects expired handoffs, oversized bodies and unsupported content types',async t=>{
  let now=0;const {post}=await fixture(t,{now:()=>now});
  assert.equal((await post({}, {'Content-Type':'text/plain'})).status,415);
  assert.equal((await post({token:'x'.repeat(17000)})).status,413);
  now=600000;assert.equal((await post()).status,409);
});
test('concurrent requests cannot execute twice',async t=>{
  let release;const pending=new Promise(resolve=>release=resolve);let calls=0;
  const {post}=await fixture(t,{apply:async()=>{calls++;await pending;return {grantsWritten:0};}});
  const first=post();while(calls===0)await new Promise(resolve=>setImmediate(resolve));
  assert.equal((await post()).status,409);release();assert.equal((await first).status,200);assert.equal(calls,1);
});
test('failed authentication never leaks the error or token and cannot replay',async t=>{
  const {post}=await fixture(t,{apply:async()=>{throw Error('sensitive-test-detail');}});
  const result=await post();assert.equal(result.status,403);assert.deepEqual(await result.json(),{code:'BOOTSTRAP_NOT_COMPLETED'});
  assert.equal((await post()).status,409);
});
test('CORS/PNA preflight is restricted to official origin and POST',async t=>{
  const {url}=await fixture(t);
  const response=await fetch(url,{method:'OPTIONS',headers:{Origin:allowedOrigin}});
  assert.equal(response.status,204);assert.equal(response.headers.get('Access-Control-Allow-Origin'),allowedOrigin);
  assert.equal(response.headers.get('Access-Control-Allow-Methods'),'POST');
  assert.equal((await fetch(url,{method:'OPTIONS',headers:{Origin:'https://attacker.invalid'}})).status,403);
});
test('challenge requires the nonce and returns no credentials or identity',async t=>{
  let calls=0;const {url,post}=await fixture(t,{apply:async()=>{calls++;return {grantsWritten:0};}});
  const challenge=(nonce)=>fetch(url.replace('/bootstrap','/challenge'),{method:'POST',
    headers:{Origin:allowedOrigin,'Content-Type':'application/json'},body:JSON.stringify({nonce})});
  assert.equal((await challenge('bad')).status,403);
  const response=await challenge('a'.repeat(64));assert.equal(response.status,200);
  assert.deepEqual(await response.json(),{protocolVersion:1,environment:'PROD',policyVersion:'legacy-through-3.5-vc12',grantsWritten:0});
  assert.equal(calls,0);assert.equal((await post()).status,200);
});
test('DNS rebinding host is denied before any operation',async t=>{
  let calls=0;const {url}=await fixture(t,{apply:async()=>{calls++;}});
  const status=await new Promise((resolve,reject)=>{
    const req=http.request(url,{method:'POST',headers:{Host:'attacker.invalid',Origin:allowedOrigin}},res=>{
      res.resume();resolve(res.statusCode);
    });req.on('error',reject);req.end();
  });
  assert.equal(status,403);assert.equal(calls,0);
});
