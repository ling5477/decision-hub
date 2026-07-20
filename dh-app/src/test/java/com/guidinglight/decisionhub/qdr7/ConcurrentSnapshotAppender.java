package com.guidinglight.decisionhub.qdr7;

import ch.qos.logback.core.AppenderBase;

import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 为并发日志生产者提供 test-only 事件收集，并在读取时返回不可变快照。
 *
 * <p>Hikari 后台线程与 HTTP worker 会同时触发 Logback appender。使用并发队列保留业务并发，避免测试线程遍历 Logback
 * {@code ListAppender} 的 live {@code ArrayList}。
 *
 * @param <E> 日志事件类型
 */
final class ConcurrentSnapshotAppender<E> extends AppenderBase<E> {

    private final Queue<E> events = new ConcurrentLinkedQueue<>();

    /**
     * 接收来自任意 Logback 调用线程的单个事件，不对被测并发路径加全局锁。
     */
    @Override
    protected void append(final E eventObject) {
        events.add(Objects.requireNonNull(eventObject, "eventObject must not be null"));
    }

    /**
     * 创建当前事件的不可变快照；并发写入期间快照为弱一致视图，worker 完成后包含全部事件。
     *
     * @return 不可变事件快照
     */
    List<E> snapshot() {
        return List.copyOf(events);
    }
}
