package fr.cs.ikats.ingestion;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import fr.cs.ikats.ingestion.api.MyResource;
import fr.cs.ikats.ingestion.api.Sessions;

/**
 * Main application class.
 *
 * @author ftoral
 */
@ApplicationPath("/api/*")
public class IngestionApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = new HashSet<Class<?>>();
        // register root resources/providers
        classes.add(Sessions.class);
        // Review#147170 supprimer MyResource ? 
        classes.add(MyResource.class);
        return classes;
    }
}