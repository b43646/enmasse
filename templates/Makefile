include ../Makefile.common

ADDRESS_SPACE_CONTROLLER_IMAGE ?= "address-space-controller"
API_SERVER_IMAGE ?= "api-server"
STANDARD_CONTROLLER_IMAGE ?= "standard-controller"
ROUTER_IMAGE ?= "router"
ARTEMIS_IMAGE ?= "artemis"
TOPIC_FORWARDER_IMAGE ?= "topic-forwarder"
ROUTER_METRICS_IMAGE ?= "router-metrics"
AGENT_IMAGE ?= "agent"
MQTT_GATEWAY_IMAGE ?= "mqtt-gateway"
MQTT_LWT_IMAGE ?= "mqtt-lwt"
NONE_AUTHSERVICE_IMAGE ?= "none-authservice"
KEYCLOAK_IMAGE ?= "jboss/keycloak-openshift:3.4.3.Final"
KEYCLOAK_PLUGIN_IMAGE ?= "keycloak-plugin"
KEYCLOAK_CONTROLLER_IMAGE ?= "keycloak-controller"
SERVICE_BROKER_IMAGE ?= "service-broker"

SRCS=$(wildcard *.jsonnet)
OBJS=$(patsubst %.jsonnet,%.json,$(SRCS))
INSTALLDIR=enmasse-$(TAG)

%.json: %.jsonnet
	jsonnet/jsonnet -V VERSION=$(COMMIT) \
		-V REGISTRY=$(DOCKER_REGISTRY) \
		-V PROJECT=$(DOCKER_ORG) \
		-V ADDRESS_SPACE_CONTROLLER_IMAGE=$(ADDRESS_SPACE_CONTROLLER_IMAGE) \
		-V API_SERVER_IMAGE=$(API_SERVER_IMAGE) \
		-V STANDARD_CONTROLLER_IMAGE=$(STANDARD_CONTROLLER_IMAGE) \
		-V ROUTER_IMAGE=$(ROUTER_IMAGE) \
		-V ARTEMIS_IMAGE=$(ARTEMIS_IMAGE) \
		-V TOPIC_FORWARDER_IMAGE=$(TOPIC_FORWARDER_IMAGE) \
		-V ROUTER_METRICS_IMAGE=$(ROUTER_METRICS_IMAGE) \
		-V SERVICE_BROKER_IMAGE=$(SERVICE_BROKER_IMAGE) \
		-V AGENT_IMAGE=$(AGENT_IMAGE) \
		-V MQTT_GATEWAY_IMAGE=$(MQTT_GATEWAY_IMAGE) \
		-V MQTT_LWT_IMAGE=$(MQTT_LWT_IMAGE) \
		-V NONE_AUTHSERVICE_IMAGE=$(NONE_AUTHSERVICE_IMAGE) \
		-V KEYCLOAK_IMAGE=$(KEYCLOAK_IMAGE) \
		-V KEYCLOAK_PLUGIN_IMAGE=$(KEYCLOAK_PLUGIN_IMAGE) \
		-V KEYCLOAK_CONTROLLER_IMAGE=$(KEYCLOAK_CONTROLLER_IMAGE) \
		-m build $<

yaml:
	for i in `find build/ -name "*.json"`; do b=`dirname $$i`; o="install/$${b#build/}"; mkdir -p $$o; ./scripts/convertyaml.py $$i $$o; done

prepare:
	if [ ! -f jsonnet ]; then $(MAKE) -C jsonnet; fi
	mkdir -p build/resources/address-space-controller
	mkdir -p build/resources/api-server
	mkdir -p build/resources/service-broker
	mkdir -p build/resources/prometheus
	mkdir -p build/resources/grafana
	mkdir -p build/resources/standard-authservice
	mkdir -p build/resources/none-authservice
	mkdir -p build/resources/external-lb
	mkdir -p build/resources/plans
	mkdir -p build/resources/resource-definitions
	mkdir -p build/resources/templates
	cp -r include/persistent-volume.json build/resources/templates

	mkdir -p install/resources/templates
	mkdir -p install/resources/address-space-controller
	mkdir -p install/resources/api-server
	mkdir -p install/resources/resource-definitions
	mkdir -p install/resources/cluster-roles/kubernetes
	mkdir -p install/resources/cluster-roles/openshift
	mkdir -p install/resources/plans
	cat include/address-space-definitions.yaml | \
		STANDARD_CONTROLLER_IMAGE=$(DOCKER_REGISTRY)/$(DOCKER_ORG)/$(STANDARD_CONTROLLER_IMAGE):$(COMMIT) \
		ROUTER_IMAGE=$(DOCKER_REGISTRY)/$(DOCKER_ORG)/$(ROUTER_IMAGE):$(COMMIT) \
		BROKER_IMAGE=$(DOCKER_REGISTRY)/$(DOCKER_ORG)/$(ARTEMIS_IMAGE):$(COMMIT) \
		TOPIC_FORWARDER_IMAGE=$(DOCKER_REGISTRY)/$(DOCKER_ORG)/$(TOPIC_FORWARDER_IMAGE):$(COMMIT) \
		ROUTER_METRICS_IMAGE=$(DOCKER_REGISTRY)/$(DOCKER_ORG)/$(ROUTER_METRICS_IMAGE):$(COMMIT) \
		SUBSERV_IMAGE=$(DOCKER_REGISTRY)/$(DOCKER_ORG)/$(SUBSERV_IMAGE):$(COMMIT) \
		SERVICE_BROKER_IMAGE=$(DOCKER_REGISTRY)/$(DOCKER_ORG)/$(SERVICE_BROKER_IMAGE):$(COMMIT) \
		AGENT_IMAGE=$(DOCKER_REGISTRY)/$(DOCKER_ORG)/$(AGENT_IMAGE):$(COMMIT) \
		MQTT_GATEWAY_IMAGE=$(DOCKER_REGISTRY)/$(DOCKER_ORG)/$(MQTT_GATEWAY_IMAGE):$(COMMIT) \
		MQTT_LWT_IMAGE=$(DOCKER_REGISTRY)/$(DOCKER_ORG)/$(MQTT_LWT_IMAGE):$(COMMIT) \
		envsubst '$${STANDARD_CONTROLLER_IMAGE},$${ROUTER_IMAGE},$${TOPIC_FORWARDER_IMAGE},$${BROKER_IMAGE},$${ROUTER_METRICS_IMAGE},$${SUBSERV_IMAGE},$${SERVICE_BROKER_IMAGE},$${AGENT_IMAGE},$${MQTT_GATEWAY_IMAGE},$${MQTT_LWT_IMAGE}' > install/resources/address-space-controller/address-space-definitions.yaml
	cp -f include/resource-definitions.yaml install/resources/resource-definitions
	cp -f include/cluster-roles/*.yaml install/resources/cluster-roles
	cp -f include/cluster-roles/kubernetes/*.yaml install/resources/cluster-roles/kubernetes
	cp -f include/cluster-roles/openshift/*.yaml install/resources/cluster-roles/openshift
	cp -f include/*-plans.yaml install/resources/plans/
	cp -f include/address.yaml include/address-space.yaml include/service-broker.yaml install/resources/templates

package: prepare $(OBJS) yaml
	mkdir -p build/$(INSTALLDIR)                                                                                                                                               
	cp -r install/* build/$(INSTALLDIR)                                                                                                                                        
	tar -czf build/$(INSTALLDIR).tgz -C build $(INSTALLDIR)

coverage:

.PHONY: yaml prepare package clean
