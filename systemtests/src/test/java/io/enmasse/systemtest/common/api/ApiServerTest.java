/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.api;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.mqtt.MqttClient;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.resources.*;
import io.enmasse.systemtest.selenium.SeleniumContainers;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.standard.AnycastTest;
import io.enmasse.systemtest.standard.mqtt.PublishTest;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.TestTag.isolated;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(isolated)
class ApiServerTest extends TestBase {
    private static Logger log = CustomLogger.getLogger();
    private static final PlansProvider plansProvider = new PlansProvider(kubernetes);

    @BeforeEach
    void setUp() {
        plansProvider.setUp();
    }

    @AfterEach
    void tearDown() {
        plansProvider.tearDown();
    }

    @Test
    void testRestApiGetSchema() throws Exception {
        AddressPlan queuePlan = new AddressPlan("test-schema-rest-api-addr-plan", AddressType.QUEUE,
                Collections.singletonList(new AddressResource("broker", 0.6)));
        plansProvider.createAddressPlanConfig(queuePlan);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 0.0, 2.0),
                new AddressSpaceResource("router", 1.0, 1.0),
                new AddressSpaceResource("aggregate", 0.0, 2.0));
        List<AddressPlan> addressPlans = Collections.singletonList(queuePlan);
        AddressSpacePlan addressSpacePlan = new AddressSpacePlan("schema-rest-api-plan", "schema-rest-api-plan",
                "standard-space", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlanConfig(addressSpacePlan);

        Future<SchemaData> data = getSchema();
        SchemaData schemaData = data.get(20, TimeUnit.SECONDS);
        log.info("Check if schema object is not null");
        assertThat(schemaData.getAddressSpaceTypes().size(), not(0));

        log.info("Check if schema object contains new address space plan");
        assertTrue(schemaData.getAddressSpaceType("standard").getPlans()
                .stream()
                .map(PlanData::getName)
                .collect(Collectors.toList()).contains("schema-rest-api-plan"));

        log.info("Check if schema contains new address plans");
        assertTrue(schemaData.getAddressSpaceType("standard").getAddressType("queue").getPlans().stream()
                .filter(s -> s.getName().equals("test-schema-rest-api-addr-plan"))
                .map(PlanData::getName)
                .collect(Collectors.toList())
                .contains("test-schema-rest-api-addr-plan"));
    }

    @Test
    void testConsoleMessagingMqttRoutes() throws Exception {
        AddressSpace addressSpace = new AddressSpace("routes-space", AddressSpaceType.STANDARD, AuthService.STANDARD);
        String endpointPrefix = "test-endpoint-";
        addressSpace.setEndpoints(Arrays.asList(
                new AddressSpaceEndpoint(endpointPrefix + "messaging", "messaging", "amqps"),
                new AddressSpaceEndpoint(endpointPrefix + "console", "console", "https"),
                new AddressSpaceEndpoint(endpointPrefix + "mqtt", "mqtt", "secure-mqtt")));
        createAddressSpace(addressSpace);

        KeycloakCredentials luckyUser = new KeycloakCredentials("Lucky", "luckyPswd");
        createUser(addressSpace, luckyUser);

        //try to get all external endpoints
        kubernetes.getExternalEndpoint(addressSpace.getNamespace(), endpointPrefix + "messaging");
        kubernetes.getExternalEndpoint(addressSpace.getNamespace(), endpointPrefix + "console");
        kubernetes.getExternalEndpoint(addressSpace.getNamespace(), endpointPrefix + "mqtt");

        //messsaging
        Destination anycast = Destination.anycast("test-routes-anycast");
        setAddresses(addressSpace, anycast);
        AmqpClient client1 = amqpClientFactory.createQueueClient(addressSpace);
        client1.getConnectOptions().setCredentials(luckyUser);
        AmqpClient client2 = amqpClientFactory.createQueueClient(addressSpace);
        client2.getConnectOptions().setCredentials(luckyUser);
        AnycastTest.runAnycastTest(anycast, client1, client2);

        //mqtt
        Destination topic = Destination.topic("mytopic", "sharded-topic");
        appendAddresses(addressSpace, topic);
        Thread.sleep(10_000);
        MqttClientFactory mqttFactory = new MqttClientFactory(kubernetes, environment, addressSpace, luckyUser);
        MqttClient mqttClient = mqttFactory.createClient();
        try {
            PublishTest.simpleMQTTSendReceive(topic, mqttClient, 3);
        } finally {
            mqttFactory.close();
        }

        //console
        SeleniumProvider selenium = null;
        try {
            SeleniumContainers.deployFirefoxContainer();
            selenium = getFirefoxSeleniumProvider();
            ConsoleWebPage console = new ConsoleWebPage(
                    selenium,
                    getConsoleRoute(addressSpace),
                    addressApiClient,
                    addressSpace,
                    luckyUser);
            console.openWebConsolePage();
            console.openAddressesPageWebConsole();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (selenium != null) {
                selenium.tearDownDrivers();
            }
            SeleniumContainers.stopAndRemoveFirefoxContainer();
        }
    }

    @Test
    void testRestApiAddressResourceParams() throws Exception {
        AddressSpace addressSpace = new AddressSpace("test-rest-api-addr-space", AddressSpaceType.BROKERED);
        AddressSpace addressSpace2 = new AddressSpace("test-rest-api-addr-space2", AddressSpaceType.BROKERED);
        createAddressSpaceList(addressSpace, addressSpace2);

        logWithSeparator(log, "Check if uuid is propagated");
        String uuid = "4bfe49c2-60b5-11e7-a5d0-507b9def37d9";
        Destination dest1 = new Destination("test-rest-api-queue", uuid, addressSpace.getName(),
                "test-rest-api-queue", AddressType.QUEUE.toString(), "brokered-queue");

        setAddresses(addressSpace, dest1);
        Address dest1AddressObj = getAddressesObjects(addressSpace, Optional.of(dest1.getName())).get(20, TimeUnit.SECONDS).get(0);
        assertEquals(uuid, dest1AddressObj.getUuid(), "Address uuid is not equal");

        logWithSeparator(log, "Check if name is optional");
        Destination dest2 = new Destination(null, null, addressSpace.getName(),
                "test-rest-api-queue2", AddressType.QUEUE.toString(), "brokered-queue");
        deleteAddresses(addressSpace);
        setAddresses(addressSpace, dest2);

        HashMap<String, String> queryParams = new HashMap<>();
        queryParams.put("address", dest2.getAddress());
        Future<List<Address>> addressesObjects = getAddressesObjects(addressSpace, Optional.empty(), Optional.of(queryParams));
        Address returnedAddress = addressesObjects.get(30, TimeUnit.SECONDS).get(0);
        log.info("Got address: {}", returnedAddress.getName());
        assertTrue(returnedAddress.getName().contains(String.format("%s.%s", addressSpace.getName(), dest2.getAddress())),
                "Address name is wrongly generated");

        logWithSeparator(log, "Check if adddressSpace is optional");
        Destination dest3 = new Destination(null, null, null,
                "test-rest-api-queue3", AddressType.QUEUE.toString(), "brokered-queue");
        deleteAddresses(addressSpace);
        setAddresses(addressSpace, dest3);

        Address dest3AddressObj = getAddressesObjects(addressSpace, Optional.empty()).get(20, TimeUnit.SECONDS).get(0);
        assertEquals(addressSpace.getName(), dest3AddressObj.getAddressSpace(), "Addressspace name is empty");

        logWithSeparator(log, "Check if behavior when addressSpace is set to another existing address space");
        Destination dest4 = new Destination(null, null, addressSpace2.getName(),
                "test-rest-api-queue4", AddressType.QUEUE.toString(), "brokered-queue");
        try {
            setAddresses(addressSpace, dest4);
        } catch (java.util.concurrent.ExecutionException ex) {
            assertTrue(ex.getMessage().contains("does not match address space"),
                    "Exception does not contain correct information");
        }

        try { //missing address
            Destination destWithouAddress = Destination.queue(null, "brokered-queue");
            setAddresses(addressSpace, destWithouAddress);
        } catch (ExecutionException expectedEx) {
            JsonObject serverResponse = new JsonObject(expectedEx.getCause().getMessage());
            assertEquals("Missing 'address' string field in 'spec'", serverResponse.getString("message"),
                    "Incorrect response from server on missing address!");
        }

        try { //missing type
            Destination destWithoutType = new Destination("not-created-address", null, "brokered-queue");
            setAddresses(addressSpace, destWithoutType);
        } catch (ExecutionException expectedEx) {
            JsonObject serverResponse = new JsonObject(expectedEx.getCause().getMessage());
            assertEquals("Missing 'type' string field in 'spec'", serverResponse.getString("message"),
                    "Incorrect response from serveron missing type!");
        }

        try { //missing plan
            Destination destWithouPlan = Destination.queue("not-created-queue", null);
            setAddresses(addressSpace, destWithouPlan);
        } catch (ExecutionException expectedEx) {
            JsonObject serverResponse = new JsonObject(expectedEx.getCause().getMessage());
            assertEquals("Missing 'plan' string field in 'spec'", serverResponse.getString("message"),
                    "Incorrect response from server on missing plan!");
        }
    }

    @Test
    void testCreateAddressResource() throws Exception {
        AddressSpace addrSpace = new AddressSpace("create-address-resource-with-a-very-long-name", AddressSpaceType.STANDARD, "unlimited-standard-without-mqtt");
        createAddressSpace(addrSpace, false);

        Destination anycast = new Destination("addr1", null, addrSpace.getName(), "addr_1", AddressType.ANYCAST.toString(), "standard-anycast");
        addressApiClient.createAddress(anycast);
        List<Address> addresses = getAddressesObjects(addrSpace, Optional.empty()).get(30, TimeUnit.SECONDS);
        assertThat(addresses.size(), is(1));
        assertThat(addresses.get(0).getName(), is(String.format("%s.%s", addrSpace.getName(), anycast.getName())));

        Destination multicast = new Destination("addr2", null, addrSpace.getName(), "addr_2", AddressType.MULTICAST.toString(), "standard-multicast");
        addressApiClient.createAddress(multicast);
        addresses = getAddressesObjects(addrSpace, Optional.empty()).get(30, TimeUnit.SECONDS);
        assertThat(addresses.size(), is(2));

        String uuid = UUID.randomUUID().toString();
        Destination longname = new Destination(addrSpace.getName() + ".myaddressnameisalsoverylonginfact." + uuid, null, addrSpace.getName(), "my_addr_name_is_also_very1long", AddressType.QUEUE.toString(), "sharded-queue");
        addressApiClient.createAddress(longname);
        addresses = getAddressesObjects(addrSpace, Optional.empty()).get(30, TimeUnit.SECONDS);
        assertThat(addresses.size(), is(3));
        TestUtils.waitForDestinationsReady(addressApiClient, addrSpace, new TimeoutBudget(5, TimeUnit.MINUTES), anycast, multicast, longname);
    }
}
