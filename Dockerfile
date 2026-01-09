FROM maven:3.9.6-eclipse-temurin-11 AS build
WORKDIR /workspace

# 把整个仓库复制进来（ACR 唯一稳妥做法）
COPY . .

# 用父工程视角构建 order
RUN mvn -B -q -DskipTests \
    -pl microservice-kubernetes-demo/microservice-kubernetes-demo-order \
    -am package

FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build \
  /workspace/microservice-kubernetes-demo/microservice-kubernetes-demo-order/target/*.jar \
  /app/app.jar

EXPOSE 8080
CMD ["java","-Xms256m","-Xmx512m","-jar","/app/app.jar"]
