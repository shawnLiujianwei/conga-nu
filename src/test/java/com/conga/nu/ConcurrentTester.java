package com.conga.nu;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;

/**
 *
 * @author Todd Fast
 */
public class ConcurrentTester<T> {

	public ConcurrentTester(int threads) {
		super();
		this.threads=threads;
	}

	public ConcurrentTester(int threads, Factory<Callable<T>> factory) {
		this(threads);
		this.factory=factory;
	}

	public int getNumThreads() {
		return threads;
	}

	public Factory<Callable<T>> getFactory() {
		return factory;
	}

	public void setFactory(Factory<Callable<T>> value) {
		factory=value;
	}

	public List<Future<T>> run(int requests) throws InterruptedException {

		// Collect the tasks
		List<Callable<T>> tasks=new ArrayList<Callable<T>>();
		Callable<T> task=null;

		for (int i=0; i<requests; i++) {
			task=getFactory().create(i);
			if (task==null)
				throw new NullPointerException(
					"Factory return null for task "+i);
			tasks.add(task);
		}

		// Execute tasks
		ExecutorService executorService=
			Executors.newFixedThreadPool(getNumThreads());

		// invokeAll() blocks until all tasks have run...
		List<Future<T>> futures=
			executorService.invokeAll(tasks);

		assertThat(futures.size(), is(requests));

		return futures;
	}




	////////////////////////////////////////////////////////////////////////////
	// Inner class
	////////////////////////////////////////////////////////////////////////////

	/**
	 *
	 * 
	 * @param <C>
	 */
	public static interface Factory<C extends Callable> {
		/**
		 *
		 *
		 * @param count
		 * @return
		 */
		public C create(final int count);
	}


	private int threads;
	private Factory<Callable<T>> factory;
}
