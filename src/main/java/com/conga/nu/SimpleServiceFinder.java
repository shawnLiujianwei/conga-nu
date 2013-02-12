package com.conga.nu;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps existing object instances as service providers. Furnishers of this
 * finder instance must maintain a reference to it and call beginRequest()/
 * endRequest() to avoid leakage of state across requests.
 *
 * @author Todd Fast
 */
public class SimpleServiceFinder extends ServiceFinder {

	/**
	 * Returns a previously registered provider instance for the service class.
	 * Note, this method will throw a class cast exception if service providers
	 * added previously do not implement their corresponding service class.
	 *
	 * @param <S>
	 * @param serviceClass
	 * @return
	 */
	@Override
	public <S> Result<S> find(Class<S> serviceClass) {
		S result=serviceClass.cast(threadServices.get().get(serviceClass));
		if (result!=null)
			return new Result<S>(serviceClass,result,Scope.REQUEST);
		else
			return null;
	}


	/**
	 *
	 *
	 */
	public void beginRequest() {
		if (threadServices.get()!=null) {
			throw new IllegalStateException(
				"Another request was begun while a request was outstanding");
		}

		threadServices.set(new HashMap<Class,Object>());
	}


	/**
	 *
	 *
	 */
	public void beginRequest(Map<Class,Object> services) {
		beginRequest();
		addRequestServices(services);
	}


	/**
	 * Note, this method does not ensure that service providers implement
	 * service classes! Use with caution.
	 *
	 */
	public void addRequestServices(Map<Class,Object> services) {
		// If a request already exists, do nothing. Alternatively, create the
		// object map for request-scoped objects.

		Map<Class,Object> map=threadServices.get();
		if (map==null) {
			map=new HashMap<Class,Object>();
			threadServices.set(map);
		}

		// TODO: Do we really need to copy the map all the time?
		map.putAll(services);
	}


	/**
	 *
	 *
	 */
	public void addRequestServices(Object... services) {
		HashMap<Class,Object> map=new HashMap<Class,Object>();

		for (Object provider: services) {
			if (provider!=null) {
				map.put(provider.getClass(),provider);
			}
		}

		addRequestServices(map);
	}


	/**
	 *
	 *
	 * @return	null if no exceptions occurred finalizing request-scope
	 *			objects, or a list of Throwables that occurred. Request-scoped
	 *			ojbects have already been cleaned up when this method returns.
	 */
	public void endRequest() {

		if (threadServices.get()!=null) {
			// Remove the request map and all the objects. Absence of this map
			// flag indicates whether a request has already begun.
			threadServices.get().clear();

			// Set map to null
			threadServices.remove();
		}
	}




	////////////////////////////////////////////////////////////////////////////
	// Fields
	////////////////////////////////////////////////////////////////////////////

	@AllowField
	private final ThreadLocal<Map<Class,Object>> threadServices=
		new ThreadLocal<Map<Class,Object>>();
}