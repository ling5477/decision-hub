# decision-hub (skeleton v1.1)

Java 21 + Spring Boot 3.5.x 的“多模型协同决策平台”后端骨架（模块化单体）。

## 骨架验收
编译：
- `mvn -DskipTests clean package`

运行（自动构建依赖模块）：
- `mvn -pl dh-app -am spring-boot:run`

检查：
- `GET http://localhost:8080/_ping` -> success=true
- `GET http://localhost:8080/actuator/health` -> UP
- 响应 Header: `X-Trace-Id` 存在
- 业务异常/参数异常将统一输出 ApiResponse.fail(...)
