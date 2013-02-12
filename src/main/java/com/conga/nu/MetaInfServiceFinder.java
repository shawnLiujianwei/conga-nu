package com.conga.nu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Finds and instantiates service providers by inspecting META-INF/services
 * configuration files
 *
 * @author Todd Fast
 */
public class MetaInfServiceFinder extends ServiceFinder {

	/**
	 *
	 * 
	 * @param container
	 */
	public MetaInfServiceFinder(Services container) {
		super();
		this.container=container;
	}


	/**
	 *
	 *
	 */
	protected Services getContainer() {
		return container;
	}


	/**
	 *
	 *
	 * @param <S>
	 * @param serviceClass
	 * @param scope
	 * @return
	 * @throws IncompatibleScopeException
	 */
	@Override
	public <S> Result<S> find(Class<S> serviceClass)
		throws ServiceInstantiationException {

		Result<S> result=null;
//		S provider=null;
//		Scope scope=null;

		// If it still doesn't exist, get the provider class, see what scope
		// it prefers, and create it in that scope. Syncyhronization should
		// only be necessary later in the code path when access to the shared
		// caches is necessary (most should be cached in thread-local caches
		// under steady state.
		final ServiceProviderInfo<S> info=findProviderInfo(serviceClass);
		if (info!=null) {
//			// Request scope takes precedence when present
//			if (info.isScopeAllowed(Scope.REQUEST)) {
//				scope=Scope.REQUEST;
//				provider=instantiateNonApplicationService(info,scope);
//			}
//			else
//			if (info.isScopeAllowed(Scope.APPLICATION)) {
//				scope=Scope.APPLICATION;
//				provider=instantiateApplicationService(info);
//			}
//			else
//			if (info.isScopeAllowed(Scope.CLIENT_MANAGED)) {
//				scope=Scope.CLIENT_MANAGED;
//				provider=instantiateNonApplicationService(info,scope);
//			}
//			else {
//				// Bug in my code: unaccounted for scope type
//				assert false: "Cannot create service in unknown scope";
//				throw new IllegalArgumentException(
//					"Cannot create service in unknown scope");
//			}

			Scope scope=null;

			// Request scope takes precedence when present
			if (info.isScopeAllowed(Scope.REQUEST)) {
				scope=Scope.REQUEST;
			}
			else
			if (info.isScopeAllowed(Scope.APPLICATION)) {
				scope=Scope.APPLICATION;
			}
			else
			if (info.isScopeAllowed(Scope.CLIENT_MANAGED)) {
				scope=Scope.CLIENT_MANAGED;
			}
			else {
				// Bug in my code: unaccounted for scope type
				throw new IllegalArgumentException(
					"Cannot determine scope of service in unknown scope");
			}

			final Scope _scope=scope;

			ServiceProviderFactory<S> factory=
				new ServiceProviderFactory<S>() {
					@Override
					public S createInstance() {
						S provider=null;

						// Request scope takes precedence when present
						if (_scope==Scope.REQUEST) {
							provider=
								instantiateNonApplicationService(info,_scope);
						}
						else
						if (_scope==Scope.APPLICATION) {
							provider=instantiateApplicationService(info);
						}
						else
						if (_scope==Scope.CLIENT_MANAGED) {
							provider=
								instantiateNonApplicationService(info,_scope);
						}
						else {
							// Bug in my code: unaccounted for scope type
							throw new IllegalArgumentException(
								"Cannot create service in unknown scope: "+
								_scope);
						}

						return provider;
					}
				};

			result=new Result<S>(serviceClass,factory,scope);
		}

		return result;
	}


	/**
	 *
	 * @param <S>
	 * @param info
	 * @param scope
	 * @return
	 * @throws ServiceInstantiationException
	 */
	private <S> S instantiateNonApplicationService(
		ServiceProviderInfo<S> info, Scope scope)
		throws ServiceInstantiationException
	{
		if (scope==Scope.APPLICATION)
			throw new IllegalArgumentException(
				"Use method instantiateSingletonService() instead");

		if (scope==Scope.REQUEST && !getContainer().inRequest()) {
			throw new ServiceInstantiationException(info.getServiceClass(),
				"Cannot instantiate service provider class "+
				info.getProviderClass().getName()+" of service type "+
				info.getServiceClass().getName()+" outside in requested "+
				"scope "+Scope.REQUEST+". See docs for method "+
				"Services.getInstance().beginRequest().");
		}

		return _instantiateService(info,scope);
	}


