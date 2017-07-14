package edu.vanderbilt.yunyul.vertxtw;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.healthchecks.Status;

import java.util.concurrent.ThreadLocalRandom;

public class TestHealthChecks {
    public static HealthChecks produceHealthChecks(Vertx vertx) {
        HealthChecks healthChecks = HealthChecks.create(vertx);
        healthChecks.register("networking/network-available", fut -> fut.complete(Status.OK()));
        healthChecks.register("networking/epoll-available", fut -> fut.complete(Status.KO(new JsonObject().put("level", 0.02))));
        healthChecks.register("random-is-working", fut -> {
            if (ThreadLocalRandom.current().nextBoolean()) {
                fut.complete(Status.OK());
            } else {
                fut.complete(Status.KO());
            }
        });
        return healthChecks;
    }
}
