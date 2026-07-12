package com.guidinglight.decisionhub.usecase.qdr.guard;

/** PostgreSQL multi-instance fixed-window admission能力端口；不暴露CRUD或tenantless查询。 */
public interface RateLimitAdmissionPort {

  /**
   * 使用DB UTC时间原子创建/递增当前bucket。
   *
   * @param command 完整identity与受限配置。
   * @return accepted、rate limited或独立store错误分类。
   */
  RateLimitAdmissionResult tryAcquire(RateLimitAdmissionCommand command);
}