	/**
	 *
	 * @param <S>
	 * @param info
	 * @return
	 * @throws ServiceInstantiationException
	 */
	private <S> S instantiateApplicationService(
		ServiceProviderInfo<S> info)
		throws ServiceInstantiationException
	{
		synchronized (getContainer().APPLICATION_OBJECTS) {

			// Look in the cache again now that we're synchronized because
			// a thread might've created an object after we looked in the cache
			// and before we got here
			S result=getContainer().findCachedService(info.getServiceClass(),
				Scope.APPLICATION);

			// If didn't find it, create a new one
			if (result==null)
				result=_instantiateService(info,Scope.APPLICATION);

			return result;
		}
	}


	/**
	 * This method assumes that it is externally synchronized! Access
	 * to singleton collections is otherwise unsafe.
	 *
	 */
	private <S> S _instantiateService(
		ServiceProviderInfo<S> info, Scope scope)
		throws ServiceInstantiationException
	{
		// DANGER! This method assumes that it is externally synchronized!
		// Access to application collections is otherwise unsafe.

		if (info==null)
			throw new IllegalArgumentException(
				"Parameter \"info\" cannot be null");

		S result=null;

		// This should never happen because we're looking in the cache after
		// synchronizing in instantiateApplicationService() above. Without
		// looking in that cache first we face a race condition after reducing
		// the amount of synchronization due to thread-local caching
		// optimization.
		assert (result=getContainer().findCachedService(
			info.getServiceClass(),scope))==null:
			"Instantiating a service in scope "+scope+" but an instance "+
			"is already cached in that scope (cached instance: "+result+")";

		if (scope==Scope.REQUEST) {
			assert getContainer().inRequest():
				"Cannot create service with scope "+Scope.REQUEST+
				" outside of request";
		}

		// The requested scope must be compatible with the provider's scope
		// declarations
		if (!info.isScopeAllowed(scope)) {
			throw new IncompatibleScopeException(
				info.getServiceClass(),
				info.getProviderClass(),
				scope);
		}

		Class<S> serviceClass=info.getServiceClass();
		Class<? extends S> serviceProviderClass=info.getProviderClass();

		if (scope==Scope.APPLICATION) {
			verifyIsOuterClass(serviceClass,serviceProviderClass,scope);
			verifyFieldsAllowed(serviceClass,serviceProviderClass,scope);
		}

		try {
			// Create the provider
			Constructor<? extends S> constructor=
				serviceProviderClass.getConstructor();
			result=constructor.newInstance();

//			// Cache the provider in the requested scope
//			cacheService(serviceClass,result,scope);
		}
		catch (Exception e) {
			ServiceInstantiationException ex=
				new ServiceInstantiationException(serviceClass,
					"Could not instantiate service provider class "+
					serviceProviderClass.getName()+" of service type "+
					serviceClass.getName()+
					(e.getCause()!=null && e.getCause()!=e
						? ": "+e.getCause() : ""),
					(e.getCause()!=null && e.getCause()!=e)
						? e.getCause() : e);

			throw ex;
		}

		return result;
	}




	////////////////////////////////////////////////////////////////////////////
	// Provider lookup methods
	////////////////////////////////////////////////////////////////////////////

	/**
	 *
	 *
	 * @param serviceClassName
	 * @param providerClassName
	 */
	public void addProviderOverride(String serviceClassName,
		String providerClassName) {

		synchronized (providerOverrides) {
			ensureLegalProviderOverrideChange();
			providerOverrides.put(serviceClassName,providerClassName);
		}
	}


	/**
	 *
	 *
	 * @param serviceClassName
	 */
	public void removeProviderOverride(String serviceClassName) {

		synchronized (providerOverrides) {
			ensureLegalProviderOverrideChange();
			providerOverrides.remove(serviceClassName);
		}
	}


	/**
	 *
	 *
	 */
	private void ensureLegalProviderOverrideChange() {
		if (getContainer().inApplication()) {
			throw new IllegalStateException(
				"The list of service provider overrides may not be modified "+
				"after entering the application state. See "+
				Services.class.getName()+".beginApplication().");
		}
	}


