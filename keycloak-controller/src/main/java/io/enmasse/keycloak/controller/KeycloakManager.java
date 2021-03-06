/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.keycloak.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.address.model.EndpointSpec;
import io.enmasse.address.model.EndpointStatus;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


public class KeycloakManager implements Watcher<AddressSpace>
{
    private static final Logger log = LoggerFactory.getLogger(KeycloakManager.class);
    private final KeycloakApi keycloak;
    private final KubeApi kube;

    public KeycloakManager(KeycloakApi keycloak, KubeApi kube) {
        this.keycloak = keycloak;
        this.kube = kube;
    }

    private EndpointSpec getConsoleEndpoint(AddressSpace addressSpace) {
        for (EndpointSpec endpoint : addressSpace.getEndpoints()) {
            if (endpoint.getService().equals("console")) {
                return endpoint;
            }
        }
        return null;
    }

    private EndpointStatus getConsoleEndpointStatus(AddressSpace addressSpace) {
        EndpointSpec spec = getConsoleEndpoint(addressSpace);
        if (spec == null) {
            return null;
        }

        for (EndpointStatus endpoint : addressSpace.getStatus().getEndpointStatuses()) {
            if (endpoint.getName().equals(spec.getName())) {
                return endpoint;
            }
        }
        return null;
    }

    private String getConsoleUri(EndpointStatus endpoint) {
        String uri = null;
        if (endpoint.getHost() != null) {
            uri = "https://" + endpoint.getHost() + "/*";
            log.info("Using {} as redirect URI for enmasse-console", uri);
        }
        return uri;
    }

    @Override
    public void onUpdate(Set<AddressSpace> addressSpaces) {
        Map<String, AddressSpace> standardAuthSvcSpaces =
                addressSpaces.stream()
                             .filter(x -> x.getAuthenticationService().getType() == AuthenticationServiceType.STANDARD && x.getEndpoints() != null)
                             .collect(Collectors.toMap(addressSpace -> Optional.ofNullable(addressSpace.getAnnotation(AnnotationKeys.REALM_NAME)).orElse(addressSpace.getName()), Function.identity()));

        Set<String> realmNames = keycloak.getRealmNames();
        log.info("Actual: {}, Desired: {}", realmNames, standardAuthSvcSpaces.keySet());
        for(String realmName : realmNames) {
            if(standardAuthSvcSpaces.remove(realmName) == null && !"master".equals(realmName)) {
                log.info("Deleting realm {}", realmName);
                keycloak.deleteRealm(realmName);
            }
        }
        for(Map.Entry<String, AddressSpace> entry : standardAuthSvcSpaces.entrySet()) {
            AddressSpace addressSpace = entry.getValue();
            String realmName = entry.getKey();
            log.info("Creating realm {}", realmName);
            String userName = addressSpace.getAnnotation(AnnotationKeys.CREATED_BY);
            String userId = addressSpace.getAnnotation(AnnotationKeys.CREATED_BY_UID);
            if (userId == null || userId.isEmpty()) {
                userId = kube.findUserId(userName);
            }

            EndpointStatus endpointStatus = getConsoleEndpointStatus(addressSpace);
            if (endpointStatus == null) {
                log.info("Address space {} has no endpoints defined", addressSpace.getName());
            } else if (endpointStatus.getHost() == null && endpointStatus.getPort() == 0) {
                log.info("Address space {} console endpoint host not known, waiting", addressSpace.getName());
            } else {
                String consoleUri = getConsoleUri(endpointStatus);
                keycloak.createRealm(realmName, userName, userId, consoleUri);
            }
        }
    }
}
