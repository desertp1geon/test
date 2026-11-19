FROM quay.io/keycloak/keycloak:26.8.1

WORKDIR /opt/keycloak

# Shared dependencies
COPY keycloak-shared/target/keycloak-shared-1.0.0-SNAPSHOT.jar \
     /opt/keycloak/providers/

COPY keycloak-broker-common/target/keycloak-broker-common-1.0.0-SNAPSHOT.jar \
     /opt/keycloak/providers/

# Identity Providers
COPY keycloak-diia-provider/target/keycloak-diia-provider-1.0.0-SNAPSHOT.jar \
     /opt/keycloak/providers/

COPY keycloak-bankid-provider/target/keycloak-bankid-provider-1.0.0-SNAPSHOT.jar \
     /opt/keycloak/providers/

# Identity Resolution
COPY keycloak-identity-resolver/target/keycloak-identity-resolver-1.0.0-SNAPSHOT.jar \
     /opt/keycloak/providers/

# Build optimized Keycloak
RUN /opt/keycloak/bin/kc.sh build
