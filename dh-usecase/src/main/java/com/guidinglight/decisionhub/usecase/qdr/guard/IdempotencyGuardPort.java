package com.guidinglight.decisionhub.usecase.qdr.guard;

/** Exact admission/query/CAS能力端口；禁止通用CRUD、latest、list-all或tenantless操作。 */
public interface IdempotencyGuardPort {

  /** 原子insert或exact reread并返回冻结duplicate语义。 */
  IdempotencyAdmissionResult admit(IdempotencyAdmissionCommand command);

  /** 按expected state/version/token执行单一合法转换，成功返回新view。 */
  IdempotencyRecordView transition(IdempotencyTransitionCommand command);

  /** tenant-first exact lookup；不存在或store错误必须fail-closed。 */
  IdempotencyRecordView findExact(
      PersistentGuardIdentity identity, String requestId, String requestHash);
}
