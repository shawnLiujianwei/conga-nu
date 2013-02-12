package com.conga.nu;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A lightweight services factory that can automatically instantiate and cache
 * services objects--implementations of interfaces declared in
 * META-INF/services configuration files.
 *
 * TODO: Update to use ConcurrentHashMap
 * 
 * @author Todd Fast
 */
public class Services {

	/**
	 *
	 *
	 */
	protected Services() {
		super();
	}


	/**
	 * Return
	 *
	 */
	public static <S> S $(Class<S> serviceClass)
		throws ServiceInstantiationException {
		return Services.getInstance().get(serviceClass);
	}


	/**
	 *
	 *
	 */
	public <S> S get(Class<S> serviceClass)
		throws ServiceInstantiationException {

		ensureApplication();

		if (serviceClass==null)
			throw new IllegalArgumentException(
				"Parameter \"serviceClass\" cannot be null");

		S provider=null;

		// Try to find object in request scope if appropriate
		if (inRequest()) {
			provider=getCachedService(serviceClass,Scope.REQUEST);
			if (provider!=null)
				return provider;
		}

		// If doesn't exist or we're not in a request, try to getInstance the
		// provider in singelton scope. Note, this code path should not
		// synchronize until it's necessary to touch the global singleton cache.
		provider=getCachedService(serviceClass,Scope.APPLICATION);
		if (provider!=null)
			return provider;

		// Delegate to the finders to get the service instance
		ServiceFinder.Result<S> result=null;

		ServiceFinder[] finders=getFinders();
		if (finders==null || finders.length==0)
			throw new ServiceInstantiationException(serviceClass,
				"No service finders are available to lookup "+
				"providers for service type "+
				serviceClass.getName());

		// Since we need to potentially create an application-scoped object,
		// let's synchronize to avoid creating singletons more than once. In
		// theory, we can relax this if we allow singleton objects to be
		// instantiated more than once, but potentially discarded, meaning they
		// can do no meaningful caching of state in the constructor. This sync 
		// point was required after finding that multiple instances of
		// application-scoped singletons were being created during simultaneous
		// requests. Indeed, all those requests should synchronize on a single
		// critical section. Note, this synchronization point will (should) not
		// affect creation of request-scoped objects, and should not generally
		// be a factor under steady-state load.
		synchronized (APPLICATION_OBJECTS) { 

			// Check the cache again

			// Try to find object in request scope if appropriate
			if (inRequest()) {
				provider=getCachedService(serviceClass,Scope.REQUEST);
				if (provider!=null)
					return provider;
			}

			// If doesn't exist or we're not in a request, try to getInstance 
			// the provider in singelton scope. Note, this code path should not
			// synchronize until it's necessary to touch the global singleton
			// cache.
			provider=getCachedService(serviceClass,Scope.APPLICATION);
			if (provider!=null)
				return provider;

			// Find the first finder that will give us the result
			for (ServiceFinder finder: finders) {
				assert finder!=null;

				result=finder.find(serviceClass);
				if (result!=null) {

					provider=result.getProvider();

					// Verify everything in the result is as expected
					if (result.getServiceClass()==null ||
						!serviceClass.isAssignableFrom(result.getServiceClass()) ||
						provider==null || !(serviceClass.isInstance(provider)) ||
						result.getScope()==null) {
						// The finder did something bad and the results are not
						// what should be expected
						assert false:
							"The provider returned by the finder \""+finder+
							"\" did not match the type of the service class "+
							serviceClass.getName()+", or the returned service "+
							"class, provider, or scope were null (provider: "+
							provider+")";

						// Ignore this result
						provider=null;
						continue;
					}

					// Sanity check that the specified scope is what's 
					// supported by the provider. Note, not all providers will 
					// be annotated as providers.
					ServiceProvider serviceAnnotation=
						provider.getClass().getAnnotation(
							ServiceProvider.class);
					if (serviceAnnotation!=null &&
							serviceAnnotation.scope()!=result.getScope()) {
						assert false:
							"The provider class "+provider.getClass()+" is "+
							"missing the "+ServiceProvider.class.getName()+
							" annotation or the specified scope is not the "+
							"same as the scope declared by the provider "+
							"(provider scope: "+serviceAnnotation.scope()+
							"; result scope: "+result.getScope()+")";

						// Ignore this result
						provider=null;
						continue;
					}

					// Found it, and passed validation
					break;
				}
			}

			if (result!=null) {
//				// See if a similar object has already been cached. If so,
//				// we can just return it.
//				provider=findCachedService(result.getServiceClass(),
//					result.getScope());
//
//				if (provider==null) {
					// Cache the provider in the requested scope
					cacheService(result.getServiceClass(),
						result.getProvider(),result.getScope());
					return result.getProvider();
//				}
//				else {
//					return provider;
//				}
			}
			else {
				// Let caller know that we couldn't find the services. This is a
				// configuration problem in the environment and almost always an
				// error.
				throw new ServiceProviderNotFoundException(serviceClass,
					"No service provider found for service type "+
					serviceClass.getName());
			}
		}
	}


//	/**
//	 *
//	 *
//	 */
//	protected <S> S get(Class<S> serviceClass, Scope scope)
//		throws ServiceInstantiationException, IllegalStateException {
//
//		ensureApplication();
//
//		if (serviceClass==null)
//			throw new IllegalArgumentException(
//				"Parameter \"serviceClass\" cannot be null");
//
//		if (scope==null)
//			throw new IllegalArgumentException(
//				"Parameter \"scope\" cannot be null");
//
//		return getService(serviceClass,scope);
//	}


