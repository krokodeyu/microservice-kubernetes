# ===== 构建阶段 =====
FROM maven:3.9.6-eclipse-temurin-11 AS build
WORKDIR /build

# 加入 Maven 国内源
COPY maven-settings.xml /root/.m2/settings.xml

# 复制当前模块的 pom
COPY pom.xml .

# 先下载依赖（利用缓存）
RUN mvn -B -q -DskipTests dependency:go-offline

# 再复制源码
COPY src src

# 打包
RUN mvn -B -q -DskipTests package

# ===== 运行阶段 =====
FROM eclipse-temurin:11-jre
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
CMD ["java","-Xms256m","-Xmx512m","-jar","/app/app.jar"]
