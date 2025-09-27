FROM quay.io/wildfly/wildfly:latest

# WAR als ROOT.war deployen
COPY target/llmbench-jakarta.war /opt/jboss/wildfly/standalone/deployments/ROOT.war