	/**
	 *
	 *
	 */
	private <S> S getCachedService(Class<S> serviceClass, Scope scope)
		throws ServiceInstantiationException, IllegalStateException {

		assert serviceClass!=null;
		assert scope!=null;

		// Disallow request-scoped services without explicit request being
		// started. If we implicitly created a request, there is no guarantee
		// it would get closed, thus resulting in bugs and objects
		// leaking across request boundaries.
		if (scope==Scope.REQUEST && !inRequest()) {
			throw new IllegalStateException(
				"Cannot create service with scope "+scope+
				" outside of request");
		}

		// Find an object that's already been created
		S result=null;

		switch (scope) {
			case REQUEST: {
				result=findCachedService(serviceClass,Scope.REQUEST);
				break;
			}

			case APPLICATION: {
				result=findCachedService(serviceClass,Scope.APPLICATION);
				break;
			}

			default: {
				throw new UnsupportedOperationException(
					"Scope \""+scope+"\" not supported");
			}
		}

		if (result!=null)
			return result;

		return null;
	}


	/**
	 *
	 *
	 */
	@SuppressWarnings("unchecked")
	/*pkg*/ <S> S findCachedService(Class<S> serviceClass, Scope scope) {

		assert serviceClass!=null:
			"Parameter \"serviceClass\" cannot be null";

		assert scope!=null:
			"Parameter \"scope\" cannot be null";

		Object result=null;

		switch (scope) {
			case REQUEST: {
				// Look in the request cache
				if (inRequest()) {
					assert threadRequestObjects.get()!=null;
					result=threadRequestObjects.get().get(serviceClass);

					// Remove from the cache if not consistent
//					if (!checkCacheConsistency(
//						serviceClass,result,Scope.REQUEST)) {
//						threadRequestObjects.get().remove(serviceClass);
//					}
				}
				break;
			}
			case APPLICATION: {

				// Try the thread-local cache first so that we can avoid
				// synchronization. Of course, this assumes that the local
				// cache will always be the same set of globally cached
				// objects, or a subset. If we eventually decide to manage
				// lifecycle of singletons, this assumption will be violated.
				Map<Class,Object> cache=threadApplicationObjectCache.get();
				if (cache!=null) {
					result=cache.get(serviceClass);
					if (result!=null)
						break;
				}

				// If not in our local cache, look in the global 
				// singleton cache, which requires synchronization
				synchronized (APPLICATION_OBJECTS) {
					result=APPLICATION_OBJECTS.get(serviceClass);

					// Remove from the cache if not consistent
//					if (!checkCacheConsistency(
//						serviceClass,result,Scope.APPLICATION)) {
//						APPLICATION_OBJECTS.remove(serviceClass);
//					}

					// While we're synchronized, copy the singleton cache
					// to a thread local cache
					if (result!=null) {
						updateThreadSingletonCache();
					}
				}
				break;
			}
			case CLIENT_MANAGED: {
				// No caching; just instantiate a new object in the caller
				result=null;
				break;
			}
			default: {
				throw new UnsupportedOperationException(
					"Scope \""+scope+"\" not supported");
			}
		}

		return (S)result;
	}


