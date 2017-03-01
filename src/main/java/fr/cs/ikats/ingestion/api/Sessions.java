package fr.cs.ikats.ingestion.api;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
    
    // Review#147170 javadoc manquante
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSession(@PathParam(value = "id") int id) {

    	ImportSessionDto session = app.getSession(id);
    	
    	if (session != null) {
    		return Response.ok(session).build();
    	} else {
    		return Response.status(Status.NOT_FOUND).build();
    	}
    }
    
    // Review#147170 javadoc manquante
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSession(ImportSessionDto session, @Context UriInfo uriInfo) {
    	
    	if (app == null) {
    		logger.error("IngestionService EJB not injected");
    		return Response.status(Status.SERVICE_UNAVAILABLE).build();
    	}

    	// Add the session
    	int id = app.addSession(session);

    	// Return the location header
    	UriBuilder uri = uriInfo.getAbsolutePathBuilder();
    	uri.path(Integer.toString(id));
    	
    	return Response.created(uri.build()).build();
    }
    
    // Review#147170 javadoc manquante pour expliquer ce service
    // Review#147170 ... du coup est ce utile d'avoir ce service non implémenté ... code mort ?
    @PUT
    @Path("{action}")
    public Response updateSessions(@PathParam(value = "action") String action) {
        // TODO implement !
		return Response.status(Status.NOT_IMPLEMENTED).build();
    }
    
    // Review#147170 javadoc manquante pour expliquer ce service
    // Review#147170 ... du coup est ce utile d'avoir ce service non implémenté ... code mort ?
    @PUT
    @Path("{id}/{action}")
    public Response updateSession(@PathParam(value = "id") int id, @PathParam(value = "action") String action) {
    	// TODO implement !
		return Response.status(Status.NOT_IMPLEMENTED).build();
    }
    
    // Review#147170 javadoc manquante
    // Review#147170 ... du coup est ce utile d'avoir ce service non implémenté ... code mort ?
    @DELETE
    public Response removeAll() {
    	// TODO implement !
		return Response.status(Status.NOT_IMPLEMENTED).build();
    }
    
    // Review#147170 javadoc manquante
    // Review#147170 ... du coup est ce utile d'avoir ce service non implémenté ... code mort ?
    @DELETE
    @Path("{id}")
    public Response removeSession(@PathParam(value = "id") int id) {
    	// TODO implement !
		return Response.status(Status.NOT_IMPLEMENTED).build();
    }
    
}
