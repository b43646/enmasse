/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.apiclients;

import io.enmasse.systemtest.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public abstract class ApiClient {
    protected static Logger log = CustomLogger.getLogger();
    protected WebClient client;
    protected Kubernetes kubernetes;
    protected Vertx vertx;
    protected Endpoint endpoint;
    protected String authzString;
    protected String apiVersion;

    protected ApiClient(Kubernetes kubernetes, Endpoint endpoint, String apiVersion) {
        this.vertx = VertxFactory.create();
        this.kubernetes = kubernetes;
        this.connect();
        this.authzString = String.format("Bearer %s", kubernetes.getApiToken());
        this.endpoint = endpoint;
        this.apiVersion = apiVersion;
    }

    protected abstract String apiClientName();

    protected void reconnect() {
        this.client.close();
        connect();
    }

    protected void connect() {
        this.client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                // TODO: Fetch CA and use
                .setTrustAll(true)
                .setVerifyHost(false));
    }

    protected <T> void responseHandler(AsyncResult<HttpResponse<T>> ar, CompletableFuture<T> promise, int expectedCode,
                                       String warnMessage) {
        try {
            if (ar.succeeded()) {
                HttpResponse<T> response = ar.result();
                if (response.statusCode() != expectedCode) {
                    log.error("expected-code: {}, responsep-code: {}, body: {}", expectedCode, response.statusCode(), response.body());
                    T body = response.body();
                    promise.completeExceptionally(new RuntimeException("Status " + response.statusCode() + " body: " + body != null ? body.toString() : null));
                } else {
                    promise.complete(ar.result().body());
                }
            } else {
                log.warn(warnMessage);
                promise.completeExceptionally(ar.cause());
            }
        } catch (io.vertx.core.json.DecodeException decEx) {
            if (ar.result().bodyAsString().toLowerCase().contains("application is not available")) {
                log.warn("'{}' is not available.", apiClientName(), ar.cause());
                throw new IllegalStateException(String.format("'%s' is not available.", apiClientName()));
            } else {
                log.warn("Unexpected object received", ar.cause());
                throw new IllegalStateException("JsonObject expected, but following object was received: " + ar.result().bodyAsString());
            }
        }
    }

    protected <T> T doRequestNTimes(int retry, Callable<T> fn, Optional<Callable<Endpoint>> endpointFn, Optional<Runnable> reconnect) throws Exception {
        return TestUtils.doRequestNTimes(retry, () -> {
            if (endpointFn.isPresent()) {
                endpoint = endpointFn.get().call();
            }
            return fn.call();
        }, reconnect);
    }

    public String getApiVersion() {
        return apiVersion;
    }
}