	/**
	 *
	 *
	 * @param serviceClass
	 * @param serviceClass
	 * @param scope
	 * @return
	 */
	private <S> boolean checkCacheConsistency(
		Class<S> serviceClass, Object service, Scope scope) {

		if (service!=null &&
			!serviceClass.isAssignableFrom(service.getClass())) {
			// Some horrible cache consistency problem due to my own
			// bug somewhere
			assert false:
				"Class in cache in scope "+scope+" is not an instance of "+
				serviceClass.getName()+" (cached instance: "+
				service.getClass()+")";

			boolean assertsEnabled=false;
			assert assertsEnabled=true; // Intentional side effect!
			if (!assertsEnabled) {
				System.err.println(
					"Class in cache in scope "+scope+" is not an instance of "+
					serviceClass.getName()+" (cached instance: "+
					service.getClass()+
					"). Request for service is being ignored.");
			}

			// Pretend we don't know about this object
			return false;
		}
		else {
			return true;
		}
	}


	/**
	 * Copy the global singleton map into a copy for the local thread to
	 * avoid synchronization under steady state
	 *
	 */
	private void updateThreadSingletonCache() {
		// Assumes that we are synchronized by the caller
		Map<Class,Object> threadLocalCache=new HashMap<Class,Object>();
		threadLocalCache.putAll(APPLICATION_OBJECTS);
		threadApplicationObjectCache.set(threadLocalCache);
	}

	/**
	 * This method assumes that it is externally synchronized! Access
	 * to singleton collections is otherwise unsafe.
	 *
	 */
	private <S> boolean cacheService(Class<S> serviceClass,
		S service, Scope scope) {

		if (serviceClass==null)
			throw new IllegalArgumentException(
				"Parameter \"serviceClass\" cannot be null");

		if (service==null)
			throw new IllegalArgumentException(
				"Parameter \"serviceProviderClass\" cannot be null");

		if (scope==null)
			throw new IllegalArgumentException(
				"Parameter \"scope\" cannot be null");

		boolean result=false;

		switch (scope){

			case REQUEST: {
				if (!inRequest()) {
					throw new IllegalStateException(
						"Cannot create service with scope "+scope+
						" outside of request");
				}

				assert threadRequestObjects.get()!=null;

				Object cachedService=
					threadRequestObjects.get().get(serviceClass);
				if (cachedService!=null) {
					// Check that we're being asked to cache the same object,
					// to detect consistency problems.
					if (cachedService!=service) {
						throw new IllegalStateException(
							"Tried to cache object "+service+
							" in request scope but a different object "+
							cachedService+" already exists in the cache");
					}
					else {
						// Object is already in cache; do nothing
						result=false;
					}
				}
				else {
					// Object not in cache; add it
					threadRequestObjects.get().put(serviceClass,service);
					result=true;
				}

				break;
			}

			case APPLICATION: {
				// DANGER! This is unsynchronized assuming that the caller
				// has synchronized

				// Check that we're being asked to cache the same
				// object, to detect consistency problems
				Object cachedService=
					APPLICATION_OBJECTS.get(serviceClass);
				if (cachedService!=null) {
					if (cachedService!=service) {
						throw new IllegalStateException(
							"Tried to cache object "+service+
							" in singleton scope but a different object "+
							cachedService+" already exists in the cache");
					}
					else {
						// Object is already in cache; do nothing
						result=false;
					}
				}
				else {
					// Object not in cache; add it
					APPLICATION_OBJECTS.put(serviceClass,service);
					result=true;
				}

				break;
			}

			case CLIENT_MANAGED:
				// We don't cache client-managed objects, so do nothing.
				// A new object will be created each time.
				result=false;
				break;

			default: {
				throw new UnsupportedOperationException(
					"Scope \""+scope+"\" not supported");
			}
		}

		return result;
	}




