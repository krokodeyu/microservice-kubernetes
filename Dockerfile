FROM maven:3.9.6-eclipse-temurin-11 AS build
WORKDIR /workspace
COPY . .

# 1) 先尝试 reactor 在 microservice-kubernetes-demo/pom.xml 的情况
# 2) 失败则尝试 reactor 在仓库根 pom.xml 的情况
RUN (mvn -B -q -DskipTests -f microservice-kubernetes-demo/pom.xml -pl microservice-kubernetes-demo-order -am package) \
 || (mvn -B -q -DskipTests -f pom.xml -pl microservice-kubernetes-demo-order -am package)

# 把 jar 放到固定位置，runtime 阶段不再猜路径
RUN (cp microservice-kubernetes-demo/microservice-kubernetes-demo-order/target/*.jar /workspace/app.jar) \
 || (cp microservice-kubernetes-demo-order/target/*.jar /workspace/app.jar)

FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar
EXPOSE 8080
CMD ["java","-Xms256m","-Xmx512m","-jar","/app/app.jar"]
