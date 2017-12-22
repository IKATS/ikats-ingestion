package fr.cs.ikats.ingestion.api;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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

	private static final Object PUT_ACTION_RESTART_SESSION = "restart";

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

    /**
     * Return the stats of the session
     * @param id
     * @return
     */
    @GET
    @Path("{id}/stats")
    @Produces(value = {MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getSessionStats(@PathParam(value = "id") int id) {

    	ImportSession session = (ImportSession) app.getSession(id);

    	if (session != null) {
    		return Response.ok(session.getStats()).build();
    	} else {
    		return Response.status(Status.NOT_FOUND).build();
    	}
    }

    // Review#147170 javadoc manquante
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSession(ImportSessionDto session, @Context UriInfo uriInfo) {

      String body = "";

    	if (app == null) {
    		logger.error("IngestionService EJB not injected");
    		return Response.status(Status.SERVICE_UNAVAILABLE).build();
    	}

    	// information for the location header
    	UriBuilder uri = uriInfo.getAbsolutePathBuilder();
    	Status status = null;

    	//
    	ImportSession existingSession = app.getExistingSession(session);
    	if (existingSession != null) {
    		// Return the location header with location of the existing session and the HTTP status 409
					Integer id = existingSession.getId();
        	uri.path(Integer.toString(id));
        	status = Status.CONFLICT;
        	body = Integer.toString(id);
		}
    	else {

    		// Else add the session and gets its id
    		int id = app.addSession(session);
				body = Integer.toString(id);

    		// prepare the new id in the location header and the response with status 201
    		uri.path(Integer.toString(id));
        	status = Status.CREATED;
    	}

    	return Response.status(status).location(uri.build()).entity(body).build();
    }

    // Review#147170 javadoc manquante pour expliquer ce service
    // Review#147170 ... du coup est ce utile d'avoir ce service non implémenté ... code mort ?
    @PUT
    @Path("{action}")
    public Response updateSessions(@PathParam(value = "action") String action) {
        // TODO implement !
		return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * <p>Allow to do an action on the provided session.</p>
     * <p>The only allowed action for now is "restart" : see {@link IngestionService#restartSession(int, boolean)}</p>
     * @param id The id of the session
     * @param action The action to do on that session
     * @param force pass that boolean to the action
     * @param uriInfo context information to allow return location header in the response
     * @return the HTTP response
     */
    @PUT
    @Path("{id}/{action}")
    public Response updateSession(@PathParam(value = "id") int id,
    		@PathParam(value = "action") String action,
    		@QueryParam(value = "force") @DefaultValue("false") boolean force,
    		@Context UriInfo uriInfo) {

    	// Check if the session exists
    	if (app.getSession(id) == null) {
			return Response.status(Status.NOT_FOUND).build();
		}

    	// run only if action is PUT_ACTION_RESTART_SESSION
    	if (action.equals(PUT_ACTION_RESTART_SESSION)) {

    		app.restartSession(id, force);

    		// information for the location header
        	UriBuilder uri = uriInfo.getAbsolutePathBuilder();

        	// Response with HTTP code 200 OK
    		return Response.ok().contentLocation(uri.path(Integer.toString(id)).build()).build();
    	}

    	return Response.notAcceptable(null).build();
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
