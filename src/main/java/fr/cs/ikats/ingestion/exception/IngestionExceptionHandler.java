package fr.cs.ikats.ingestion.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle {@link IngestionException} exception types.
 */
@Provider
public class IngestionExceptionHandler implements ExceptionMapper<IngestionException> {

    private static Logger logger = LoggerFactory.getLogger(IngestionExceptionHandler.class);
    
    @Override
    public Response toResponse(IngestionException exception) {
        
        // Default Error : internal server
        logger.error("Error handled while processing request {}", exception.getMessage());
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
    }

}