	////////////////////////////////////////////////////////////////////////////
	// Finder methods
	////////////////////////////////////////////////////////////////////////////

	/**
	 *
	 *
	 */
	public void addFinder(ServiceFinder finder) {
		if (finder==null) {
			throw new IllegalArgumentException(
				"Parameter \"finder\" cannot be null");
		}

		try {
			ServiceFinder.verifyFieldsAllowed(ServiceFinder.class,
				finder.getClass(),Scope.CLIENT_MANAGED);
		}
		catch (Exception e) {
			throw new IllegalArgumentException(
				"To avoid potential state leakage, service finders must "+
				"specifically whitelist their declared fields using the "+
				AllowField.class.getName()+" annotation",e);
		}

		// The call to add should be synchronized at the list level
		FINDERS.get().add(finder);
	}


	/**
	 * Returns an array of finders. This array is a copy and may be modified.
	 * Any changes to finders referenced by this array MUST be done in a
	 * threadsafe way, as they may still be in use.
	 *
	 */
	public ServiceFinder[] getFinders() {
		List<ServiceFinder> finders=FINDERS.get();

		// Assuming here that the toArray() method is fully synchronized...
		return finders.toArray(new ServiceFinder[0]);
	}




	////////////////////////////////////////////////////////////////////////////
	// Application and request lifecycle methods
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Marks the beginning of a "request", meaning a scope that will be used
	 * to create and manage objects marked as request-scoped.
	 *
	 */
	public void beginRequest() {

		ensureApplication();

		if (threadRequestObjects.get()!=null &&
			!threadRequestObjects.get().isEmpty()) {
			throw new IllegalStateException(
				"Another request was begun while a request was outstanding");
		}

		threadRequestObjects.set(new HashMap<Class,Object>());
	}


	/**
	 *
	 *
	 */
	public boolean inRequest() {
		return threadRequestObjects.get()!=null;
	}


	/**
	 *
	 *
	 */
	protected <O> O getRequestObject(Class<O> clazz) {
		return null;
	}
	

	/**
	 * 
	 * 
	 * @return	null if no exceptions occurred finalizing request-scope 
	 *			objects, or a list of Throwables that occurred. Request-scoped
	 *			ojbects have already been cleaned up when this method returns.
	 */
	public List<Throwable> endRequest() {

		List<Throwable> result=null;

		if (threadRequestObjects.get()!=null &&
			!threadRequestObjects.get().isEmpty()) {

			// Finalize all request-scoped objects
			result=notifyRequestObjects();

			// TODO: Do we want a way for the caller to suggest that any
			// exceptions that occur should prevent the request from closing?
			// Tricky thing is that we would be basically need a two-phase
			// commit protocol for objects so that they all finalize or none
			// do and we can retry.

			// Remove the request map and all the objects. Absence of this map
			// flag indicates whether a request has already begun.
			threadRequestObjects.get().clear();

			// Set map to null
			threadRequestObjects.remove();
		}

		return result;
	}


	/**
	 *
	 *
	 */
	protected List<Throwable> notifyRequestObjects() {

		List<Throwable> exceptions=null;

		for (Object object: threadRequestObjects.get().values()) {
			if (object instanceof RequestCompletionListener) {
				// Call requestComplete() on each object. Swallow any 
				// exceptions and return them.
				try {
					((RequestCompletionListener)object).requestComplete();
				}
				catch (Throwable e) {
					if (exceptions==null)
						exceptions=new ArrayList<Throwable>();
					exceptions.add(e);
				}
			}
		}

		return exceptions;
	}


