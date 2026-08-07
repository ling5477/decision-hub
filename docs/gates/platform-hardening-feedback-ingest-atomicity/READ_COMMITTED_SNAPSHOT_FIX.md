# READ COMMITTED Snapshot Fix

The failed implementation read envelope and event state using two SQL statements. Under PostgreSQL READ COMMITTED, each statement could observe a different committed snapshot, allowing a concurrent winner to appear as a false `EVENT_ONLY`.

The published fix uses one `JdbcTemplate.query` invocation containing one SQL statement. CTEs count and project envelope plus exact-event state inside one PostgreSQL statement snapshot. `ON CONFLICT DO NOTHING` and committed-winner readback remain, but cross-statement snapshot skew is eliminated. True orphan and ambiguity states still fail closed.