	/**
	 * Allows clients to force use of particular service provider
	 * implementations, regardless of which provider classes are available on
	 * the classpath. Any changes to this map will only take effect for
	 * creation of providers after modification. This feature should be used
	 * carefully
	 *
	 * @return	Map of service interface class names to service provider class
	 *			names. This map may not be modified directly.
	 */
	public Map<String,String> getProviderOverrides() {
		return Collections.unmodifiableMap(providerOverrides);
	}


	/**
	 *
	 *
	 * @param serviceClassName
	 * @param providerClassName
	 */
	public void addProviderBlacklist(String serviceClassName,
		String providerClassName) {

		synchronized (providerBlacklist) {

			ensureLegalProviderBlacklistChange();

			// Get read-only list of providers
			Set<String> providers=providerBlacklist.get(serviceClassName);

			// Create a new set and add all existing providers to it
			Set<String> newSet=new HashSet<String>();
			if (providers!=null)
				newSet.addAll(providers);

			newSet.add(providerClassName);

			providerBlacklist.put(serviceClassName,
				Collections.unmodifiableSet(newSet));
		}
	}


	/**
	 *
	 *
	 * @param serviceClassName
	 */
	public void removeProviderBlacklist(String serviceClassName) {

		synchronized (providerBlacklist) {

			ensureLegalProviderBlacklistChange();

			Set<String> providers=providerBlacklist.get(serviceClassName);
			if (providers!=null)
				providers.remove(serviceClassName);
		}
	}


	/**
	 *
	 *
	 */
	private void ensureLegalProviderBlacklistChange() {
		if (getContainer().inApplication()) {
			throw new IllegalStateException(
				"The service provider blacklist may not be modified "+
				"after entering the application state. See "+
				Services.class.getName()+".beginApplication().");
		}
	}


	/**
	 * Allows clients to deny use of particular service provider
	 * implementations, regardless of which provider classes are available on
	 * the classpath. Any changes to this map will only take effect for
	 * creation of providers after modification. This feature should be used
	 * carefully
	 *
	 * @return	Map of service interface class names to service provider class
	 *			names. This map may not be modified directly.
	 */
	public Map<String,Set<String>> getProviderBlacklist() {
		return Collections.unmodifiableMap(providerBlacklist);
	}


	/**
	 *
	 *
	 * @param <S>
	 * @param serviceClass
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <S> ServiceProviderInfo<S> findProviderInfo(Class<S> serviceClass) {

		ServiceProviderInfo<S> result=null;

		// Try to avoid synchronization by looking in our thread-local cache,
		// which is just a (potential) subset of the global service provider
		// class cache. If not found here, proceed

		if (threadServiceProviderInfoCache.get()!=null) {
			result=threadServiceProviderInfoCache.get().get(serviceClass);
			if (result!=null)
				return result;
		}

		synchronized (SERVICE_PROVIDER_INFO) {

			result=SERVICE_PROVIDER_INFO.get(serviceClass);
			if (result!=null) {
				updateThreadServiceProviderClassCache();
				return result;
			}

			// TODO: Do we want to think about using Thread's context class
			// loader for those cases where the Services instance may be
			// instantiated more than once? Right now, there is only a single
			// global instance, so our own class loader should be sufficient.
			// Also, consider that the context class loader would only be
			// appropriate for request-scoped services.
			List<Class<? extends S>> providerClasses=
				findProviders(serviceClass,getClass().getClassLoader());

			// We have a bit of a problem here, since there is no guarantee
			// that the one provider that we choose to return will be
			// instantiatable. Take a chance for now and solve this problem
			// later. (We might want to introduce an annotation of the priority
			// so that we can look for providers in a particular order.)
			for (Class<? extends S> providerClass: providerClasses) {
				ServiceProvider annotation=
					providerClass.getAnnotation(ServiceProvider.class);

				if (annotation!=null) {

					result=new ServiceProviderInfo<S>(
						serviceClass,providerClass);

					// Cache the provider class so we can create more
					// instances later
					SERVICE_PROVIDER_INFO.put(serviceClass,result);

					updateThreadServiceProviderClassCache();

					// Put a copy on the local thread's cache
					break;
				}
				else {
					// Ignore implementation class because it's not annotated
					System.out.println("Ignoring service provider class "+
						providerClass.getName()+" for service "+
						serviceClass.getName()+" because it has not been "+
						"annotated as a provider. See "+
						ServiceProvider.class.getName()+".");
				}
			}

			return result;
		}
	}


	/**
	 * Copy the global provider class map into a copy for the local thread
	 * to avoid synchronization under steady state
	 *
	 */
	private void updateThreadServiceProviderClassCache() {
		// Assumes that we are synchronized by the caller
		Map<Class,ServiceProviderInfo> threadLocalCache=
			new HashMap<Class,ServiceProviderInfo>();
		threadLocalCache.putAll(SERVICE_PROVIDER_INFO);
		threadServiceProviderInfoCache.set(threadLocalCache);
	}


