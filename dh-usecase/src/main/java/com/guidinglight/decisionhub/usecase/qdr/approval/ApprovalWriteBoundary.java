package com.guidinglight.decisionhub.usecase.qdr.approval;

import java.util.Objects;

/**
 * Human Approval Packet 写入边界。
 *
 * <p>usecase 层只声明“create / decision submit 必须在同一个 fail-closed 写入边界内完成”，不直接依赖
 * Spring transaction、JDBC 或 JPA。应用层可以用本端口接入真实事务；单元测试和无事务场景使用直通实现。
 */
@FunctionalInterface
public interface ApprovalWriteBoundary {

    /**
     * 执行一段 approval 写操作。
     *
     * @param action 包含 repository write 与 audit write 的操作。
     * @param <T> 返回值类型。
     * @return action 的返回值。
     */
    <T> T execute(ApprovalWriteAction<T> action);

    /**
     * 写入边界内实际执行的 action。
     *
     * @param <T> 返回值类型。
     */
    @FunctionalInterface
    interface ApprovalWriteAction<T> {

        /**
         * 执行写入逻辑。
         *
         * @return 写入结果。
         */
        T get();
    }

    /**
     * 直通写入边界，用于纯单元测试或没有外部事务能力的场景。
     *
     * @return 不额外包裹事务的写入边界。
     */
    static ApprovalWriteBoundary direct() {
        return new ApprovalWriteBoundary() {
            @Override
            public <T> T execute(final ApprovalWriteAction<T> action) {
                return Objects.requireNonNull(action, "action").get();
            }
        };
    }
}
