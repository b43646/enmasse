/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.mqtt;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttServerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * Vert.x based MQTT Frontend for EnMasse
 */
@Component
public class MqttFrontend extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(MqttFrontend.class);

    @Value(value = "${enmasse.mqtt.bindaddress:0.0.0.0}")
    private String bindAddress;
    @Value(value = "${enmasse.mqtt.listenport:1883}")
    private int listenPort;
    @Value(value = "${enmasse.mqtt.connectaddress:0.0.0.0}")
    private String connectAddress;
    @Value(value = "${enmasse.mqtt.connectport:5672}")
    private int connectPort;

    private MqttServer server;

    /**
     * Start the MQTT server component
     *
     * @param startFuture
     */
    private void bindMqttServer(Future<Void> startFuture) {

        MqttServerOptions options = new MqttServerOptions();
        options.setHost(this.bindAddress).setPort(this.listenPort);

        this.server = MqttServer.create(this.vertx, options);

        this.server
                .endpointHandler(this::handleMqttEndpointConnection)
                .listen(done -> {

                    if (done.succeeded()) {
                        LOG.info("MQTT frontend running on {}:{}", this.bindAddress, this.server.actualPort());
                        startFuture.complete();
                    } else {
                        LOG.error("Error while starting up MQTT frontend", done.cause());
                        startFuture.fail(done.cause());
                    }

                });
    }

    private void handleMqttEndpointConnection(MqttEndpoint endpoint) {
        // TODO
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        LOG.info("Starting MQTT frontend verticle...");
        this.bindMqttServer(startFuture);
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {

        LOG.info("Stopping MQTT frontend verticle ...");

        Future<Void> shutdownTracker = Future.future();
        shutdownTracker.setHandler(done -> {
           if (done.succeeded()) {
               LOG.info("MQTT frontend has been shut down successfully");
               stopFuture.complete();
           } else {
               LOG.info("Error while shutting down MQTT frontend", done.cause());
               stopFuture.fail(done.cause());
           }
        });

        if (this.server != null) {
            this.server.close(shutdownTracker.completer());
        } else {
            shutdownTracker.complete();
        }
    }
}
