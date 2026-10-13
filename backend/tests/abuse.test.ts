import { describe, it, expect } from 'vitest';
import { AbuseGuard, type AbuseConfig } from '../src/abuse';
const config: AbuseConfig = { ABUSE_PROTECTION: 'enforced',
  ABUSE_REQUESTS_PER_MINUTE: '3', ABUSE_READS_PER_UID_PER_MINUTE: '2' };
describe('free bounded isolate abuse controls', () => {
  it('bounds expensive verification attempts and resets at the next minute', () => {
    const guard = new AbuseGuard();
    expect([1,2,3,4].map(() => guard.request(config, 0))).toEqual([true,true,true,false]);
    expect(guard.request(config, 60000)).toBe(true);
  });
  it('isolates verified owners/projects and blocks a read before D1 budget is consumed', () => {
    const guard = new AbuseGuard();
    expect([1,2,3].map(() => guard.read(config, 'project', 'alice', 0))).toEqual([true,true,false]);
    expect(guard.read(config, 'project', 'bob', 0)).toBe(true);
    expect(guard.read(config, 'other', 'alice', 0)).toBe(true);
    expect(guard.read(config, 'project', 'alice', 60000)).toBe(true);
  });
  it('fails closed on enabled invalid configuration and supports a read-only kill switch', () => {
    const guard = new AbuseGuard();
    for (const value of [undefined,'0','-1','NaN','1.5','1000000']) {
      expect(guard.request({...config, ABUSE_REQUESTS_PER_MINUTE: value},0)).toBe(false);
    }
    expect(guard.request({ENTITLEMENT_READ_ENABLED:'false'},0)).toBe(false);
    expect(guard.request({ABUSE_PROTECTION:'unexpected'},0)).toBe(false);
    expect(guard.request({},0)).toBe(true);
  });
  it('bounds memory without letting new identities evict existing counters', () => {
    const guard = new AbuseGuard();
    for(let n=0;n<512;n++) expect(guard.read(config,'project',`owner-${n}`,0)).toBe(true);
    expect(guard.read(config,'project','extra',0)).toBe(false);
    expect(guard.read(config,'project','owner-0',0)).toBe(true);
    expect(guard.read(config,'project','owner-0',0)).toBe(false);
    expect(guard.read(config,'project','extra',60000)).toBe(true);
  });
});
