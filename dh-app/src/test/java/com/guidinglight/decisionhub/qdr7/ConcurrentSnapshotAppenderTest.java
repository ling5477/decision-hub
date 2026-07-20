package com.guidinglight.decisionhub.qdr7;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * 验证 test-only 日志采样器在高并发写入和快照期间保持样本完整性。
 */
class ConcurrentSnapshotAppenderTest {

    private static final int STRESS_ROUNDS = 10;
    private static final int WRITER_COUNT = 16;
    private static final int SAMPLES_PER_WRITER = 1_000;
    private static final int EXPECTED_SAMPLES_PER_ROUND = WRITER_COUNT * SAMPLES_PER_WRITER;

    @Test
    void concurrentWritersPreserveExactUniqueSamplesAcrossImmutableSnapshots() throws Exception {
        for (int round = 0; round < STRESS_ROUNDS; round++) {
            verifyConcurrentRound(round);
        }
    }

    /**
     * 每轮使用独立 collector，保证快照和最终样本不会跨 round 污染。
     */
    private void verifyConcurrentRound(final int round) throws Exception {
        final ConcurrentSnapshotAppender<Integer> collector = new ConcurrentSnapshotAppender<>();
        collector.start();
        final ExecutorService executor = Executors.newFixedThreadPool(WRITER_COUNT + 1);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch firstSamplesReady = new CountDownLatch(WRITER_COUNT);
        final CountDownLatch firstSnapshotCaptured = new CountDownLatch(1);
        final CountDownLatch writersFinished = new CountDownLatch(WRITER_COUNT);
        final AtomicInteger concurrentSnapshotCount = new AtomicInteger();
        final List<Future<?>> workers = new ArrayList<>();
        boolean completed = false;
        try {
            for (int writer = 0; writer < WRITER_COUNT; writer++) {
                final int writerIndex = writer;
                workers.add(
                        executor.submit(
                                () -> {
                                    start.await();
                                    final int writerBase =
                                            round * EXPECTED_SAMPLES_PER_ROUND
                                                    + writerIndex * SAMPLES_PER_WRITER;
                                    try {
                                        collector.doAppend(writerBase);
                                        firstSamplesReady.countDown();
                                        firstSnapshotCaptured.await();
                                        for (int sample = 1;
                                             sample < SAMPLES_PER_WRITER;
                                             sample++) {
                                            collector.doAppend(writerBase + sample);
                                        }
                                    } finally {
                                        writersFinished.countDown();
                                    }
                                    return null;
                                }));
            }
            workers.add(
                    executor.submit(
                            () -> {
                                start.await();
                                assertThat(firstSamplesReady.await(10, TimeUnit.SECONDS)).isTrue();
                                try {
                                    assertDistinct(collector.snapshot());
                                    concurrentSnapshotCount.incrementAndGet();
                                } finally {
                                    firstSnapshotCaptured.countDown();
                                }
                                while (!writersFinished.await(1, TimeUnit.MILLISECONDS)) {
                                    assertDistinct(collector.snapshot());
                                    concurrentSnapshotCount.incrementAndGet();
                                }
                                return null;
                            }));

            start.countDown();
            for (final Future<?> worker : workers) {
                worker.get(30, TimeUnit.SECONDS);
            }
            completed = true;
        } finally {
            firstSnapshotCaptured.countDown();
            if (completed) {
                executor.shutdown();
            } else {
                executor.shutdownNow();
            }
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
            collector.stop();
        }

        final int roundStart = round * EXPECTED_SAMPLES_PER_ROUND;
        final int roundEndExclusive = roundStart + EXPECTED_SAMPLES_PER_ROUND;
        final List<Integer> finalSnapshot = collector.snapshot();
        assertThat(concurrentSnapshotCount.get()).isPositive();
        assertThat(finalSnapshot)
                .hasSize(EXPECTED_SAMPLES_PER_ROUND)
                .doesNotHaveDuplicates()
                .allMatch(sample -> sample >= roundStart && sample < roundEndExclusive);
        assertThatThrownBy(() -> finalSnapshot.add(-1))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * 并发写入期间的任一弱一致快照都不得重复返回同一采样值。
     */
    private static void assertDistinct(final List<Integer> snapshot) {
        assertThat(snapshot).doesNotHaveDuplicates();
    }
}
