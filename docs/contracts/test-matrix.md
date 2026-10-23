# Test matrix e fixtures Given/When/Then — v1.0.0

Fonte: [README/invariantes](README.md), [Billing](billing-state-matrix.md), [Analytics](analytics-events.md), schemas e [API](api.openapi.yaml). Escopo: S01 documental exclusivamente.

## O que é validado nesta story

- Metaschema JSON Schema 2020-12 de raízes/definições e OpenAPI 3.1 com todas as referências locais.
- Fixtures positivas/negativas por tipo e status HTTP, autoridade de emissor, proibição de parâmetros privados e enums únicos.
- Oráculos normativos de acesso/validade, progresso cumulativo, qualificação/void/canal, comissão idempotente, dedupe de uso, timeout/defer e créditos após rollback.
- Coerência dos nomes entre documentos/schemas/OpenAPI; decisões comerciais pendentes explicitamente sem default.

Os oráculos apenas verificam regras do contrato em dados sintéticos. **Não são implementação de policy engine, backend ou tests Android.** Ambiente, prova oficial financeira e validação device permanecem nas stories responsáveis. Fixture GRANTED/paid não significa evidência de grant/pagamento real.

## Formato das fixtures

O bloco JSON abaixo é parte normativa dos contratos. `baseFixtures` contém objetos completos com referência de schema. Cada `schemaCases` tem Given/When/Then, `baseFixture`, `set` (JSON Pointers a substituir/inserir) e `remove` (JSON Pointers a remover). Ausência de set/remove mantém base. `expectedValid=false` requer rejeição, não erro do validador/resolver. Os dados são clonados por caso para não contaminar os próximos.

`semanticCases` contém entradas e resultados esperados das invariantes que JSON Schema não expressa (tempo/contagem/identidade/efeito externo). O instant autoritativo é serverNow fixo da fixture; datas, provider/policy/threshold de exemplo **não** são decisões comerciais. Tokens/IDs marcados fixture nunca devem ser usados em Play.

