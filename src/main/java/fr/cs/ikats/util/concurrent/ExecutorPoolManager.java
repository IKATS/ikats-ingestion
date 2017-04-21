package fr.cs.ikats.util.concurrent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.enterprise.concurrent.ManagedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a fixed size thread pool with an {@link ExecutorService}.<br>
 * The submit method return null if the submitted task could not be executed in case of the pool is busy. 
 * @author ftoral
 */
@Singleton
public class ExecutorPoolManager {

	private ExecutorService threadPoolExecutor = null;

	int corePoolSize = 5;
	int maxPoolSize = 10;
	long keepAliveTime = 5000;
	int workingQueueSize = 10;

	// Review#147170 quasi redondant avec la factory de IngestionService ? pourquoi 
    // Review#147170 y a t il une difference dans name=... ?
	@Resource(name = "DefaultManagedThreadFactory")
	ManagedThreadFactory factory;
	
	private Logger logger = LoggerFactory.getLogger(ExecutorPoolManager.class);

	@PostConstruct
	public void init() {
		logger.info("Create the executor pool, size:{} max:{}, queue:{}", corePoolSize, maxPoolSize, workingQueueSize);
		threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize,
				keepAliveTime, TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(workingQueueSize), factory);

	}

	@PreDestroy
	public void destroy() {
		logger.info("Destroy called, shutting donc the ThreadPoolExecutor");
		threadPoolExecutor.shutdown();
	}

	/**
	 * Submit a task to a fixed size {@link ExecutorService}
	 * @param task
	 * @return the {@link Future}'s task or null if we couldn't submit it
	 */
	public <T> Future<T> submit(Callable<T> task) {
		try {
			return threadPoolExecutor.submit(task);
		} catch (RejectedExecutionException ree) {
			// The REE can't be throwed because it will be catched by EJB container
			// has we do not implement any handler, the null value returned signifies that tasks was not scheduled. 
			// https://docs.oracle.com/javaee/7/tutorial/ejb-basicexamples005.htm
			return null;
		}
	}
}
