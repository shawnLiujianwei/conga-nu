package com.conga.nu;

/**
 * Indicates a problem instantiate a service (provider) class
 *
 * @author Todd Fast
 */
public class ServiceInstantiationException extends RuntimeException {

	public ServiceInstantiationException(Class serviceType) {
		super();

		if (serviceType==null)
			throw new IllegalArgumentException(
				"Paramter \"serviceType\" cannot be null");

		this.serviceType=serviceType;
	}

	public ServiceInstantiationException(Class serviceType, String message) {
		super(message);

		if (serviceType==null)
			throw new IllegalArgumentException(
				"Paramter \"serviceType\" cannot be null");

		this.serviceType=serviceType;
	}

	public ServiceInstantiationException(Class serviceType, Throwable e) {
		super(e);

		if (serviceType==null)
			throw new IllegalArgumentException(
				"Paramter \"serviceType\" cannot be null");

		this.serviceType=serviceType;
	}

	public ServiceInstantiationException(Class serviceType, String message,
		Throwable e) {
		super(message,e);

		if (serviceType==null)
			throw new IllegalArgumentException(
				"Paramter \"serviceType\" cannot be null");

		this.serviceType=serviceType;
	}

	public Class getServiceType() {
		return serviceType;
	}

	private static final long serialVersionUID=1L;

	private Class serviceType;
}
