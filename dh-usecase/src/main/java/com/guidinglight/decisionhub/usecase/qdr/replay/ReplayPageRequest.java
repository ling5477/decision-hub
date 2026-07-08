package com.guidinglight.decisionhub.usecase.qdr.replay;

/**
 * QDR replay repository 分页请求。
 *
 * <p>列表查询必须显式分页，`limit` 最大 100。超过上限直接拒绝，防止无边界读取 regression baseline。
 *
 * @param limit  最大返回行数，范围 1..100。
 * @param offset 起始偏移，必须非负。
 */
public record ReplayPageRequest(int limit, int offset) {

    /**
     * 单页最大行数。
     */
    public static final int MAX_LIMIT = 100;

    /**
     * 校验分页参数。
     */
    public ReplayPageRequest {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        if (limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must not exceed 100");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be non-negative");
        }
    }
}
