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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fr.cs.ikats.ingestion.IngestionService;
import fr.cs.ikats.ingestion.model.ImportSession;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("sessions")
@Stateless
public class Sessions {
	
	@EJB IngestionService app;
	
    /**
     * @return the list of {@link ImportSession} that will be returned as a application/json response.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ImportSession> getSessionsList() {
        return app.getSessions();
//    	return new ArrayList<ImportSession>(0);
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSession(ImportSessionDto session) {
    	return null;
    }
    
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public Response cancellSession(ImportSessionDto session) {
    	return null;
    }
    
}
