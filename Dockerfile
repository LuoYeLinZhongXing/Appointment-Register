# 使用官方OpenJDK 17镜像作为基础镜像
FROM eclipse-temurin:17-jdk-alpine

# 设置维护者信息
LABEL maintainer="落叶林中行"

# 设置工作目录
WORKDIR /app

# 复制Maven构建的jar包到容器中
# 注意：这里假设您会先在本地执行mvn clean package构建
COPY ar-server/target/ar-server-0.0.1-SNAPSHOT.jar app.jar

# 暴露应用端口
EXPOSE 8080

# 设置JVM参数和环境变量
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
ENV SPRING_PROFILES_ACTIVE=docker

# 创建非 root 用户运行应用（安全最佳实践）
RUN addgroup --system appgroup && adduser -S -G appgroup appuser
RUN chown -R appuser:appgroup /app
USER appuser

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar app.jar"]
