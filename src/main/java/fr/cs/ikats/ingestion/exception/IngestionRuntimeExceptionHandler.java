/**
 * Copyright 2018 CS Systèmes d'Information
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.cs.ikats.ingestion.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle {@link IngestionRuntimeException} exception types.
 */
@Provider
public class IngestionRuntimeExceptionHandler implements ExceptionMapper<IngestionRuntimeException> {

    private static Logger logger = LoggerFactory.getLogger(IngestionRuntimeExceptionHandler.class);
    
    @Override
    public Response toResponse(IngestionRuntimeException exception) {
        
        if(IngestionRejectedException.class.isAssignableFrom(exception.getClass())) {
            logger.error("Unable to processingestion {}", exception.getMessage());
            return Response.status(Status.SERVICE_UNAVAILABLE).entity(exception.getMessage()).build();            
        } 

        // Default Error : internal server
        logger.error("Error handled while processing request {}", exception.getMessage());
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
    }

}