	/**
	 * Marks the beginning of a "request", meaning a scope that will be used
	 * to create and manage objects marked as request-scoped.
	 *
	 */
	public void beginApplication() {
		// Check if currently in application state
		if (inApplication())
			throw new IllegalStateException(
				"The application has already been started");

		// Check if we've ever been in the application state. Note, this
		// check is currently unnecessary because we've removed the
		// endApplication() method.
		if (applicationStarted!=null && !applicationStarted)
			throw new IllegalStateException(
				"This instance has already been in the "+
				"application state and cannot be used again. See the "+
				Services.class.getName()+".reset() method.");

		// If a request already exists, do nothing. Alternatively, create the
		// object map for request-scoped objects.
		applicationStarted=Boolean.TRUE;
	}


	/**
	 *
	 *
	 */
	public boolean inApplication() {
		return applicationStarted!=null && applicationStarted;
	}


	/**
	 *
	 *
	 */
	/*pkg*/ final void ensureApplication() {
		if (!inApplication()) {
			throw new IllegalStateException(
				"This instance is not in the scope of an "+
				"application and cannot be used. See the "+
				Services.class.getName()+".beginApplication() method.");
		}
	}


	/*
	 * While conceptually, it might be useful to allow the instance and all
	 * the singleton instances to know when an application was ending, in
	 * practice this leads to a number of subtle bugs. For example, one thread
	 * could end the application while other threads are still processing
	 * requests. These requests would then fail when they tried to obtain
	 * services from the Services factory. This may be desirable but may also
	 * be unexpected.
	 *
	 * In order to address something like this would require the ability to
	 * allow existing in-request threads to complete their requests without
	 * letting other threads begin requests. This is possible, but starts
	 * getting heavyweight for what we need at the moment.
	 *
	 * In addition, allowing an application to be ended means we need to
	 * do some synchronization when getting the Services instance. Right now
	 * we can avoid that because of the simple use of an atomic reference in
	 * reset().
	 *
	 * Also note that if singletons could respond to application ending
	 * events, they would need to be coded to know much more about their
	 * use from requests and would need to allow access only from outstanding
	 * request threads. While it may be able to greatly assist these instances
	 * by creating helper methods in the Services class (e.g.
	 * shouldAllow(<current thread>), this enormously complicates writing
	 * services and violates the spirit of this library.
	 */


//	/**
//	 *
//	 *
//	 * @return	null if no exceptions occurred finalizing request-scope
//	 *			objects, or a list of Throwables that occurred. Request-scoped
//	 *			ojbects have already been cleaned up when this method returns.
//	 */
//	public List<Throwable> endApplication() {
//
//		if (applicationStarted==Boolean.FALSE) {
//			throw new IllegalStateException(
//				"The application state for this instance has already "+
//				"been ended and cannot be ended again. See stack "+
//				"trace to find from where this method was originally called.",
//				endApplicationStackTrace);
//		}
//
//		ensureApplication();
//
//		List<Throwable> result=null;
//
//		// Save a stack trace of the original thread that called this method
//		// for debugging when this method is called more than once
//		endApplicationStackTrace=new Exception(
//			"Stack trace of first endApplication() caller at "+new Date());
//
//		// Finalize all singleton-scoped objects
//		result=notifyApplicationObjects();
//
//		// TODO: Do we want a way for the caller to suggest that any
//		// exceptions that occur should prevent the request from closing?
//		// Tricky thing is that we would be basically need a two-phase
//		// commit protocol for objects so that they all finalize or none
//		// do and we can retry. Not worth it.
//
//		// Remove the request map and all the objects. Absence of this map
//		// flag whether a request has already begun.
//		APPLICATION_OBJECTS.clear();
//		threadRequestObjects=null;
//		threadApplicationObjects=null;
//		threadServiceProviderClasses=null;
//
//		return result;
//	}

//	/**
//	 *
//	 *
//	 */
//	protected List<Throwable> notifyApplicationObjects() {
//
//		List<Throwable> exceptions=null;
//
//		for (Object object: APPLICATION_OBJECTS.values()) {
//			if (object instanceof ApplicationCompletionListener) {
//				// Call applicationComplete() on each object. Swallow any
//				// exceptions and return them.
//				try {
//					((ApplicationCompletionListener)object)
//						.applicationComplete();
//				}
//				catch (Throwable e) {
//					if (exceptions==null)
//						exceptions=new ArrayList<Throwable>();
//					exceptions.add(e);
//				}
//			}
//		}
//
//		return exceptions;
//	}





