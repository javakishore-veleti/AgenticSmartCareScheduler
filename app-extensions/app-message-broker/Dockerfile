FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Copy parent pom and broker module pom
COPY pom.xml .
COPY app-common/pom.xml app-common/
COPY app-dao/pom.xml app-dao/
COPY app-core/pom.xml app-core/
COPY app-domain-admin/pom.xml app-domain-admin/
COPY app-domain-customer/pom.xml app-domain-customer/
COPY app-wfs/pom.xml app-wfs/
COPY app-cloud-integration/pom.xml app-cloud-integration/
COPY app-cloud-integration/app-cloud-aws/pom.xml app-cloud-integration/app-cloud-aws/
COPY app-cloud-integration/app-cloud-aws/app-cloud-aws-wfs/pom.xml app-cloud-integration/app-cloud-aws/app-cloud-aws-wfs/
COPY app-cloud-integration/app-cloud-aws/app-cloud-aws-ai/pom.xml app-cloud-integration/app-cloud-aws/app-cloud-aws-ai/
COPY app-cloud-integration/app-cloud-aws/app-cloud-aws-bedrock/pom.xml app-cloud-integration/app-cloud-aws/app-cloud-aws-bedrock/
COPY app-cloud-integration/app-cloud-aws/app-cloud-aws-ml/pom.xml app-cloud-integration/app-cloud-aws/app-cloud-aws-ml/
COPY app-cloud-integration/app-cloud-aws/app-cloud-aws-agents/pom.xml app-cloud-integration/app-cloud-aws/app-cloud-aws-agents/
COPY app-agents/pom.xml app-agents/
COPY app-agents/app-agent-patient/pom.xml app-agents/app-agent-patient/
COPY app-agents/app-agent-communication/pom.xml app-agents/app-agent-communication/
COPY app-agents/app-agent-provider/pom.xml app-agents/app-agent-provider/
COPY app-agents/app-agent-resource/pom.xml app-agents/app-agent-resource/
COPY app-agents/app-agent-audit/pom.xml app-agents/app-agent-audit/
COPY app-agents/app-agent-workflow-updates/pom.xml app-agents/app-agent-workflow-updates/
COPY app-extensions/pom.xml app-extensions/
COPY app-extensions/app-message-broker/pom.xml app-extensions/app-message-broker/
COPY app-web/pom.xml app-web/

RUN mvn dependency:go-offline -B 2>/dev/null || true

COPY . .
RUN mvn clean package -pl app-extensions/app-message-broker -am -DskipTests -B

# Runtime
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S smartcare && adduser -S smartcare -G smartcare
WORKDIR /app
COPY --from=builder /build/app-extensions/app-message-broker/target/app-message-broker-*.jar app.jar
USER smartcare
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s CMD wget -qO- http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
