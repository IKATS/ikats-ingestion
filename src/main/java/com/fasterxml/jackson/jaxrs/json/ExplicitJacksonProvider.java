package com.fasterxml.jackson.jaxrs.json;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

/**
 * Stackoverflow : How to use Jackson as JSON provider for JAX-RS-Client instead of Johnzon in TomEE 7?
 * https://stackoverflow.com/a/38915278 
 * 
 * quote : « jackson uses a custom matching strategy and therefore uses "/". So the bus priority is ignored since mediatype priority is enough to say johnzon is "more adapted" than jackson for json.
 * To solve it the easiest is likely to override jackson provider (just extend it) and decorate it with @Provides/@Consumes with MediaType.APPLICATION_JSON instead of wildcard one »
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ExplicitJacksonProvider extends JacksonJaxbJsonProvider {

}
