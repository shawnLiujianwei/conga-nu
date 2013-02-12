package com.conga.nu;

/**
 * Indicates a problem instantiate a service (provider) class
 *
 * @author Todd Fast
 */
public class ServiceProviderNotFoundException
	extends ServiceInstantiationException
{
	public ServiceProviderNotFoundException(Class serviceType) {
		super(serviceType);
	}

	public ServiceProviderNotFoundException(Class serviceType, String message) {
		super(serviceType,message);
	}

	public ServiceProviderNotFoundException(Class serviceType, Throwable e) {
		super(serviceType,e);
	}

	public ServiceProviderNotFoundException(Class serviceType, String message,
		Throwable e) {
		super(serviceType,message,e);
	}

	private static final long serialVersionUID=1L;
}
