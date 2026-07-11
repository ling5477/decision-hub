package com.guidinglight.decisionhub.usecase.qdr.replay.deterministic;

/** Deterministic replay comparator 唯一允许输出的冻结 difference taxonomy。 */
public enum ReplayDifferenceType {
  /** Structured context 或 optional lineage 不一致。 */
  CONTEXT_DIFFERENCE,
  /** Policy version 不一致。 */
  POLICY_VERSION_DIFFERENCE,
  /** Prompt version 不一致。 */
  PROMPT_VERSION_DIFFERENCE,
  /** Model version 不一致。 */
  MODEL_VERSION_DIFFERENCE,
  /** Gateway version 不一致。 */
  GATEWAY_VERSION_DIFFERENCE,
  /** Expected summary structured projection 不一致。 */
  EXPECTED_SUMMARY_DIFFERENCE,
  /** Safe evidence refs 不一致。 */
  EVIDENCE_DIFFERENCE,
  /** Domain-separated replay output hash 不一致。 */
  OUTPUT_HASH_DIFFERENCE,
  /** Comparator 缺少 required structured input。 */
  MISSING_REQUIRED_INPUT
}
