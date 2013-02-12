package com.conga.nu;

/**
 *
 * @author Todd Fast
 */
public abstract class ServiceProviderFactory<S> {

	/**
	 *
	 *
	 */
	public abstract S createInstance();
}
