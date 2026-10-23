import {describe,expect,it} from 'vitest';
import {operatorReadiness} from '../scripts/operator-readiness';
import type {Session} from '../src/auth';
const now = new Date('2026-10-14T12:00:00Z');
const t = now.getTime()/1000;
const ownerUid = 'private-owner-fixture';
const owner: Session = {uid:ownerUid,claims:{sub:ownerUid,aud:'calcmot',iss:'https://securetoken.google.com/calcmot',
  admin:true,auth_time:t-10,email:'private-fixture@example.invalid'}};
const assess = (session=owner,expected:'owner'|'ordinary'='owner',configured:string|undefined=ownerUid) =>
  operatorReadiness(session,'calcmot',configured,expected,now);
describe('new read-only operator preflight (not grant authorization)',()=>{
  it('confirms the configured owner only with boolean admin and recent reauthentication',()=>{
    expect(assess().status).toBe('GO');
    expect(assess({...owner,claims:{...owner.claims,admin:'true'}}).status).toBe('HOLD');
    expect(assess({...owner,claims:{...owner.claims,admin:false}}).status).toBe('HOLD');
  });
  it('denies an unconfigured owner and does not infer privilege from email',()=>{
    expect(operatorReadiness(owner,'calcmot',undefined,'owner',now).status).toBe('HOLD');
    expect(assess({...owner,uid:'other',claims:{...owner.claims,sub:'other',admin:false}}).status).toBe('HOLD');
  });
  it('flags a different administrator rather than treating it as the owner or ordinary control',()=>{
    const other = {...owner,uid:'other',claims:{...owner.claims,sub:'other'}};
    expect(assess(other).unexpectedOtherAdmin).toBe(true);
    expect(assess(other).status).toBe('HOLD'); expect(assess(other,'ordinary').status).toBe('HOLD');
  });
  it('checks the exact recent-authentication boundary and rejects future/missing auth_time',()=>{
    expect(assess({...owner,claims:{...owner.claims,auth_time:t-300}}).status).toBe('GO');
    for(const auth_time of [t-301,t+1,undefined])
      expect(assess({...owner,claims:{...owner.claims,auth_time}}).status).toBe('HOLD');
  });
  it('requires the same subject, issuer and project as the verified session',()=>{
    for(const change of [{aud:'other-project'},{iss:'https://untrusted.invalid'},{sub:'another'}])
      expect(assess({...owner,claims:{...owner.claims,...change}}).status).toBe('HOLD');
  });
  it('confirms a single ordinary control while explicitly not claiming a global audit',()=>{
    const result = assess({...owner,uid:'other',claims:{...owner.claims,sub:'other',admin:false}},'ordinary');
    expect(result.status).toBe('GO'); expect(result.scope).toContain('no global privilege inventory');
    expect(assess(owner,'ordinary').status).toBe('HOLD');
  });
  it('never outputs private UID, email, bearer or the full claims',()=>{
    const output = JSON.stringify(assess());
    for(const secret of [ownerUid,'private-fixture@example.invalid','Bearer','"claims"']) expect(output).not.toContain(secret);
  });
});
