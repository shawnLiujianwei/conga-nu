package com.conga.nu;

/**
 * Indicates that the provider class
 *
 * @author Todd Fast
 */
public class IncompatibleScopeException extends ServiceInstantiationException {

	public IncompatibleScopeException(Class serviceType,
		Class providerClass, Scope scope) {

		super(serviceType,message(serviceType,providerClass,scope));
		this.providerClass=providerClass;
		this.scope=scope;
	}

	private static String message(Class serviceType,
		Class providerClass, Scope scope) {

		if (serviceType==null)
			throw new IllegalArgumentException(
				"Paramter \"serviceType\" cannot be null");

		if (providerClass==null)
			throw new IllegalArgumentException(
				"Paramter \"providerClass\" cannot be null");

		if (scope==null)
			throw new IllegalArgumentException(
				"Paramter \"scope\" cannot be null");

		return "Service provider class "+providerClass.getName()+
			"declares that it cannot be instantiated within the "+scope+
			" scope (service class: "+serviceType.getName()+")";
	}

	public Class getProviderClass() {
		return providerClass;
	}

	public Scope getScope() {
		return scope;
	}

	private static final long serialVersionUID=1L;

	private Class providerClass;
	private Scope scope;
}
