/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.conga.nu;

import com.conga.nu.test.RequestService;
import com.conga.nu.test.ApplicationService;
import com.conga.nu.test.ApplicationServiceWithImmutableFields;
import com.conga.nu.test.ApplicationServiceWithMutableFields;
import com.conga.nu.test.BogusService;
import com.conga.nu.test.RequestServiceImpl;
import com.conga.nu.test.RequestServiceImpl2;
import com.conga.nu.test.TestBlacklistRequestServiceImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.conga.nu.Services.*;

/**
 *
 * @author Todd Fast
 */
@SuppressWarnings("unchecked")
public class ServicesTest {

    public ServicesTest() {
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
	public void tryReset() {

		Services.reset();
		Services.getInstance().initializeDefaultFinder();
		try {
			Services s1=Services.getInstance();
			Services s2=Services.getInstance();
			assertSame(s1,s2);

			Services.reset();
			Services.getInstance().initializeDefaultFinder();
			Services s3=Services.getInstance();
			assertNotSame(s3,s1);
		}
		finally {
			Services.reset();
		}
	}

	@Test
	public void createSingletonService() {

		Services.reset();
		Services.getInstance().initializeDefaultFinder();
		Services.getInstance().beginApplication();
		try {
			ApplicationService service=$(ApplicationService.class);

			// Reset so we can run other tests in this class
			service.resetInstanceCount();

			assertNotNull(service);
			assertEquals(
				service.echoValue(1234),ApplicationService.CONSTANT+"1234");


		}
		finally {
			Services.reset();
		}
	}

	@Test(expected=ServiceInstantiationException.class)
	public void requestServiceNotAllowed() {
		Services.reset();
		Services.getInstance().initializeDefaultFinder();
		Services.getInstance().beginApplication();
		try {
			$(RequestService.class);
			fail("Should not be able to instantiate request-scoped service "+
				"outside of request");
		}
		finally {
			Services.reset();
		}
	}

	@Test
	public void createRequestService() {

		Services.reset();
		Services.getInstance().initializeDefaultFinder();
		Services.getInstance().beginApplication();
		Services.getInstance().beginRequest();
		try {
			RequestService service=$(RequestService.class);
			assertNotNull(service);
			assertEquals(
				RequestService.CONSTANT+"1234",service.echoValue(1234));
		}
		finally {
			Services.getInstance().endRequest();
			Services.reset();
		}
	}

	@Test
	public void reuseRequestService() {

		Services.reset();
		Services.getInstance().initializeDefaultFinder();
		Services.getInstance().beginApplication();

		Services.getInstance().beginRequest();
		try {
			RequestService service1=$(RequestService.class);
			assertNotNull(service1);

			RequestService service2=$(RequestService.class);
			assertNotNull(service2);

			assertSame("Service instances were not reused",service1,service2);
		}
		finally {
			Services.getInstance().endRequest();
			Services.reset();
		}
	}

	@Test
	public void multipleRequestsSingleThread() {

		RequestService service1=null;
		RequestService service2=null;

		Services.reset();
		Services.getInstance().initializeDefaultFinder();
		Services.getInstance().beginApplication();
		try {
			Services.getInstance().beginRequest();
			try {
				service1=$(RequestService.class);
				assertNotNull(service1);
			}
			finally {
				Services.getInstance().endRequest();
			}

			Services.getInstance().beginRequest();
			try {
				service2=$(RequestService.class);
				assertNotNull(service2);

				assertNotSame(
					"Service instances were reused",service1,service2);
			}
			finally {
				Services.getInstance().endRequest();
			}
		}
		finally {
			Services.reset();
		}
	}

	@Test
	public void insideApplicationState() {
		Services.reset();
		Services.getInstance().initializeDefaultFinder();
		Services.getInstance().beginApplication();
		try {
			// This should succeed because we haven't started the application
			$(ApplicationService.class).resetInstanceCount();
		}
		finally {
			Services.reset();
		}
	}

	@Test(expected=IllegalStateException.class)
	public void outsideApplicationState() {
		Services.reset();
		Services.getInstance().initializeDefaultFinder();
		try {
			// This should fail because we haven't started the application
			$(ApplicationService.class).resetInstanceCount();
		}
		finally {
			Services.reset();
		}
	}

	@Test(expected=ServiceInstantiationException.class)
	public void overrideProviderInvalidClass() {
		Services.reset();

		MetaInfServiceFinder finder=
			new MetaInfServiceFinder(Services.getInstance());
		finder.addProviderOverride(
			ApplicationService.class.getName(),"java.lang.String");
		Services.getInstance().addFinder(finder);

		Services.getInstance().beginApplication();
		try
		{
			$(ApplicationService.class).resetInstanceCount();
		}
		finally {
			Services.reset();
		}
	}

	@Test(expected=ServiceInstantiationException.class)
	public void overrideProviderWrongClass() {
		Services.reset();

		MetaInfServiceFinder finder=
			new MetaInfServiceFinder(Services.getInstance());
		finder.addProviderOverride(
			ApplicationService.class.getName(),"java.lang.String");
		Services.getInstance().addFinder(finder);

		Services.getInstance().beginApplication();
		try
		{
			$(ApplicationService.class).resetInstanceCount();
		}
		finally {
			Services.reset();
		}
	}

	@Test(expected=IllegalStateException.class)
	public void overrideProviderBeforeApplication() {
		Services.reset();

		MetaInfServiceFinder finder=
			new MetaInfServiceFinder(Services.getInstance());
		Services.getInstance().addFinder(finder);

		Services.getInstance().beginApplication();
		try
		{
			finder.addProviderOverride(
				ApplicationService.class.getName(),"java.lang.String");
		}
		finally {
			Services.reset();
		}
	}

	@Test(expected=IllegalStateException.class)
	public void overrideBlacklistBeforeApplication() {
		Services.reset();

		MetaInfServiceFinder finder=
			new MetaInfServiceFinder(Services.getInstance());
		Services.getInstance().addFinder(finder);

		Services.getInstance().beginApplication();
		try
		{
			finder.addProviderBlacklist(
				ApplicationService.class.getName(),"java.lang.String");
		}
		finally {
			Services.reset();
		}
	}

	@Test
	public void blacklistHigherPriorities() {
		Services.reset();

		MetaInfServiceFinder finder=
			new MetaInfServiceFinder(Services.getInstance());
		Services.getInstance().addFinder(finder);

		// Blacklist the two higher priority providers so that we get
		// TestBlacklistRequestServiceImpl
		finder.addProviderBlacklist(
			RequestService.class.getName(),RequestServiceImpl.class.getName());
		finder.addProviderBlacklist(
			RequestService.class.getName(),RequestServiceImpl2.class.getName());

		Services.getInstance().beginApplication();
		try
		{
			Services.getInstance().beginRequest();

			RequestService service=$(RequestService.class);
			assertNotNull(service);

			assertEquals(
				TestBlacklistRequestServiceImpl.BLACKLISTED,
				service.echoValue(1));
		}
		finally {
			Services.reset();
		}
	}


	@Test
	public void leakageAcrossResets() {
		Services.reset();

		MetaInfServiceFinder finder=
			new MetaInfServiceFinder(Services.getInstance());
		Services.getInstance().addFinder(finder);

		try
		{
			assertTrue(finder.getProviderOverrides().isEmpty());

			finder.addProviderOverride(
				ApplicationService.class.getName(),"java.lang.String");

			assertEquals(
				finder.getProviderOverrides().size(),1);

			Services.reset();
			finder=new MetaInfServiceFinder(Services.getInstance());
			Services.getInstance().addFinder(finder);

			assertTrue(finder.getProviderOverrides().isEmpty());
		}
		finally {
			Services.reset();
		}
	}

	@Test
	public void numObjectsCached() {
		Services.reset();
		Services.getInstance().initializeDefaultFinder();
		Services.getInstance().beginApplication();
		try
		{
			assertEquals(
				Services.getInstance().getNumApplicationObjects(),0);

			$(ApplicationService.class);

			assertEquals(
				Services.getInstance().getNumApplicationObjects(),1);

			$(ApplicationService.class);

			assertEquals(
				Services.getInstance().getNumApplicationObjects(),1);

			Services.getInstance().beginRequest();

			assertEquals(
				Services.getInstance().getNumRequestObjects(),0);

			$(RequestService.class);
			$(RequestService.class);

			assertEquals(
				Services.getInstance().getNumRequestObjects(),1);

			Services.getInstance().endRequest();

			assertEquals(
				Services.getInstance().getNumRequestObjects(),0);

			Services.getInstance().beginRequest();

			$(RequestService.class);
			$(RequestService.class);

			assertEquals(
				Services.getInstance().getNumRequestObjects(),1);

			Services.reset();

			assertEquals(
				Services.getInstance().getNumApplicationObjects(),0);
			assertEquals(
				Services.getInstance().getNumRequestObjects(),0);
		}
		finally {
			Services.reset();
		}
	}

	@Test(expected=ServiceInstantiationException.class)
	public void serviceWithMutableFields() {
		Services.reset();
		Services.getInstance().beginApplication();
		try
		{
			// This should fail because of fields in superclass
			$(ApplicationServiceWithMutableFields.class);
		}
		finally {
			Services.reset();
		}
	}

	@Test
	public void serviceWithImmutableFields() {
		Services.reset();
		Services.getInstance().initializeDefaultFinder();
		Services.getInstance().beginApplication();
		try
		{
			// This should be OK
			$(ApplicationServiceWithImmutableFields.class);
		}
		finally {
			Services.reset();
		}
	}

	@Test(expected=ServiceProviderNotFoundException.class)
	public void bogusService() {
		Services.reset();
		Services.getInstance().initializeDefaultFinder();
		Services.getInstance().beginApplication();
		try
		{
			Services.getInstance().beginRequest();

			// This should blow up since the META-INF/services configuration
			// points to an impl class that doesn't implement this interface
			$(BogusService.class);

			Services.getInstance().endRequest();
		}
		finally {
			Services.reset();
		}
	}

//	@Test
//	public void useClassLookup() {
//		Services.reset();
//		Services.getInstance().beginApplication();
//		try
//		{
//			$<ApplicationService> r=new $<ApplicationService>();
//
//			assertTrue(r.get() instanceof ApplicationService);
//			assertNotNull(r.get());
//		}
//		finally {
//			Services.reset();
//		}
//	}
}