```json
{
  "fixtureVersion": "1.0.0",
  "note": "Dados integralmente sintéticos. Datas/policy/provider/janelas de fixtures não configuram produção. Estrutura/oracles documentais apenas; não prova ambiente Play ou implementação Android.",
  "baseFixtures": {
    "snapshot_blocked": {
      "schema": "entitlement.schema.json#/$defs/EntitlementSnapshot",
      "instance": {
        "contractVersion": "1.0.0",
        "revision": 1,
        "serverNow": "2026-10-09T12:00:00Z",
        "checkedAt": "2026-10-09T12:00:00Z",
        "subscriptionRequired": true,
        "legacyCore": false,
        "coreAllowed": false,
        "proAllowed": false,
        "proFeatureSet": [],
        "legacy": {
          "state": "UNKNOWN",
          "legacyGrantedAt": null,
          "legacyEvidenceType": null,
          "legacyCoreVersionBaseline": null,
          "migrationVersion": null,
          "grantRevision": null
        },
        "play": {
          "state": "NONE",
          "billingPhase": "NONE",
          "validUntil": null,
          "checkedAt": null,
          "productId": null,
          "basePlanId": null
        },
        "promo": {
          "state": "NONE",
          "startAt": null,
          "validUntil": null
        },
        "review": {
          "claimId": null,
          "provisionalUntil": null
        },
        "nextAction": "CHOOSE_PLAN"
      }
    },
    "snapshot_legacy": {
      "schema": "entitlement.schema.json#/$defs/EntitlementSnapshot",
      "instance": {
        "contractVersion": "1.0.0",
        "revision": 1,
        "serverNow": "2026-10-09T12:00:00Z",
        "checkedAt": "2026-10-09T12:00:00Z",
        "subscriptionRequired": true,
        "legacyCore": true,
        "coreAllowed": true,
        "proAllowed": false,
        "proFeatureSet": [],
        "legacy": {
          "state": "GRANTED",
          "legacyGrantedAt": "2026-10-01T12:00:00Z",
          "legacyEvidenceType": "fixture_review",
          "legacyCoreVersionBaseline": "fixture_baseline",
          "migrationVersion": "fixture_migration",
          "grantRevision": 1
        },
        "play": {
          "state": "NONE",
          "billingPhase": "NONE",
          "validUntil": null,
          "checkedAt": null,
          "productId": null,
          "basePlanId": null
        },
        "promo": {
          "state": "NONE",
          "startAt": null,
          "validUntil": null
        },
        "review": {
          "claimId": null,
          "provisionalUntil": null
        },
        "nextAction": "NONE"
      }
    },
    "snapshot_trial": {
      "schema": "entitlement.schema.json#/$defs/EntitlementSnapshot",
      "instance": {
        "contractVersion": "1.0.0",
        "revision": 1,
        "serverNow": "2026-10-09T12:00:00Z",
        "checkedAt": "2026-10-09T12:00:00Z",
        "subscriptionRequired": true,
        "legacyCore": false,
        "coreAllowed": true,
        "proAllowed": true,
        "proFeatureSet": [],
        "legacy": {
          "state": "UNKNOWN",
          "legacyGrantedAt": null,
          "legacyEvidenceType": null,
          "legacyCoreVersionBaseline": null,
          "migrationVersion": null,
          "grantRevision": null
        },
        "play": {
          "state": "ACTIVE",
          "billingPhase": "TRIAL",
          "validUntil": "2026-10-16T12:00:00Z",
          "checkedAt": "2026-10-09T12:00:00Z",
          "productId": "fixture_product",
          "basePlanId": "fixture_monthly"
        },
        "promo": {
          "state": "NONE",
          "startAt": null,
          "validUntil": null
        },
        "review": {
          "claimId": null,
          "provisionalUntil": null
        },
        "nextAction": "NONE"
      }
    },
    "snapshot_review": {
      "schema": "entitlement.schema.json#/$defs/EntitlementSnapshot",
      "instance": {
        "contractVersion": "1.0.0",
        "revision": 1,
        "serverNow": "2026-10-09T12:00:00Z",
        "checkedAt": "2026-10-09T12:00:00Z",
        "subscriptionRequired": true,
        "legacyCore": false,
        "coreAllowed": true,
        "proAllowed": false,
        "proFeatureSet": [],
        "legacy": {
          "state": "UNDER_REVIEW",
          "legacyGrantedAt": null,
          "legacyEvidenceType": null,
          "legacyCoreVersionBaseline": null,
          "migrationVersion": null,
          "grantRevision": null
        },
        "play": {
          "state": "NONE",
          "billingPhase": "NONE",
          "validUntil": null,
          "checkedAt": null,
          "productId": null,
          "basePlanId": null
        },
        "promo": {
          "state": "NONE",
          "startAt": null,
          "validUntil": null
        },
        "review": {
          "claimId": "fixture_claim",
          "provisionalUntil": "2026-10-10T12:00:00Z"
        },
        "nextAction": "CONTACT_SUPPORT"
      }
    },
    "legacy_grant": {
      "schema": "entitlement.schema.json#/$defs/LegacyGrant",
      "instance": {
        "uid": "fixture_driver",
        "status": "GRANTED",
        "policyCutoffUtc": null,
        "policyVersion": "fixture_bridge_policy",
        "legacyGrantedAt": "2026-10-09T12:00:00Z",
        "legacyEvidenceType": "fixture_review",
        "evidenceHash": "fixture_evidence_hash",
        "legacyCoreVersionBaseline": "fixture_baseline",
        "migrationVersion": "fixture_migration",
        "grantRevision": 1,
        "decisionActor": "fixture_admin"
      }
    },
    "legacy_claim": {
      "schema": "entitlement.schema.json#/$defs/LegacyClaim",
      "instance": {
        "claimId": "fixture_claim",
        "uid": "fixture_driver",
        "state": "UNDER_REVIEW",
        "evidenceType": "fixture_unknown",
        "evidenceHash": null,
        "openedAt": "2026-10-09T12:00:00Z",
        "resolvedAt": null,
        "reviewer": null,
        "provisionalUntil": null,
        "policyVersion": "fixture_policy"
      }
    },
    "attribution_locked": {
      "schema": "referral.schema.json#/$defs/Attribution",
      "instance": {
        "inviteeUid": "fixture_invitee",
        "state": "LOCKED",
        "sourceType": "DRIVER",
        "sourceId": "fixture_driver",
        "campaignId": "fixture_campaign",
        "code": "FIXTURE1",
        "method": "INSTALL_REFERRER",
        "claimedAt": "2026-10-09T11:00:00Z",
        "lockedAt": "2026-10-09T12:00:00Z"
      }
    },
    "referral_qualified": {
      "schema": "referral.schema.json#/$defs/ReferralQualification",
      "instance": {
        "referralId": "fixture_referral",
        "campaignId": "fixture_campaign",
        "inviterUid": "fixture_driver",
        "inviteeUid": "fixture_invitee",
        "sourceType": "DRIVER",
        "state": "QUALIFIED",
        "firstCoreAt": null,
        "firstPaidAt": "2026-10-01T12:00:00Z",
        "firstPaidRef": "fixture_payment_ref",
        "qualificationEligibleAt": "2026-10-09T12:00:00Z",
        "qualifiedAt": "2026-10-09T12:00:00Z",
        "disqualificationReason": null
      }
    },
    "reward_earned": {
      "schema": "referral.schema.json#/$defs/RewardLedgerEntry",
      "instance": {
        "rewardId": "fixture_reward_1",
        "campaignId": "fixture_campaign",
        "inviterUid": "fixture_driver",
        "milestone": 1,
        "targetDays": 7,
        "deltaDays": 7,
        "appliedDays": 0,
        "state": "EARNED",
        "idempotencyKey": "fixture_campaign:fixture_driver:1",
        "sourcePurchaseHash": "fixture_sale_hash",
        "createdAt": "2026-10-09T12:00:00Z",
        "operationId": null,
        "applicationMode": "UNDECIDED",
        "expectedExpiry": null,
        "observedExpiry": null,
        "rewardAppliedAt": null,
        "promoStart": null,
        "promoEnd": null,
        "etag": null
      }
    },
    "reward_applied": {
      "schema": "referral.schema.json#/$defs/RewardLedgerEntry",
      "instance": {
        "rewardId": "fixture_reward_1",
        "campaignId": "fixture_campaign",
        "inviterUid": "fixture_driver",
        "milestone": 1,
        "targetDays": 7,
        "deltaDays": 7,
        "appliedDays": 7,
        "state": "APPLIED",
        "idempotencyKey": "fixture_campaign:fixture_driver:1",
        "sourcePurchaseHash": "fixture_sale_hash",
        "createdAt": "2026-10-09T12:00:00Z",
        "operationId": "fixture_operation_1",
        "applicationMode": "PLAY_DEFER",
        "expectedExpiry": "2026-11-09T12:00:00Z",
        "observedExpiry": "2026-11-16T12:00:00Z",
        "rewardAppliedAt": "2026-10-09T12:00:00Z",
        "promoStart": null,
        "promoEnd": null,
        "etag": "fixture_etag"
      }
    },
    "reward_needs_reconciliation": {
      "schema": "referral.schema.json#/$defs/RewardLedgerEntry",
      "instance": {
        "rewardId": "fixture_reward_1",
        "campaignId": "fixture_campaign",
        "inviterUid": "fixture_driver",
        "milestone": 1,
        "targetDays": 7,
        "deltaDays": 7,
        "appliedDays": 0,
        "state": "NEEDS_RECONCILIATION",
        "idempotencyKey": "fixture_campaign:fixture_driver:1",
        "sourcePurchaseHash": "fixture_sale_hash",
        "createdAt": "2026-10-09T12:00:00Z",
        "operationId": "fixture_operation_1",
        "applicationMode": "PLAY_DEFER",
        "expectedExpiry": "2026-11-09T12:00:00Z",
        "observedExpiry": null,
        "rewardAppliedAt": null,
        "promoStart": null,
        "promoEnd": null,
        "etag": "fixture_etag"
      }
    },
    "reward_view": {
      "schema": "referral.schema.json#/$defs/RewardView",
      "instance": {
        "rewardId": "fixture_reward_1",
        "milestone": 1,
        "targetDays": 7,
        "deltaDays": 7,
        "appliedDays": 7,
        "state": "APPLIED",
        "applicationMode": "PLAY_DEFER",
        "rewardAppliedAt": "2026-10-09T12:00:00Z",
        "validUntil": "2026-11-16T12:00:00Z",
        "redeemUntil": null,
        "nextAction": "NONE"
      }
    },
    "paused_summary": {
      "schema": "referral.schema.json#/$defs/ReferralSummary",
      "instance": {
        "contractVersion": "1.0.0",
        "revision": 3,
        "serverNow": "2026-10-09T12:00:00Z",
        "programMode": "REWARDS_PAUSED",
        "referralCode": "FIXTURE1",
        "referralLink": "https://calcmot.com.br/r/FIXTURE1",
        "qualifiedCount": 3,
        "pendingCount": 1,
        "earnedDays": 30,
        "appliedDays": 7,
        "nextGoal": 5,
        "rewards": [
          {
            "rewardId": "fixture_reward_1",
            "milestone": 1,
            "targetDays": 7,
            "deltaDays": 7,
            "appliedDays": 7,
            "state": "APPLIED",
            "applicationMode": "PLAY_DEFER",
            "rewardAppliedAt": "2026-10-09T12:00:00Z",
            "validUntil": "2026-11-16T12:00:00Z",
            "redeemUntil": null,
            "nextAction": "NONE"
          },
          {
            "rewardId": "fixture_reward_3",
            "milestone": 3,
            "targetDays": 30,
            "deltaDays": 23,
            "appliedDays": 0,
            "state": "EARNED",
            "applicationMode": "UNDECIDED",
            "rewardAppliedAt": null,
            "validUntil": null,
            "redeemUntil": null,
            "nextAction": "WAIT_ELIGIBLE_STATE"
          }
        ]
      }
    },
    "commission_eligible": {
      "schema": "referral.schema.json#/$defs/CommissionLedgerEntry",
      "instance": {
        "commissionId": "fixture_commission_1",
        "creatorId": "fixture_creator",
        "inviteeUid": "fixture_invitee",
        "campaignId": "fixture_campaign",
        "economicPurchaseKey": "fixture_sale_1",
        "milestone": "FIRST_CHARGE",
        "amountMinor": 300,
        "currency": "BRL",
        "state": "ELIGIBLE",
        "idempotencyKey": "fixture_creator:fixture_invitee:FIRST_CHARGE",
        "paymentRefHash": "fixture_payment_ref",
        "eligibilityAt": "2026-10-09T12:00:00Z",
        "settledAt": null,
        "batchId": null
      }
    },
    "commission_settled": {
      "schema": "referral.schema.json#/$defs/CommissionLedgerEntry",
      "instance": {
        "commissionId": "fixture_commission_renewal",
        "creatorId": "fixture_creator",
        "inviteeUid": "fixture_invitee",
        "campaignId": "fixture_campaign",
        "economicPurchaseKey": "fixture_sale_2",
        "milestone": "FIRST_RENEWAL",
        "amountMinor": 100,
        "currency": "BRL",
        "state": "SETTLED",
        "idempotencyKey": "fixture_creator:fixture_invitee:FIRST_RENEWAL",
        "paymentRefHash": "fixture_payment_ref",
        "eligibilityAt": "2026-10-09T12:00:00Z",
        "settledAt": "2026-10-09T12:00:00Z",
        "batchId": "fixture_batch"
      }
    },
    "purchase_trial": {
      "schema": "entitlement.schema.json#/$defs/PurchaseEvent",
      "instance": {
        "purchaseEventId": "fixture_trial",
        "economicPurchaseKey": "fixture_trial_key",
        "uid": "fixture_invitee",
        "subscriptionId": "fixture_subscription",
        "category": "TRIAL_STARTED",
        "verificationState": "VERIFIED",
        "paymentStatus": "UNKNOWN",
        "orderRefHash": null,
        "amountMinor": 0,
        "currency": "BRL",
        "verifiedAt": "2026-10-09T12:00:00Z",
        "firstRenewal": false
      }
    },
    "purchase_paid": {
      "schema": "entitlement.schema.json#/$defs/PurchaseEvent",
      "instance": {
        "purchaseEventId": "fixture_charge",
        "economicPurchaseKey": "fixture_paid_key",
        "uid": "fixture_invitee",
        "subscriptionId": "fixture_subscription",
        "category": "FIRST_CHARGE",
        "verificationState": "VERIFIED",
        "paymentStatus": "PAID",
        "orderRefHash": "fixture_order_hash",
        "amountMinor": 1000,
        "currency": "BRL",
        "verifiedAt": "2026-10-09T12:00:00Z",
        "firstRenewal": false
      }
    },
    "analytics_core": {
      "schema": "entitlement.schema.json#/$defs/AnalyticsEnvelope",
      "instance": {
        "name": "core_active_day",
        "emitter": "CLIENT",
        "occurredAt": "2026-10-09T12:00:00Z",
        "dedupeKey": "fixture_identity:2026-10-09",
        "params": {
          "app_family": "uber"
        }
      }
    },
    "analytics_financial": {
      "schema": "entitlement.schema.json#/$defs/AnalyticsEnvelope",
      "instance": {
        "name": "first_charge_verified",
        "emitter": "BACKEND",
        "occurredAt": "2026-10-09T12:00:00Z",
        "dedupeKey": "fixture_paid_key",
        "params": {
          "plan": "MONTHLY"
        }
      }
    },
    "api_purchase": {
      "schema": "api.openapi.yaml#/components/schemas/PurchaseRequest",
      "instance": {
        "purchaseToken": "FIXTURE_TOKEN_DO_NOT_USE"
      }
    },
    "api_redeem": {
      "schema": "api.openapi.yaml#/components/schemas/RedeemRequest",
      "instance": {}
    },
    "api_attribution": {
      "schema": "api.openapi.yaml#/components/schemas/AttributionClaimRequest",
      "instance": {
        "action": "LOCK",
        "code": null,
        "method": "ORGANIC"
      }
    },
    "api_verified": {
      "schema": "api.openapi.yaml#/paths/~1v1~1billing~1purchases/post/responses/200/content/application~1json/schema",
      "instance": {
        "state": "VERIFIED",
        "entitlementRevision": 2,
        "reason": null,
        "requestId": "fixture_request",
        "serverNow": "2026-10-09T12:00:00Z"
      }
    },
    "api_admin_result": {
      "schema": "api.openapi.yaml#/paths/~1admin~1legacy~1grant/post/responses/200/content/application~1json/schema",
      "instance": {
        "claimId": "fixture_claim",
        "state": "GRANTED",
        "openedAt": "2026-10-01T12:00:00Z",
        "resolvedAt": "2026-10-09T12:00:00Z",
        "provisionalUntil": null,
        "revision": 2,
        "serverNow": "2026-10-09T12:00:00Z"
      }
    }
  },
  "schemaCases": [
    {
      "id": "P01",
      "given": "Fixture sintética snapshot_blocked com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra entitlement.schema.json#/$defs/EntitlementSnapshot.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "snapshot_blocked",
      "expectedValid": true
    },
    {
      "id": "P02",
      "given": "Fixture sintética snapshot_legacy com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra entitlement.schema.json#/$defs/EntitlementSnapshot.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "snapshot_legacy",
      "expectedValid": true
    },
    {
      "id": "P03",
      "given": "Fixture sintética snapshot_trial com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra entitlement.schema.json#/$defs/EntitlementSnapshot.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "snapshot_trial",
      "expectedValid": true
    },
    {
      "id": "P04",
      "given": "Fixture sintética snapshot_review com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra entitlement.schema.json#/$defs/EntitlementSnapshot.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "snapshot_review",
      "expectedValid": true
    },
    {
      "id": "P05",
      "given": "Fixture sintética legacy_grant com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra entitlement.schema.json#/$defs/LegacyGrant.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "legacy_grant",
      "expectedValid": true
    },
    {
      "id": "P06",
      "given": "Fixture sintética legacy_claim com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra entitlement.schema.json#/$defs/LegacyClaim.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "legacy_claim",
      "expectedValid": true
    },
    {
      "id": "P07",
      "given": "Fixture sintética attribution_locked com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra referral.schema.json#/$defs/Attribution.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "attribution_locked",
      "expectedValid": true
    },
    {
      "id": "P08",
      "given": "Fixture sintética referral_qualified com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra referral.schema.json#/$defs/ReferralQualification.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "referral_qualified",
      "expectedValid": true
    },
    {
      "id": "P09",
      "given": "Fixture sintética reward_earned com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra referral.schema.json#/$defs/RewardLedgerEntry.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "reward_earned",
      "expectedValid": true
    },
    {
      "id": "P10",
      "given": "Fixture sintética reward_applied com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra referral.schema.json#/$defs/RewardLedgerEntry.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "reward_applied",
      "expectedValid": true
    },
    {
      "id": "P11",
      "given": "Fixture sintética reward_needs_reconciliation com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra referral.schema.json#/$defs/RewardLedgerEntry.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "reward_needs_reconciliation",
      "expectedValid": true
    },
    {
      "id": "P12",
      "given": "Fixture sintética reward_view com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra referral.schema.json#/$defs/RewardView.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "reward_view",
      "expectedValid": true
    },
    {
      "id": "P13",
      "given": "Fixture sintética paused_summary com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra referral.schema.json#/$defs/ReferralSummary.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "paused_summary",
      "expectedValid": true
    },
    {
      "id": "P14",
      "given": "Fixture sintética commission_eligible com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra referral.schema.json#/$defs/CommissionLedgerEntry.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "commission_eligible",
      "expectedValid": true
    },
    {
      "id": "P15",
      "given": "Fixture sintética commission_settled com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra referral.schema.json#/$defs/CommissionLedgerEntry.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "commission_settled",
      "expectedValid": true
    },
    {
      "id": "P16",
      "given": "Fixture sintética purchase_trial com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra entitlement.schema.json#/$defs/PurchaseEvent.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "purchase_trial",
      "expectedValid": true
    },
    {
      "id": "P17",
      "given": "Fixture sintética purchase_paid com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra entitlement.schema.json#/$defs/PurchaseEvent.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "purchase_paid",
      "expectedValid": true
    },
    {
      "id": "P18",
      "given": "Fixture sintética analytics_core com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra entitlement.schema.json#/$defs/AnalyticsEnvelope.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "analytics_core",
      "expectedValid": true
    },
    {
      "id": "P19",
      "given": "Fixture sintética analytics_financial com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra entitlement.schema.json#/$defs/AnalyticsEnvelope.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "analytics_financial",
      "expectedValid": true
    },
    {
      "id": "P20",
      "given": "Fixture sintética api_purchase com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra api.openapi.yaml#/components/schemas/PurchaseRequest.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "api_purchase",
      "expectedValid": true
    },
    {
      "id": "P21",
      "given": "Fixture sintética api_redeem com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra api.openapi.yaml#/components/schemas/RedeemRequest.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "api_redeem",
      "expectedValid": true
    },
    {
      "id": "P22",
      "given": "Fixture sintética api_attribution com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra api.openapi.yaml#/components/schemas/AttributionClaimRequest.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "api_attribution",
      "expectedValid": true
    },
    {
      "id": "P23",
      "given": "Fixture sintética api_verified com autoridade/fonte declarada no tipo.",
      "when": "Validar a estrutura contra api.openapi.yaml#/paths/~1v1~1billing~1purchases/post/responses/200/content/application~1json/schema.",
      "then": "Aceitar a estrutura; não autoriza operação em produção.",
      "baseFixture": "api_verified",
      "expectedValid": true
    },
    {
      "id": "N01",
      "given": "Fixture sintética snapshot_legacy.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar grant confirmado com legacyCore falso.",
      "baseFixture": "snapshot_legacy",
      "set": {
        "/legacyCore": false
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N02",
      "given": "Fixture sintética snapshot_legacy.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar feature Pro disponível com proAllowed falso.",
      "baseFixture": "snapshot_legacy",
      "set": {
        "/proFeatureSet": [
          "PRO_INSIGHTS_WEEKLY"
        ]
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N03",
      "given": "Fixture sintética purchase_trial.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar trial classificado como cobrança paga.",
      "baseFixture": "purchase_trial",
      "set": {
        "/paymentStatus": "PAID"
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N04",
      "given": "Fixture sintética snapshot_trial.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar estado UI usado como SubscriptionState.",
      "baseFixture": "snapshot_trial",
      "set": {
        "/play/state": "TRIAL_ACTIVE"
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N05",
      "given": "Fixture sintética legacy_claim.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar alias antigo como estado armazenado.",
      "baseFixture": "legacy_claim",
      "set": {
        "/state": "PENDING"
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N06",
      "given": "Fixture sintética analytics_financial.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar alias como segundo evento canônico.",
      "baseFixture": "analytics_financial",
      "set": {
        "/name": "paid_first_verified"
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N07",
      "given": "Fixture sintética analytics_financial.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar cliente emitindo first_charge_verified.",
      "baseFixture": "analytics_financial",
      "set": {
        "/emitter": "CLIENT"
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N08",
      "given": "Fixture sintética analytics_core.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar identificador pessoal nos params.",
      "baseFixture": "analytics_core",
      "set": {
        "/params/uid": "fixture_uid"
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N09",
      "given": "Fixture sintética reward_earned.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar terceiro marco como incremento de 30; correto é 23.",
      "baseFixture": "reward_earned",
      "set": {
        "/milestone": 3,
        "/targetDays": 30,
        "/deltaDays": 30
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N10",
      "given": "Fixture sintética reward_applied.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar APPLIED sem delta aplicado.",
      "baseFixture": "reward_applied",
      "set": {
        "/appliedDays": 0
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N11",
      "given": "Fixture sintética reward_needs_reconciliation.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar efeito incerto sem operação identificada.",
      "baseFixture": "reward_needs_reconciliation",
      "set": {
        "/operationId": null
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N12",
      "given": "Fixture sintética commission_settled.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar R$3 na primeira renovação; correto é R$1.",
      "baseFixture": "commission_settled",
      "set": {
        "/amountMinor": 300
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N13",
      "given": "Fixture sintética api_purchase.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar UID livre no request público de compra.",
      "baseFixture": "api_purchase",
      "set": {
        "/uid": "fixture_other_uid"
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N14",
      "given": "Fixture sintética api_redeem.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar cliente escolhendo dias de resgate.",
      "baseFixture": "api_redeem",
      "set": {
        "/deltaDays": 60
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N15",
      "given": "Fixture sintética api_attribution.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar cliente escolhendo origem real.",
      "baseFixture": "api_attribution",
      "set": {
        "/sourceId": "fixture_hijack"
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N16",
      "given": "Fixture sintética legacy_grant.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar timestamp não normalizado UTC Z.",
      "baseFixture": "legacy_grant",
      "set": {
        "/legacyGrantedAt": "2026-10-09T09:00:00-03:00"
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N17",
      "given": "Fixture sintética paused_summary.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar total 97 dias.",
      "baseFixture": "paused_summary",
      "set": {
        "/earnedDays": 97,
        "/appliedDays": 97
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N18",
      "given": "Fixture sintética snapshot_trial.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar ACTIVE confirmado sem validade.",
      "baseFixture": "snapshot_trial",
      "set": {
        "/play/validUntil": null
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N19",
      "given": "Fixture sintética reward_applied.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar defer aplicado sem etag da leitura prévia.",
      "baseFixture": "reward_applied",
      "set": {
        "/etag": null
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N20",
      "given": "Fixture sintética reward_view.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar confirmação APPLIED sem data efetiva.",
      "baseFixture": "reward_view",
      "set": {
        "/validUntil": null
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N21",
      "given": "Fixture sintética commission_settled.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar SETTLED sem prova temporal de liquidação.",
      "baseFixture": "commission_settled",
      "set": {
        "/settledAt": null
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N22",
      "given": "Fixture sintética analytics_core.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar mensagem livre como reason técnico.",
      "baseFixture": "analytics_core",
      "set": {
        "/params/reason": "endereço do motorista"
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N23",
      "given": "Fixture sintética api_verified.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar HTTP 200 de compra como verificação pendente.",
      "baseFixture": "api_verified",
      "set": {
        "/state": "VERIFYING",
        "/entitlementRevision": null
      },
      "remove": [],
      "expectedValid": false
    },
    {
      "id": "N24",
      "given": "Fixture sintética attribution_locked.",
      "when": "Substituir/remover os campos listados e validar.",
      "then": "Rejeitar LOCKED sem timestamp autoritativo.",
      "baseFixture": "attribution_locked",
      "set": {},
      "remove": [
        "/lockedAt"
      ],
      "expectedValid": false
    },
    {
      "id": "P24",
      "given": "Grant admin confirmado em dados sintéticos.",
      "when": "Validar resposta HTTP 200 administrativa.",
      "then": "Aceitar somente GRANTED com resolução confirmada.",
      "baseFixture": "api_admin_result",
      "expectedValid": true
    },
    {
      "id": "N25",
      "given": "Resposta admin ainda sob revisão.",
      "when": "Retornar HTTP 200 de grant com UNDER_REVIEW.",
      "then": "Rejeitar grant declarado concluído sem estado GRANTED.",
      "baseFixture": "api_admin_result",
      "set": {
        "/state": "UNDER_REVIEW",
        "/resolvedAt": null
      },
      "expectedValid": false
    }
  ],
  "semanticCases": [
    {
      "id": "G01",
      "rule": "access",
      "given": "Snapshot sintético snapshot_legacy e serverNow da fixture.",
      "when": "Aplicar alterações e a união INV-03 no instante autoritativo.",
      "then": "Core permanente não depende de Play/Pro.",
      "snapshotFixture": "snapshot_legacy",
      "set": {},
      "expected": {
        "coreAllowed": true,
        "proAllowed": false
      }
    },
    {
      "id": "G02",
      "rule": "access",
      "given": "Snapshot sintético snapshot_trial e serverNow da fixture.",
      "when": "Aplicar alterações e a união INV-03 no instante autoritativo.",
      "then": "Trial confirmado libera direito mas não qualifica pagamento.",
      "snapshotFixture": "snapshot_trial",
      "set": {},
      "expected": {
        "coreAllowed": true,
        "proAllowed": true
      }
    },
    {
      "id": "G03",
      "rule": "access",
      "given": "Snapshot sintético snapshot_trial e serverNow da fixture.",
      "when": "Aplicar alterações e a união INV-03 no instante autoritativo.",
      "then": "Cancelamento conserva período vigente.",
      "snapshotFixture": "snapshot_trial",
      "set": {
        "/play/state": "CANCELED_WITH_ACCESS"
      },
      "expected": {
        "coreAllowed": true,
        "proAllowed": true
      }
    },
    {
      "id": "G04",
      "rule": "access",
      "given": "Snapshot sintético snapshot_trial e serverNow da fixture.",
      "when": "Aplicar alterações e a união INV-03 no instante autoritativo.",
      "then": "Hold não mantém fonte paga, salvo independente.",
      "snapshotFixture": "snapshot_trial",
      "set": {
        "/play/state": "ON_HOLD",
        "/coreAllowed": false,
        "/proAllowed": false
      },
      "expected": {
        "coreAllowed": false,
        "proAllowed": false
      }
    },
    {
      "id": "G05",
      "rule": "access",
      "given": "Snapshot sintético snapshot_trial e serverNow da fixture.",
      "when": "Aplicar alterações e a união INV-03 no instante autoritativo.",
      "then": "Fim do período é exclusivo; igual expiry não está válido.",
      "snapshotFixture": "snapshot_trial",
      "set": {
        "/serverNow": "2026-10-16T12:00:00Z",
        "/coreAllowed": false,
        "/proAllowed": false
      },
      "expected": {
        "coreAllowed": false,
        "proAllowed": false
      }
    },
    {
      "id": "G06",
      "rule": "access",
      "given": "Snapshot sintético snapshot_review e serverNow da fixture.",
      "when": "Aplicar alterações e a união INV-03 no instante autoritativo.",
      "then": "Review não vira booleano perpétuo.",
      "snapshotFixture": "snapshot_review",
      "set": {
        "/review/provisionalUntil": "2026-10-08T12:00:00Z",
        "/coreAllowed": false
      },
      "expected": {
        "coreAllowed": false,
        "proAllowed": false
      }
    },
    {
      "id": "G07",
      "rule": "access",
      "given": "Snapshot sintético snapshot_blocked e serverNow da fixture.",
      "when": "Aplicar alterações e a união INV-03 no instante autoritativo.",
      "then": "Flag cobrança OFF conserva Core na ponte, sem Pro fictício.",
      "snapshotFixture": "snapshot_blocked",
      "set": {
        "/subscriptionRequired": false,
        "/coreAllowed": true
      },
      "expected": {
        "coreAllowed": true,
        "proAllowed": false
      }
    },
    {
      "id": "G08",
      "rule": "milestones",
      "given": "Um indicador/campanha com qualificados únicos chegando/repetindo.",
      "when": "Processar os counts em ordem, reservando marcos uma vez.",
      "then": "Incrementos 7/23/30 e teto 60, sem reinício por consumo/replay/6º.",
      "qualifiedCounts": [
        1,
        2,
        3,
        4,
        5,
        6,
        5
      ],
      "expectedDeltas": [
        7,
        0,
        23,
        0,
        30,
        0,
        0
      ],
      "expectedEarnedDays": 60
    },
    {
      "id": "G09",
      "rule": "qualification",
      "given": "Trial ACTIVE sem cobrança.",
      "when": "Tentar qualificar indicado.",
      "then": "Não qualifica trial.",
      "input": {
        "newDistinct": true,
        "identityRegularized": true,
        "sameIdentity": false,
        "sourceType": "DRIVER",
        "paymentVerified": true,
        "paidProof": false,
        "qualificationWindowPassed": true,
        "voided": false
      },
      "expectedQualified": false
    },
    {
      "id": "G10",
      "rule": "qualification",
      "given": "Primeira cobrança verificada com janela ainda não concluída.",
      "when": "Processar qualificação.",
      "then": "Mantém QUALIFICATION_PENDING.",
      "input": {
        "newDistinct": true,
        "identityRegularized": true,
        "sameIdentity": false,
        "sourceType": "DRIVER",
        "paymentVerified": true,
        "paidProof": true,
        "qualificationWindowPassed": false,
        "voided": false
      },
      "expectedQualified": false
    },
    {
      "id": "G11",
      "rule": "qualification",
      "given": "Cobrança com void antes da qualificação.",
      "when": "Processar após janela.",
      "then": "Desqualifica, sem prêmio.",
      "input": {
        "newDistinct": true,
        "identityRegularized": true,
        "sameIdentity": false,
        "sourceType": "DRIVER",
        "paymentVerified": true,
        "paidProof": true,
        "qualificationWindowPassed": true,
        "voided": true
      },
      "expectedQualified": false
    },
    {
      "id": "G12",
      "rule": "qualification",
      "given": "Mesmo titular indicador/indicado.",
      "when": "Processar source.",
      "then": "Rejeita self-referral.",
      "input": {
        "newDistinct": true,
        "identityRegularized": true,
        "sameIdentity": true,
        "sourceType": "DRIVER",
        "paymentVerified": true,
        "paidProof": true,
        "qualificationWindowPassed": true,
        "voided": false
      },
      "expectedQualified": false
    },
    {
      "id": "G13",
      "rule": "qualification",
      "given": "Indicado distinto, regularizado, prova paga e janela válidas sem void.",
      "when": "Qualificar uma vez.",
      "then": "QUALIFIED somente por fonte backend.",
      "input": {
        "newDistinct": true,
        "identityRegularized": true,
        "sameIdentity": false,
        "sourceType": "DRIVER",
        "paymentVerified": true,
        "paidProof": true,
        "qualificationWindowPassed": true,
        "voided": false
      },
      "expectedQualified": true
    },
    {
      "id": "G14",
      "rule": "channel",
      "given": "Origem CREATOR já travada.",
      "when": "Decidir destino remunerado.",
      "then": "Comissão somente; não criar reward DRIVER.",
      "sourceType": "CREATOR",
      "expectedReward": false,
      "expectedCommission": true
    },
    {
      "id": "G15",
      "rule": "channel",
      "given": "Origem DRIVER já travada.",
      "when": "Decidir destino remunerado.",
      "then": "Reward DRIVER somente; não criar comissão.",
      "sourceType": "DRIVER",
      "expectedReward": true,
      "expectedCommission": false
    },
    {
      "id": "G16",
      "rule": "defer_timeout",
      "given": "Operação foi enviada, resposta perdeu-se.",
      "when": "Conciliar sem prova ainda do efeito.",
      "then": "NEEDS_RECONCILIATION e zero novo envio, não APPLIED.",
      "trace": [
        "RESERVED",
        "SENT",
        "TIMEOUT"
      ],
      "expectedState": "NEEDS_RECONCILIATION",
      "expectedNewSend": false,
      "expectedAppliedDays": 0
    },
    {
      "id": "G17",
      "rule": "commission",
      "given": "Mesmo creator/indicado recebe primeira cobrança e primeira renovação reais, com repetições.",
      "when": "Processar identidades econômicas/etapas idempotentes.",
      "then": "Comissão total 400 centavos, não 800.",
      "events": [
        {
          "stage": "FIRST_CHARGE",
          "economicKey": "fixture_sale_1",
          "paid": true,
          "voided": false
        },
        {
          "stage": "FIRST_CHARGE",
          "economicKey": "fixture_sale_1",
          "paid": true,
          "voided": false
        },
        {
          "stage": "FIRST_RENEWAL",
          "economicKey": "fixture_sale_2",
          "paid": true,
          "voided": false
        },
        {
          "stage": "FIRST_RENEWAL",
          "economicKey": "fixture_sale_2",
          "paid": true,
          "voided": false
        }
      ],
      "expectedAmountMinor": 400
    },
    {
      "id": "G18",
      "rule": "core_dedupe",
      "given": "Frames/replay/timezone alterado do mesmo resultado e nova oferta com valores iguais.",
      "when": "Usar ID de conclusão, preservando a data original; não fingerprint de preço.",
      "then": "Duas análises/dias; replay não cria terceira data.",
      "receipts": [
        {
          "identityKey": "fixture_uid",
          "analysisId": "fixture_a",
          "localDate": "2026-10-08",
          "kind": "REAL"
        },
        {
          "identityKey": "fixture_uid",
          "analysisId": "fixture_a",
          "localDate": "2026-10-08",
          "kind": "REAL"
        },
        {
          "identityKey": "fixture_uid",
          "analysisId": "fixture_b",
          "localDate": "2026-10-09",
          "kind": "REAL"
        },
        {
          "identityKey": "fixture_uid",
          "analysisId": "fixture_a",
          "localDate": "2026-10-10",
          "kind": "REAL"
        },
        {
          "identityKey": "fixture_uid",
          "analysisId": "fixture_demo",
          "localDate": "2026-10-11",
          "kind": "DEMO"
        }
      ],
      "aliases": {},
      "expectedAnalyses": 2,
      "expectedDays": 2,
      "expectedFirstCoreValues": 1
    },
    {
      "id": "G19",
      "rule": "core_dedupe",
      "given": "Mapa de linking legítimo fornecido pelo Auth (sintético), com mesmos IDs/datas copiados.",
      "when": "Unir dados pela identidade canônica sem decidir ownership.",
      "then": "Uma análise, um dia e um primeiro valor.",
      "receipts": [
        {
          "identityKey": "fixture_anon",
          "analysisId": "fixture_a",
          "localDate": "2026-10-09",
          "kind": "REAL"
        },
        {
          "identityKey": "fixture_linked",
          "analysisId": "fixture_a",
          "localDate": "2026-10-09",
          "kind": "REAL"
        }
      ],
      "aliases": {
        "fixture_anon": "fixture_linked"
      },
      "expectedAnalyses": 1,
      "expectedDays": 1,
      "expectedFirstCoreValues": 1
    },
    {
      "id": "G20",
      "rule": "defer_effect",
      "given": "Defer confirmado da fixture aplicada.",
      "when": "Comparar preExpiry/delta com expiry real observado.",
      "then": "Uma extensão de sete dias, aplicada uma vez.",
      "ledgerFixture": "reward_applied",
      "expectedDeltaSeconds": 604800
    },
    {
      "id": "G21",
      "rule": "rollback",
      "given": "Campanha pausada com sete dias aplicados e 23 conquistados.",
      "when": "Desligar novas concessões mantendo histórico.",
      "then": "earnedDays=30/appliedDays=7 e crédito EARNED visíveis, jobs continuam.",
      "summaryFixture": "paused_summary",
      "expectedEarnedDays": 30,
      "expectedAppliedDays": 7,
      "expectedState": "REWARDS_PAUSED"
    },
    {
      "id": "G22",
      "rule": "access",
      "given": "Promo com startAt futuro confirmado pelo servidor (somente fixture).",
      "when": "Avaliar o intervalo de acesso antes/no instante de início.",
      "then": "Antes de startAt a concessão não libera acesso.",
      "snapshotFixture": "snapshot_blocked",
      "set": {
        "/serverNow": "2026-10-09T12:00:00Z",
        "/promo/state": "ACTIVE",
        "/promo/startAt": "2026-10-10T12:00:00Z",
        "/promo/validUntil": "2026-10-17T12:00:00Z",
        "/coreAllowed": false,
        "/proAllowed": false
      },
      "expected": {
        "coreAllowed": false,
        "proAllowed": false
      }
    },
    {
      "id": "G23",
      "rule": "access",
      "given": "Promo com startAt futuro confirmado pelo servidor (somente fixture).",
      "when": "Avaliar o intervalo de acesso antes/no instante de início.",
      "then": "O início confirmado libera o direito; não cria cobrança.",
      "snapshotFixture": "snapshot_blocked",
      "set": {
        "/serverNow": "2026-10-10T12:00:00Z",
        "/promo/state": "ACTIVE",
        "/promo/startAt": "2026-10-10T12:00:00Z",
        "/promo/validUntil": "2026-10-17T12:00:00Z",
        "/coreAllowed": true,
        "/proAllowed": true
      },
      "expected": {
        "coreAllowed": true,
        "proAllowed": true
      }
    }
  ]
}
```

