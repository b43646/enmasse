package enmasse.storage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.storage.controller.admin.ClusterManager;
import io.vertx.core.AbstractVerticle;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonSession;
import org.apache.qpid.proton.amqp.messaging.*;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * AMQPServer for triggering deployments through AMQP
 */
public class AMQPServer extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(AMQPServer.class.getName());
    private final int port;
    private final ClusterManager clusterManager;
    private ProtonServer server;
    private static final ObjectMapper mapper = new ObjectMapper();

    public AMQPServer(ClusterManager clusterManager, int port) {
        this.port = port;
        this.clusterManager = clusterManager;
    }

    public void start() {
        server = ProtonServer.create(vertx);
        server.connectHandler(connection -> {
            connection.setContainer("storage-controller");
            connection.openHandler(conn -> {
                log.info("Connection opened");
            }).closeHandler(conn -> {
                connection.close();
                connection.disconnect();
                log.info("Connection closed");
            }).disconnectHandler(protonConnection -> {
                connection.disconnect();
                log.info("Disconnected");
            }).open();
            connection.sessionOpenHandler(ProtonSession::open);
            connection.receiverOpenHandler(protonReceiver -> {
                if ("address-config".equals(protonReceiver.getRemoteTarget().getAddress())) {
                    protonReceiver.handler(this::onAddressConfig);
                    protonReceiver.open();
                } else {
                    protonReceiver.close();
                }
            });
        }).listen(port);;
    }

    private void onAddressConfig(ProtonDelivery delivery, Message message) {
        String data = (String) ((AmqpValue) message.getBody()).getValue();
        try {
            clusterManager.configUpdated(mapper.readTree(data));
            delivery.disposition(new Accepted(), true);
        } catch (IOException e) {
            delivery.disposition(new Rejected(), true);
        }
    }


    public void stop() {
        if (server != null) {
            server.close();
        }
    }
}