	////////////////////////////////////////////////////////////////////////////
	// White-box testing methods
	////////////////////////////////////////////////////////////////////////////

	/*default*/ int getNumApplicationObjects() {
		return APPLICATION_OBJECTS.size();
	}

	/*default*/ int getNumRequestObjects() {
		Map map;
		return (map=threadRequestObjects.get())!=null ? map.size() : 0;
	}




	////////////////////////////////////////////////////////////////////////////
	// Static methods
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the current instance
	 *
	 */
	public static Services getInstance() {
		return INSTANCE.get();
	}


	/**
	 * Removes the current instance and all cached providers and finders. Note,
	 * after resetting, the new instance will have no finders, nor will the
	 * application been started.
	 *
	 */
	public static void reset() {
		INSTANCE.getAndSet(new Services());
		// TODO: Should we clean up anything here?
	}


	/**
	 * Initializes and adds the default finder
	 *
	 */
	public void initializeDefaultFinder() {
		this.addFinder(new MetaInfServiceFinder(this));
	}


//	/**
//	 *
//	 *
//	 */
//	public static class $<S> extends Object {
//		public S get() {
//			// TODO: Look up Type from declaration and use it to get instance
//
////    class Processor<T> {
////    }
////
////    class StringProcessor<S extends String> extends Processor<S> {
////    }
////
////    public void whatKindOfProcessorIsItAnyway(Processor p) {
////        Type processorType = p.getClass().getTypeParameters()[0].getBounds()[0];
////        if ( String.class.equals(processorType) ) {
////            System.out.println("I'm a string processor");
////        } else {
////            System.out.println("I process these: " + processorType);
////        }
////    }
////
//
//			return null;
//
////			String clazz=getClass().getTypeParameters()[0].getName();
////System.out.println("Name: "+clazz);
//////			if (clazz instanceof Class)
//////				return (S)Services.getInstance().get((Class)clazz);
//////			else
////				return null;
//		}
//	}




	////////////////////////////////////////////////////////////////////////////
	// Fields
	////////////////////////////////////////////////////////////////////////////

	public static final String PROPERTY_INITIALIZE_DEFAULT=
		Services.class.getName()+".initialize";

	private static final AtomicReference<Services> INSTANCE=
		new AtomicReference<Services>();
	private final AtomicReference<List<ServiceFinder>> FINDERS=
		new AtomicReference<List<ServiceFinder>>(
			Collections.synchronizedList(new ArrayList<ServiceFinder>()));

	// Thread-local cache
	private ThreadLocal<Map<Class,Object>> threadRequestObjects=
		new ThreadLocal<Map<Class,Object>>();
	private ThreadLocal<Map<Class,Object>> threadApplicationObjectCache=
		new ThreadLocal<Map<Class,Object>>();

	// Global cache. This is declared package-private so that we can
	// synchronize with the cache when creating application objects in the
	// default finder.
	/*pkg*/ final Map<Class,Object> APPLICATION_OBJECTS=
		Collections.synchronizedMap(new HashMap<Class,Object>());

	private Boolean applicationStarted;
//	private Exception endApplicationStackTrace;

	static {
		Services instance=new Services();

		String shouldInitialize=System.getProperty(
			PROPERTY_INITIALIZE_DEFAULT,"true");
		if (Boolean.parseBoolean(shouldInitialize)) {
			// Create the default instance
			instance.addFinder(new MetaInfServiceFinder(instance));
			instance.beginApplication();
		}

		INSTANCE.set(instance);
	}
}
