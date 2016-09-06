package fr.cs.ikats.ingestion.process;

import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cs.ikats.ingestion.model.ImportSession;
import fr.cs.ikats.ingestion.model.ImportStatus;

public class IngestionProcess implements Runnable {

	private ManagedThreadFactory threadFactory;
	private ImportSession session;
	
	private Logger logger = LoggerFactory.getLogger(IngestionProcess.class);

	public IngestionProcess(ManagedThreadFactory threadFactory, ImportSession session) {
		this.threadFactory = threadFactory;
		this.session = session;
	}

	@Override
	public void run() {
		
		// Implement life cycle :
		
		// 1- Analyse import to do : prepare ImportSession object
		// 2- Create the dataset
		// 3- Launch first batch import
		//    For each item : if OpenTSDB import OK, add it to the dataset, create the metadata 
		// 4- Analyse first batch import
		// 5- Loop to 2 until each item is imported if previous analyse permits it.
		
		// manage only on ImportSession
		while(session.getStatus() != ImportStatus.COMPLETED
				|| session.getStatus() != ImportStatus.CANCELLED
				|| session.getStatus() != ImportStatus.STOPPED) {
			
			switch (session.getStatus()) {
			case CREATED:
				Thread analyser = threadFactory.newThread(new ImportAnalyser(this.session));
				analyser.start();
				break;
			case ANALYSED:
				logger.info("Import session analysed: Dataset={}, Nb Items to import={}", session.getDataset(), session.getItemsToImport().size());
				registerDataset(session);
				break;
			case DATASET_REGISTERED:
				logger.info("Datset {} registered in IKATS", session.getDataset());
			case RUNNING:
			case COMPLETED:
			default:
				// TODO manage an exception here when implementation will be full
				//break;
				// For instance, set import cancelled :
				session.setStatus(ImportStatus.CANCELLED);
				continue;
			}
			
			try {
				Thread.sleep(10000);
			} catch (InterruptedException ie) {
				// TODO manage error ?
				logger.warn("Interrupted while waiting", ie);
			}
		}
		

	}

	private void registerDataset(ImportSession session) {

		// Create web client, form, and url to call TDM API 
        ClientConfig clientConfig = new ClientConfig();
        Client client = ClientBuilder.newClient(clientConfig);
        String appUrl = "http://localhost"; // FIXME to be externalised in resource ?
        
        // 1- try to find dataset
        String url = appUrl + "/dataset/" + session.getDataset();
        Response response = client.target(url).request().get();
        
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
        	logger.info("Dataset {} exists, will be updated with session import (id {})", session.getDataset(), session.getId());
        	return;
        }
        // ELSE
        
        // 2- if dataset doesn't exists, create it with no list of tsuid
        // http://localhost/TemporalDataManagerWebApp/dataset/import/{dataset}
        // Form data : 
        //   - tsuidList (empty)
        //   - description
        clientConfig.register(MultiPartFeature.class);
        client = ClientBuilder.newClient(clientConfig);
        
        Form form = new Form();
        form.param("description", session.getDescription());
        form.param("tsuidList", "");

        url = appUrl + "/dataset/import/" + session.getDataset();
        response = client.target(url).request().post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        
        if(response.getStatus() != Response.Status.CREATED.getStatusCode()) {
        	logger.error("Dataset {} not created as per TDM API response {}", session.getDataset(), response.getStatusInfo());
        	// Cancel the import session
        	session.setStatus(ImportStatus.CANCELLED);
        }

        logger.info("Dataset {} registered by import in session id {}", session.getDataset(), session.getId());
	}

}