## Given/When/Then de integração futura (não executados em S01)

| Contrato / requisito | Given / When / Then | Story/evidência responsável |
|---|---|---|
| INV-01/02; ENT-001/002/004 | Legado GRANTED, reinstalação/troca de aparelho/linking legítimo → Core restaurado; late-claim plausível → revisão/provisório com limite, não bloqueio abrupto. | S04–S09/S16; OQ-01/02/AR-01 |
| INV-03/04; ENT-003, NFR-002/005/009 | Rede perdida durante oferta → nenhuma chamada growth/billing/parser; grant durável/cache legítimo protegidos; expiry restritivo só em entrada segura, tolerância finita aprovada. | S08/S12/S25/S27; OQ-03/10 |
| INV-05; BILL-001/002/003/007, CORE-001 | Setup incompleto/demo → nenhum trial; Play oferta válida após readiness e escolha → preço/período/disclosure reais; sem oferta → paid-only, sem 7d fictícios. | S17/S18/S24; Console OQ-05/S03, screenshot de referência |
| INV-06/07; BILL-004/005/006/008, DATA-003 | Callback/RTDN duplicado/desordenado/perdido → GET Play e estado/revisão coerentes; trial ACK; compra cobrada sem acesso → incidente P0; mensal↔trimestral/recovery conserva titular. | S19–S27/S21; testes Play/ordens oficiais, nenhum endpoint presumido |
| INV-08; ATTR-001/002, REF-001 | App/no-app/Install Referrer → primeira fonte válida locked; código tardio/self-referral → rejeita/hub informativo; Sharesheet por gesto → evento de abertura, não envio comprovado. | S28–S33; App Signing/domínio AR-01 |
| INV-09/10; REF-002/003 | Trial/void antes da janela → não qualified; cinco pagantes distintos, evento repetido/6º → 60 totais e deltas 7/23/30; mesma aquisição não remunera canais duplos. | S39/S40/S44; janela/antifraude OQ-06 |
| INV-11; REF-004, NFR-004/006 | Defer enviado/timeout/etag antigo → não reenviar cego, reconciliar/revisar; expiry real confirmado → APPLIED; RTDN deferred não cria marco novo. | S41/S44; API/elegibilidade oficial OQ-05 |
| INV-12; REF-005 | Período pago/recorrente conflitante e defer inelegível → crédito EARNED, não promo com cobrança paralela; sem assinatura → resgate voluntário/datas/sem autorrenovação; legacy sem Insights → rewards OFF; expirar Pro preserva Core. | S38/S42/S43/S44; termos OQ-06/utilidade OQ-11 |
| INV-13; AFF-001/OPS-003 | FirstCharge/firstRenewal reais → R$3/R$1 uma vez; trimestral sem renovação real em M2 → não remunera; payout/lote/refund parcial → auditoria e histórico preservados. | S45–S49; fonte S21/termos OQ-06 |
| INV-14; PRIV-001/NFR-007 | UID/token/contato/OCR enviados por cliente ou escrita financeira SDK/admin sem papel → rejeita; creator consulta → só agregado próprio; exclusão/retention aprovadas por categoria. | S02/S14/S15 e stories financeiras; OQ-07 |
| INV-15; OPS-001/NFR-008 | Flags cobrança/reward OFF → nenhum novo checkout/concessão; grants/compras/promos/créditos/obrigações e RTDN/ACK/reconcile/restore continuam. | S13/S16/S27/S44/S49 |
| INV-16; GROW-001/002/003/NFR-010 | Monitoring/OCR/overlay/oferta recente/condução incerta → silêncio; ≥2 dias seguro, caps 2/30d,14d,2 dismiss→60d, opt-out persistente; review sem recompensa/gating/nota; cartão verdadeiro. | S31/S32/S43/S50/S51; OQ-10/12, UI/device/TalkBack |
| INV-17; DATA-001/002 | Repeated frames/demo/linking/timezone → IDs/dias/firstValue não inflados; D7 exato/coorte nova72h/trial maduro/monthly-quarterly → denominadores/maturação corretos, indisponível ≠0. | S10/S34–S36/S48; AR-02 |
| INV-18; CORE-002/OPS-002, NFR-001/003 | Matriz versões/política/latência/ANR incompleta → HOLD; P0 Core ou violação de plataforma → STOP sem bypass. | S03/S11/S12/S27; baseline OQ-09 |
| ASO-001 | Uma screenshot/hipótese ≥7d, UI real → aquisição/ativação interpretável, sem preço simultâneo/ganhos garantidos ou downloads tratados como retenção. | S52; sem implementação na S01 |

