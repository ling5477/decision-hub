package com.guidinglight.decisionhub.infra.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEventJpaRepository extends JpaRepository<LedgerEventEntity, String> {
  List<LedgerEventEntity> findByRunIdOrderByAtAsc(String runId);
}
