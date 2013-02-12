package com.conga.nu;

import com.conga.nu.test.RequestService;
import com.conga.nu.test.ApplicationService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNot.*;
import static org.hamcrest.core.IsNull.*;
import static com.conga.nu.Services.*;

/**
 *
 * @author Todd Fast
 */
public class ConcurrentServicesTest {

    public ConcurrentServicesTest() {
    }


	@BeforeClass
	public static void setUpClass() throws Exception {
	}


	@AfterClass
	public static void tearDownClass() throws Exception {
	}

    @Before
    public void setUp() {
		Services.reset();
		Services.getInstance().initializeDefaultFinder();
		Services.getInstance().beginApplication();
    }

    @After
    public void tearDown() {
		Services.reset();
    }

//@Ignore
//	@Test
//	public void multipleRequests()
//		throws Exception {
//
//		final int REQUEST_COUNT = 1000;
//		final int THREAD_COUNT = 25;
//
//		List<Callable<RequestService>> tasks=
//			new ArrayList<Callable<RequestService>>();
//
//		for (int i=0; i<REQUEST_COUNT; i++)
//		{
//			final int value=i;
//
//			// Tasks - each task makes exactly one service invocation.
//			Callable<RequestService> callable=
//				new Callable<RequestService>() {
//
//					private int counter=value;
//
//					public RequestService call() throws Exception {
//
//						Services.getInstance().beginRequest();
//						try {
////							if (Math.random()<0.50)
//
//							Thread.sleep(Math.round(Math.random()*250));
//							RequestService service=$(RequestService.class);
//							// System.out.println(service.getID());
//							// System.out.println(counter);
//
//							$(SingletonService.class);
//
//							return service;
//						}
//						finally {
//							Services.getInstance().endRequest();
//						}
//					}
//				};
//
//			tasks.add(callable);
//		}
//
//		// Execute tasks
//		ExecutorService executorService=
//			Executors.newFixedThreadPool(THREAD_COUNT);
//		// invokeAll() blocks until all tasks have run...
//		List<Future<RequestService>> futures=
//			executorService.invokeAll(tasks);
//		assertThat(futures.size(), is(REQUEST_COUNT));
//
//		// Assertions
//		Set<RequestService> services=
//			new HashSet<RequestService>(REQUEST_COUNT);
//		for (Future<RequestService> future : futures)
//		{
//			// get() will throw an exception if an exception was thrown
//			// when getting the service.
//			RequestService requestService = future.get();
//
//			// Did we get a RequestService?
//			assertThat(requestService, not(nullValue()));
//
//			// Is the RequestService id unique (see Set.add() javadoc)?
//			assertThat(services.add(requestService), is(true));
//		}
//
//		// Did we get the right number of RequestService ids?
//		assertThat(services.size(),is(REQUEST_COUNT));
//	}

	@Test
	public void multipleRequestsForRequestService()
		throws Exception {

		final int REQUEST_COUNT=1000;
		final int THREAD_COUNT=25;

		ConcurrentTester tester=
			new ConcurrentTester<RequestService>(THREAD_COUNT);

		tester.setFactory(
			new ConcurrentTester.Factory<Callable<RequestService>>() {
				public Callable<RequestService> create(final int count) {
					return new Callable<RequestService>() {
						public RequestService call() throws Exception {

							Services.getInstance().beginRequest();
							try {
								Thread.sleep(Math.round(Math.random()*125));
								RequestService service=$(RequestService.class);
								Thread.sleep(Math.round(Math.random()*125));
								//System.out.println(count);
								return service;
							}
							finally {
								List<Throwable> completionExceptions=
									Services.getInstance().endRequest();
								assertNull(completionExceptions);
							}
						}
					};
				}});

		List<Future<RequestService>> futures=tester.run(REQUEST_COUNT);

		// Assertions
		Set<RequestService> services=
			new HashSet<RequestService>(REQUEST_COUNT);
		for (Future<RequestService> future : futures)
		{
			// get() will throw an exception if an exception was thrown
			// when getting the service.
			RequestService service = future.get();

			// Did we get a RequestService?
			assertThat(service, not(nullValue()));

			// Is the RequestService unique
			assertThat(service.isCompleted(), is(true));

			// Is the RequestService unique
			assertThat(services.add(service), is(true));
		}

		// Did we get the right number of RequestService ids?
		assertThat(services.size(),is(REQUEST_COUNT));
	}


	@Test
	public void multipleRequestsForSingletonService()
		throws Exception {

		final int REQUEST_COUNT=200;
		final int THREAD_COUNT=5;

		ConcurrentTester tester=
			new ConcurrentTester<ApplicationService>(THREAD_COUNT);

		tester.setFactory(
			new ConcurrentTester.Factory<Callable<ApplicationService>>() {
				public Callable<ApplicationService> create(final int count) {
					return new Callable<ApplicationService>() {
						public ApplicationService call() throws Exception {
							Thread.sleep(Math.round(Math.random()*125));
							ApplicationService service=
								$(ApplicationService.class);
							Thread.sleep(Math.round(Math.random()*125));
							//System.out.println(count);
							return service;
						}
					};
				}});

		List<Future<ApplicationService>> futures=tester.run(REQUEST_COUNT);

		// Assertions
		ApplicationService instance=null;
		int count=0;

		for (Future<ApplicationService> future : futures)
		{
			// get() will throw an exception if an exception was thrown
			// when getting the service.
			ApplicationService service=future.get();
			if (instance==null)
				instance=service;

			count++;

			// Did we get a service?
			assertThat(service,not(nullValue()));

			// Is the service unique
			assertThat(service.isCompleted(), is(false));

			assertSame(instance,service);
		}

		// Did we get the right number of RequestService ids?
		assertThat(count,is(REQUEST_COUNT));
	}
}