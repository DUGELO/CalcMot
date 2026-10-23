/** One dispatcher shared by stdin console and loopback pilot. Caller verifies JWT and owner first. */
export async function executeConsoleCommand(api,env,session,request) {
  const review=new api.LegacyReviewService(env);
  const privacy=new api.LegacyPrivacyService(env);
  let uid=request.uid;
  if(!['open','own','policy'].includes(request.action)&&uid===undefined) {
    if(typeof request.claimId!=='string'||request.claimId.length>128)throw new Error('INVALID_REQUEST');
    const target=await env.DB.prepare('SELECT uid FROM legacy_claims WHERE firebase_project_id=? AND claim_id=?')
      .bind(env.FIREBASE_PROJECT_ID,request.claimId).first();
    if(!target)throw new Error('NOT_FOUND');uid=target.uid;
  }
  switch(request.action) {
    case 'open': return review.open(session,request.input,request.key);
    case 'own': return review.getOwn(session);
    case 'inspect': return review.inspect(session,uid,request.claimId);
    case 'evidence': return review.addEvidence(session,uid,request.claimId,request.revision,request.input,request.key);
    case 'decide': return review.decide(session,uid,request.claimId,request.revision,request.input,request.key);
    case 'policy': return review.authorizePolicy(session);
    case 'issue': return review.issue(session,uid,request.claimId,request.revision,request.input,request.key);
    case 'privacy-plan': return privacy.plan(session,uid,request.input?.mode);
    case 'privacy-close': return privacy.closeCase(session,uid);
    case 'privacy-execute': return privacy.execute(session,uid,request.input?.mode,request.input?.confirmation);
    default: throw new Error('INVALID_REQUEST');
  }
}
