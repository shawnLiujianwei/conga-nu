/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.conga.nu;

import com.conga.nu.test.ApplicationService;
import com.conga.nu.test.RequestService;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static com.conga.nu.Services.*;

/**
 *
 * @author Todd Fast
 */
public class PerformanceTest {

    public PerformanceTest() {
    }


	@BeforeClass
	public static void setUpClass() throws Exception {
		Services.reset();
	}


	@AfterClass
	public static void tearDownClass() throws Exception {
		Services.reset();
	}

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

	@Test
	public void simpleIteration() {

		Object value=null;

		long baseline=System.nanoTime();

		for (int i=0; i<ITERATIONS; i++) {
			value=null;
		}
		long lap=System.nanoTime();

		outputTime("Simple",baseline,lap);
	}

	@Test
	public void mapAccessIteration() {

		final String TEST="test";
		Object value=null;

		Map map=new HashMap();
		map.put(TEST,new Object());

		long baseline=System.nanoTime();

		for (int i=0; i<ITERATIONS; i++) {
			value=map.get(TEST);
		}
		long lap=System.nanoTime();

		outputTime("Map access",baseline,lap);
	}

	@Test
	public void threadLocalIteration() {

		Object value=null;

		ThreadLocal threadLocal=new ThreadLocal();
		threadLocal.set(new Object());

		long baseline=System.nanoTime();

		for (int i=0; i<ITERATIONS; i++) {
			value=threadLocal.get();
		}
		long lap=System.nanoTime();

		outputTime("ThreadLocal access",baseline,lap);
	}

	@Test
	public void requestServiceIteration() {

		Services.getInstance().reset();
		Services.getInstance().initializeDefaultFinder();
		try {
			Services.getInstance().beginApplication();
			Services.getInstance().beginRequest();

			Object value=$(RequestService.class);

			long baseline=System.nanoTime();

			for (int i=0; i<ITERATIONS; i++) {
				value=$(RequestService.class);
			}
			long lap=System.nanoTime();

			outputTime("Request service",baseline,lap);
		}
		finally {
			Services.getInstance().endRequest();
			Services.getInstance().reset();
		}
	}

	@Test
	public void applicationServiceIteration() {

		Services.getInstance().reset();
		Services.getInstance().initializeDefaultFinder();
		try {
			Services.getInstance().beginApplication();
			Services.getInstance().beginRequest();

			Object value=$(ApplicationService.class);

			long baseline=System.nanoTime();

			for (int i=0; i<ITERATIONS; i++) {
				value=$(ApplicationService.class);
			}
			long lap=System.nanoTime();

			outputTime("Singleton service",baseline,lap);
		}
		finally {
			$(ApplicationService.class).resetInstanceCount();
			Services.getInstance().endRequest();
			Services.getInstance().reset();
		}
	}

	@Test
	public void beginEndRequestIteration() {

		Services.getInstance().reset();
		Services.getInstance().initializeDefaultFinder();
		try {
			Services.getInstance().beginApplication();

			long baseline=System.nanoTime();

			for (int i=0; i<ITERATIONS; i++) {
				Services.getInstance().beginRequest();
				Services.getInstance().endRequest();
			}
			long lap=System.nanoTime();

			outputTime("Begin/end request",baseline,lap);
		}
		finally {
			Services.getInstance().reset();
		}
	}


	@Test
	public void fullRequestSimulationIteration() {

		Services.getInstance().reset();
		Services.getInstance().initializeDefaultFinder();
		try {
			Services.getInstance().beginApplication();

			Object value=null;

			long baseline=System.nanoTime();

			for (int i=0; i<ITERATIONS; i++) {
				Services.getInstance().beginRequest();
				value=$(RequestService.class);
				value=$(ApplicationService.class);
				Services.getInstance().endRequest();
			}
			long lap=System.nanoTime();

			outputTime("Full request simulation",baseline,lap);
		}
		finally {
			Services.getInstance().reset();
		}
	}

	private void outputTime(String message, long baseline, long lap) {
		System.out.println("[Performance]: "+message+": "+ITERATIONS+
			" iterations took "+(lap-baseline)+"ns ("+
			(float)((lap-baseline)/1000000f)+"ms): "+
			(float)((lap-baseline)/(float)ITERATIONS)+"ns per iteration");
	}

	private static final int ITERATIONS=10000000;
}