	/**
	 * Find all the service provider classes for the service
	 *
	 * Includes fix for java.util.ServiceLoader bug
	 * http://bugs.sun.com/view_bug.do?bug_id=6587593
	 *
	 */
	@SuppressWarnings("unchecked")
	public <S> List<Class<? extends S>> findProviders(
		Class<S> serviceClass, ClassLoader classLoader) {

		getContainer().ensureApplication();

		if (serviceClass==null)
			throw new IllegalArgumentException(
				"Parameter \"serviceClass\" cannot be null");

		if (classLoader==null)
			throw new IllegalArgumentException(
				"Parameter \"classLoader\" cannot be null");

		final String PREFIX = "META-INF/services/";

		String serviceName=serviceClass.getName();

//		if (Log.isLevelEnabled(Services.class, Level.FINE)) {
//			Log.log(Services.class, Level.FINE, "ServiceName=" + serviceName);
//		}

		List<Class<? extends S>> providerClasses=
			new ArrayList<Class<? extends S>>();

		// Try to find a provider that has been specifically configured
		String overrideClassName=getProviderOverrides().get(serviceName);
		if (overrideClassName!=null) {
			try {
				// Load the class and add to our result list
				Class<? extends S> clazz=(Class<? extends S>)
					classLoader.loadClass(overrideClassName);

				// The above statement doesn't throw an exception even if the
				// class is not of type S. We must manually check that the
				// type is correct.
				if (!serviceClass.isAssignableFrom(clazz)) {
					throw new ClassCastException(
						"Service provider of type "+clazz.getName()+
						" must implement service interface "+serviceName);
				}

				// Make sure the class is a provider
				if (!isServiceProvider(clazz)) {
					throw new ServiceInstantiationException(serviceClass,
						"Overridden provider class "+overrideClassName+
						" was not annotated as a service (service type: "+
						serviceClass.getName()+")");
				}

				providerClasses.add(clazz);
			}
			catch (ClassNotFoundException e) {
				throw new ServiceInstantiationException(serviceClass,
					"Overridden provider class "+overrideClassName+
					" could not be found for service "+
					serviceClass.getName()+". Check that the class name is "+
					"correct and on the classpath.",e);
			}
			catch (ClassCastException e) {
				throw new ServiceInstantiationException(serviceClass,
					"Overridden provider class "+overrideClassName+
					" for service "+serviceClass.getName()+" does not "+
					"implement the service interface",e);
			}
		}
		else {
			try {
				// Grab all service declaration files on the class loader
				Enumeration<URL> urls=
					classLoader.getResources(PREFIX+serviceName);

				while (urls.hasMoreElements()) {
					URL url=urls.nextElement();
		//			if (Log.isLevelEnabled(Services.class, Level.FINE)) {
		//				Log.log(Services.class, Level.FINE, "URL=" + url);
		//			}

					URLConnection uc=null;
					InputStream in=null;
					BufferedReader reader=null;

					try {
						uc=url.openConnection();
						uc.setUseCaches(false);
						in=uc.getInputStream();
						reader=new BufferedReader(
							new InputStreamReader(in,"utf-8"));

						List<String> classNames=new ArrayList<String>();

						// This implementation is stupid and convoluted;
						// leave it some JDK engineer...
						int lineCount=1;
						while (lineCount >= 0) {
							lineCount=parseLine(serviceClass,url,
								reader,lineCount,classNames);
						}

						Set<String> blacklist=
							getProviderBlacklist().get(serviceName);

						for (String className: classNames) {
							Class<? extends S> clazz=null;
							try {
								// Ignore blacklisted providers
								if (blacklist!=null &&
									blacklist.contains(className))
									continue;

								// Load the class and add to our result list
								clazz=(Class<? extends S>)
									classLoader.loadClass(className);

								// Check that the type is consistent
								if (!serviceClass.isAssignableFrom(clazz)) {
									throw new ClassCastException(
										"Service provider of type "+
										clazz.getName()+" must implement "+
										"service interface "+serviceName);
								}

								// We only care if it's been annotated as
								// a provider
								if (isServiceProvider(clazz)) {
									providerClasses.add(clazz);
									Logger.getLogger(
										Services.class.getName()).info(
										"Instantiated provider "+className+
										" for service "+serviceName);
								}
								else {
									Logger.getLogger(
										Services.class.getName()).info(
										"Warning: Provider "+
										"class "+className+" for service "+
										serviceName+" is not annotated with @"+
										ServiceProvider.class.getName()+" and "+
										"has been ignored");
								}
							}
							catch (ClassNotFoundException e) {
								// Ignore this provider
								e.printStackTrace();
							}
							catch (ClassCastException e) {
								// Ignore this provider
								System.err.println("Ignoring provider "+
									clazz.getName());
								e.printStackTrace();
								// TODO: Log
							}
						}

						// Sort the classes according to their priority
						// declarations
						Collections.sort(providerClasses,PRIORITY_COMPARATOR);
					}
					catch (IOException e) {
						// Ignore this file's providers
						e.printStackTrace();
						// TODO: Log
					}
					finally {
						try {
							if (reader!=null)
								reader.close();
						}
						catch (IOException e) {
							// Ignore
							e.printStackTrace();
							// TODO: Log
						}

						try {
							if (in!=null)
								in.close();
						}
						catch (IOException e) {
							// Ignore
							e.printStackTrace();
							// TODO: Log
						}
					}
				}
			}
			catch (IOException e) {
				// Ignore providers for this class
				e.printStackTrace();
				// TODO: Log
			}
		}

		return providerClasses;
	}

