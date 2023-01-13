package com.airhacks.ping.boundary;

import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import static java.lang.System.getenv;
@Path("hello")
public class HelloWorldResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject helloWorld() {
        String hostname = Optional.ofNullable(getenv("HOSTNAME")).orElse("localhost");
        return Json.createObjectBuilder()
            .add("message", "Cloud Native Application")
            .add("hostname", hostname)
            .build();
    }
}
