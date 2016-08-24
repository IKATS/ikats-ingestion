package fr.cs.ikats.ingestion.api;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.ingestion.IngestionService;
import fr.cs.ikats.ingestion.model.ImportSession;

/**
 * Root resource (exposed at "sessions" path)
 */
@Path("sessions")
@Stateless
public class Sessions {
	
	@EJB IngestionService app;
	
	private Logger logger = LoggerFactory.getLogger(Sessions.class);
	
    /**
     * @return the list of {@link ImportSession} as a 'application/json' response.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSessionsList() {
    	
    	if (app == null) {
    		logger.error("IngestionService EJB not injected");
    		return Response.status(Status.SERVICE_UNAVAILABLE).build();
    	}
    	
    	GenericEntity<List<ImportSession>> sessionsWrapped = new GenericEntity<List<ImportSession>>(app.getSessions(), List.class); 
        return Response.ok(sessionsWrapped).build();
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSession(ImportSessionDto session, @Context UriInfo uriInfo) {
    	
    	if (app == null) {
    		logger.error("IngestionService EJB not injected");
    		return Response.status(Status.SERVICE_UNAVAILABLE).build();
    	}

    	int id = app.addSession(session);
    	UriBuilder uri = uriInfo.getAbsolutePathBuilder();
    	uri.path(Integer.toString(id));
    	
    	return Response.created(uri.build()).build();
    }
    
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public Response cancellSession(ImportSessionDto session) {
    	// TODO implement !
		return Response.status(Status.NOT_IMPLEMENTED).entity(session).build();
    }
    
}