	/**
	 * Parse a single line from the given configuration file, adding the name
	 * on the line to the names list. Adapted from java.util.ServiceLoader.
	 *
	 */
	private int parseLine(Class service, URL u, BufferedReader r, final int lc,
			List<String> names)
			throws IOException, ServiceConfigurationError {

		String ln = r.readLine();
//		if (Log.isLevelEnabled(Services.class, Level.FINE)) {
//			Log.log(Services.class, Level.FINE, "PROVIDER=" + ln);
//		}
		if (ln == null) {
			return -1;
		}
		int ci = ln.indexOf('#');
		if (ci >= 0) {
			ln = ln.substring(0, ci);
		}
		ln = ln.trim();
		int n = ln.length();
		if (n != 0) {
			if ((ln.indexOf(' ') >= 0) || (ln.indexOf('\t') >= 0)) {
				// fail(service, u, lc, "Illegal configuration-file syntax");
				return lc;
			}
			int cp = ln.codePointAt(0);
			if (!Character.isJavaIdentifierStart(cp)) {
				// fail(service, u, lc, "Illegal provider-class name: " + ln);
				return lc;
			}
			for (int i = Character.charCount(cp); i < n;
				i += Character.charCount(cp)) {

				cp = ln.codePointAt(i);
				if (!Character.isJavaIdentifierPart(cp) && (cp != '.')) {
					// fail(service, u, lc, "Illegal provider-class name: " + ln);
					return lc;
				}
			}
			if (!names.contains(ln)) {
				names.add(ln);
			}
		}
		return lc + 1;
	}

	private void fail(Class service, String msg)
			throws ServiceConfigurationError {
		throw new ServiceConfigurationError(service.getName() + ": " + msg);
	}

//	private void fail(Class service, URL u, int line, String msg)
//			throws ServiceConfigurationError {
//		fail(service, u + ":" + line + ": " + msg);
//	}

