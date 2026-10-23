import { it, expect } from 'vitest';
import { handleRequest, type Env } from '../src/index';

it('returns the frozen retry envelope before key lookup/D1 on shutdown or exhausted verification budget', async () => {
  let keyCalls = 0, databaseCalls = 0;
  const env: Env = {
    FIREBASE_PROJECT_ID: 'calcmot', ABUSE_PROTECTION: 'enforced',
    ABUSE_REQUESTS_PER_MINUTE: '1', ABUSE_READS_PER_UID_PER_MINUTE: '1',
    DB: {prepare: () => { databaseCalls++; throw new Error('D1 must not be reached'); }} as unknown as D1Database
  };
  const request = () => new Request('https://local.invalid/v1/me/entitlement', {
    headers: {Authorization: 'Bearer invalid'}
  });
  const keys = async () => { keyCalls++; throw new Error('must not fetch keys'); };
  const disabled = await handleRequest(request(), {...env, ENTITLEMENT_READ_ENABLED:'false'}, keys);
  expect(disabled.status).toBe(503);
  expect((await handleRequest(request(), env, keys)).status).toBe(401);
  const exhausted = await handleRequest(request(), env, keys);
  expect(exhausted.status).toBe(503);
  expect(await exhausted.json()).toEqual({code:'RETRY_LATER', retryable:true,
    requestId: exhausted.headers.get('X-Request-ID'), serverNow: expect.any(String)});
  expect(exhausted.headers.get('Cache-Control')).toBe('no-store');
  expect(keyCalls).toBe(0); expect(databaseCalls).toBe(0);
});