## Limites de prontidão

OQ-08 é resolvida pelos contratos. OQ-01–07/OQ-09–12 e AR-01/02 continuam abertas. Nenhum teste sintético substitui prove of firstPaid, política de cutoff/recovery, tolerância offline, janela void/resgate, regra de plan change ou benefício Pro validado. Nenhum serviço, feature flag ou story posterior é habilitado por este relatório.

## Resultado da execução S01 — 2026-10-09

| Verificação executada | Resultado |
|---|---|
| JSON Schema 2020-12, duas raízes e 34 definições | PASS, metaschemas válidos. |
| OpenAPI 3.1.0, 13 paths/operações e referências locais | PASS, base URI do arquivo explicitamente resolvida. |
| Fixtures de schema/HTTP | **49/49 PASS**: 24 positivas aceitas, 25 negativas rejeitadas pelos motivos contratuais. |
| Oráculos normativos semânticos | **23/23 PASS**: fontes/expiry/início futuro de promo, 7/23/30 totais, trial/void/self-referral/janela, canal único, defer incerto/confirmado, comissão idempotente, dedupe/linking e rollback. |
| Coerência de enums/eventos/segurança | PASS: 30 eventos/emissores; reward/subscription enums coerentes; rota canônica única; Firebase público versus identidade de serviço internal; RBAC admin/creator; request público sem UID/dias escolhidos. |
| Rastreabilidade | PASS: 33 requisitos funcionais e 10 não funcionais referenciados no README, com invariantes/stories/bloqueios. Não significa requisitos de produto implementados. |
| Documentação | PASS: IDs de casos/operações únicos, fences/whitespace e links locais válidos. |

Ferramentas utilizadas: `jsonschema.Draft202012Validator` + `FormatChecker`/registro local `referencing.Registry`; `OpenAPIV31SpecValidator(document, base_uri=file_uri).validate()`; leitura do bloco JSON e avaliação dos oráculos em processo temporário. Validadores ficaram em `.tmp/s01-validation/`, fora das dependências do aplicativo. Não foi criado framework/test runner no código de produção.

Não executados (pertencem a outras stories): build/testes Android, screenshot de UI implementada, Auth/Firestore/Console/Play/RTDN reais, prova financeira, deployment e flags. S01 encerrada após validação documental; S02–S52 não iniciadas.

