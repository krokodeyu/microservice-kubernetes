# ===== build stage =====
FROM maven:3.9.6-eclipse-temurin-11 AS build
WORKDIR /workspace

# 先复制最上层 pom（如果仓库根就是多模块父 pom）
COPY pom.xml ./pom.xml

# 再复制 demo 父工程 pom（解决 com.ewolff:microservice-kubernetes-demo 这个 parent）
COPY microservice-kubernetes-demo/pom.xml ./microservice-kubernetes-demo/pom.xml

# 再复制 order 模块 pom
COPY microservice-kubernetes-demo/microservice-kubernetes-demo-order/pom.xml \
     ./microservice-kubernetes-demo/microservice-kubernetes-demo-order/pom.xml

# 先离线拉依赖（此时 parent 已可解析）
RUN mvn -B -q -DskipTests -pl microservice-kubernetes-demo/microservice-kubernetes-demo-order -am dependency:go-offline

# 再复制源码（只复制 demo 目录即可）
COPY microservice-kubernetes-demo ./microservice-kubernetes-demo

# 编译打包 order
RUN mvn -B -q -DskipTests -pl microservice-kubernetes-demo/microservice-kubernetes-demo-order -am package

# ===== runtime stage =====
FROM eclipse-temurin:11-jre
WORKDIR /app

# 拷贝打出来的 jar
COPY --from=build /workspace/microservice-kubernetes-demo/microservice-kubernetes-demo-order/target/*.jar /app/app.jar

EXPOSE 8080
CMD ["java","-Xms256m","-Xmx512m","-jar","/app/app.jar"]
