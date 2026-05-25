package com.guidinglight.decisionhub.domain.checkpoint;

/**
 * Stage2-PoC-B1：checkpoint 状态。
 *
 * <p>{@link #DRAFT} 尚未写入持久层；{@link #RECORDED} 已落库；{@link #DISCARDED} 因 rollback 或重复触发被弃用。
 */
public enum CheckpointStatus {
  DRAFT,
  RECORDED,
  DISCARDED
}
