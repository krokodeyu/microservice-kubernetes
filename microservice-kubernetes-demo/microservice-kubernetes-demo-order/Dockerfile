# ===== build stage =====
FROM maven:3.9.6-eclipse-temurin-11 AS build
WORKDIR /src

# 先复制 pom，利用缓存
COPY pom.xml .
COPY microservice-kubernetes-demo/pom.xml microservice-kubernetes-demo/pom.xml
COPY microservice-kubernetes-demo/microservice-kubernetes-demo-order/pom.xml microservice-kubernetes-demo/microservice-kubernetes-demo-order/pom.xml

# 下载依赖（可选，但能明显加速）
RUN mvn -q -e -DskipTests -pl microservice-kubernetes-demo/microservice-kubernetes-demo-order -am dependency:go-offline

# 再复制源代码
COPY microservice-kubernetes-demo microservice-kubernetes-demo

# 编译打包 order
RUN mvn -q -DskipTests -pl microservice-kubernetes-demo/microservice-kubernetes-demo-order -am package

# ===== runtime stage =====
FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /src/microservice-kubernetes-demo/microservice-kubernetes-demo-order/target/*.jar app.jar
EXPOSE 8080
CMD ["java","-Xms256m","-Xmx512m","-jar","/app/app.jar"]