	private boolean isServiceProvider(Class<?> clazz) {
		return clazz.getAnnotation(ServiceProvider.class)!=null;
	}


	////////////////////////////////////////////////////////////////////////////
	// White-box testing methods
	////////////////////////////////////////////////////////////////////////////

	/*default*/ Map<Class,ServiceProviderInfo> getServiceProviderInfo() {
		return Collections.unmodifiableMap(SERVICE_PROVIDER_INFO);
	}



	////////////////////////////////////////////////////////////////////////////
	// Inner type
	////////////////////////////////////////////////////////////////////////////

	/**
	 *
	 * @param <S>
	 */
	protected static class ServiceProviderInfo<S> {

		public ServiceProviderInfo(Class<S> serviceClass,
			Class<? extends S> providerClass) {

			super();

			if (serviceClass==null)
				throw new IllegalArgumentException(
					"Parameter \"serviceClass\" cannot be null");

			if (providerClass==null)
				throw new IllegalArgumentException(
					"Parameter \"providerClass\" cannot be null");

			this.serviceClass=serviceClass;
			this.providerClass=providerClass;
			introspectScopes();
		}

		public Class<S> getServiceClass() {
			return serviceClass;
		}

		public Class<? extends S> getProviderClass() {
			return providerClass;
		}

		protected Set<Scope> getScopes() {
			return scopes;
		}

		public boolean isScopeAllowed(Scope scope) {
			return getScopes().contains(scope);
		}

		protected void introspectScopes() {
			// Get the scopes from the provider class
			assert serviceClass!=null;
			assert providerClass!=null;

			// Cache the scopes from the provider class
			ServiceProvider serviceAnnotation=
				providerClass.getAnnotation(ServiceProvider.class);

			// If no service annotation, then throw an exception. Later we
			// can think about how to manage classes without annotations.
			if (serviceAnnotation==null) {
				throw new IllegalArgumentException(
					"Service provider class \"+providerClass.getName()+" +
					"\" does not declare that it is a service using the "+
					ServiceProvider.class.getName()+" annotation (service "+
					"type: "+serviceClass.getName()+")");
			}

			scopes=Collections.unmodifiableSet(
				EnumSet.copyOf(Arrays.asList(serviceAnnotation.scope())));
		}

		private Class<S> serviceClass;
		private Class<? extends S> providerClass;
		private Set<Scope> scopes;
	}




	////////////////////////////////////////////////////////////////////////////
	// Inner type
	////////////////////////////////////////////////////////////////////////////

	/**
	 *
	 *
	 * @param <Class>
	 */
	private static class PriorityComparator<C extends Class<?>>
		implements Comparator<C>, Serializable
	{
		public int compare(C c1, C c2) {

			if (c1==null && c2==null)
				return 0;

			if (c1==null)
				return 1;

			if (c2==null)
				return -1;

			ServiceProvider sp1=
				c1.getAnnotation(ServiceProvider.class);
			ServiceProvider sp2=
				c2.getAnnotation(ServiceProvider.class);

			if (sp1==null && sp2==null)
				return 0;

			if (sp1==null)
				return 1;

			if (sp2==null)
				return -1;

			if (sp2.priority()<sp1.priority())
				return -1;
			else
			if (sp2.priority()>sp1.priority())
				return 1;
			else
				return 0;
		}

		private static final long serialVersionUID=1L;
	}

	@AllowField
	private static final PriorityComparator PRIORITY_COMPARATOR=
		new PriorityComparator();

	// Thread-local cache
	@AllowField
	private ThreadLocal<Map<Class,ServiceProviderInfo>>
		threadServiceProviderInfoCache=
			new ThreadLocal<Map<Class,ServiceProviderInfo>>();

	// Global cache
	@AllowField
	private final Map<Class,ServiceProviderInfo> SERVICE_PROVIDER_INFO=
		Collections.synchronizedMap(new HashMap<Class,ServiceProviderInfo>());

	@AllowField
	private final Map<String,String> providerOverrides=
		Collections.synchronizedMap(new HashMap<String,String>());
	@AllowField
	private final Map<String,Set<String>> providerBlacklist	=
		Collections.synchronizedMap(new HashMap<String,Set<String>>());

	@AllowField
	private final Services container;
}
