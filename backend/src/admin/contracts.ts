/** Administrative v1 only. Browser-safe DTOs; no Firebase/Worker runtime imports. */
export type ClaimState = 'UNDER_REVIEW' | 'GRANTED' | 'REJECTED';
export interface ClaimView {
  claimId: string; state: ClaimState; openedAt: string; resolvedAt: string|null;
  provisionalUntil: string|null; revision: number; serverNow: string;
}
export interface Page<T> {items:T[]; nextCursor:string|null; serverNow:string}
export interface Counts {counts:Record<ClaimState,number>; serverNow:string}
export interface AdminSession {
  contractVersion:'1.3.0'; environment:'DEV'|'PROD'; authenticated:true; reauthenticationRequired:boolean;
  mutationsEnabled:boolean;
  reauthenticateBy:string; serverNow:string;
}
export type DecisionKind = 'APPROVE'|'REJECT'|'REQUEST_MORE'|'OWNERSHIP_CONFLICT';
export type EventKind = DecisionKind|'EVIDENCE_ADDED'|'RESTORE'|'GRANT_ISSUED';
export type DecisionBasis = 'NONE'|'VERIFIABLE_HISTORY'|'ASSISTED_ASSESSMENT'|'EXISTING_GRANT';
export interface DecisionSummary {
  eventId:string; kind:DecisionKind; basis:DecisionBasis; toRevision:number; occurredAt:string;
}
export interface GrantSummary {legacyCoreVersionBaseline:string; grantRevision:number; legacyGrantedAt:string}
export interface Inspection {view:ClaimView; latestDecision:DecisionSummary|null; grant:GrantSummary|null}
export interface EvidenceInput {
  kind:'HISTORY'|'AUXILIARY'|'DECLARATION';
  category:'SUPPORT_RECORD'|'INSTALL_CONTEXT'|'USAGE_CONTEXT'|'ACCOUNT_CONTINUITY'|'USER_STATEMENT';
  originKey:string; reference:string; authorityEvidenceId?:string;
}
export interface EvidenceView extends EvidenceInput {evidenceId:string; addedAt:string}
export interface DecisionInput {
  outcome:DecisionKind; evidenceIds:string[]; reason:string; ownershipConfirmed:boolean;
  contradictionsCleared:boolean; legacyUseConcluded:boolean;
}
export interface IssuanceInput {
  approvalEventId:string; confirmation:{claimId:string; permanentCoreOnly:true};
}
export interface DecisionResult {view:ClaimView; decision:DecisionSummary}
export interface IssuanceResult {view:ClaimView; grant:GrantSummary; idempotent:boolean}
export interface EventView {
  eventId:string; kind:EventKind; basis:DecisionBasis; actor:'OWNER'|'SYSTEM'; occurredAt:string;
  fromRevision:number; toRevision:number;
  details:{reason?:string; evidenceIds?:string[]; evidenceId?:string; ownershipConfirmed?:boolean;
    contradictionsCleared?:boolean; legacyUseConcluded?:boolean; grantIssuance?:string};
}
export type ErrorCode = 'AUTH_REQUIRED'|'FORBIDDEN'|'REAUTH_REQUIRED'|'INVALID_REQUEST'|'NOT_FOUND'|
  'METHOD_NOT_ALLOWED'|'PAYLOAD_TOO_LARGE'|'UNSUPPORTED_MEDIA_TYPE'|'PRECONDITION_REQUIRED'|
  'CONFLICT'|'GRANT_PRESERVED'|'MORE_EVIDENCE_REQUIRED'|'LINK_REQUIRED'|'POLICY_REQUIRED'|
  'RATE_LIMITED'|'RETRY_LATER';
export interface AdminError {code:ErrorCode; message:string; requestId:string; retryable:boolean; serverNow:string}
