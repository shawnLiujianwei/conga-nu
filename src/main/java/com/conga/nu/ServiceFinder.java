package com.conga.nu;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Finds a service provider instance for a given service type. 
 * 
 * Note, instances of ServiceFinder must be threadsafe and MUST NOT cache
 * provider instances or previous results.
 *
 * @author Todd Fast
 */
public abstract class ServiceFinder {

	/**
	 *
	 *
	 */
	protected ServiceFinder() {
		super();
	}


	/**
	 * Returns the provider for the specified service class, or null if the
	 * provider could not be found. Malformed result objects (e.g. with null
	 * values or bogus return values) will be ignored and discarded.
	 *
	 * Note, the finder MUST NOT cache provider instances or previous results.
	 */
	public abstract <S> Result<S> find(Class<S> serviceClass)
		throws ServiceInstantiationException;


	/**
	 * Ensures that application-scoped services do not have unrecognized state
	 *
	 */
	protected static <S> void verifyIsOuterClass(
		Class<S> serviceClass, Class<? extends S> providerClass, Scope scope)
		throws ServiceInstantiationException {

		// If it's an inner class, it must be declared static
		Class declaringClass=providerClass.getDeclaringClass();
		if (declaringClass!=null) {
			Class[] innerClasses=declaringClass.getDeclaredClasses();
			for (Class innerClass: innerClasses) {
				if (!Modifier.isStatic(innerClass.getModifiers())) {
					throw new ServiceInstantiationException(serviceClass,
						"Could not instantiate service provider class "+
						providerClass.getName()+" of service type "+
						serviceClass.getName()+" in scope \""+scope+
						"\" because it is an non-static inner class");
				}
			}
		}
	}


	/**
	 * Ensures that application-scoped services do not have unrecognized state
	 *
	 */
	protected static <S> void verifyFieldsAllowed(
		Class<S> serviceClass, Class<? extends S> providerClass, Scope scope)
		throws ServiceInstantiationException {

		Class clazz=providerClass;

		// Check that all fields are either static or static final
		while (clazz!=null) {
			Field[] fields=clazz.getDeclaredFields();
			for (Field field: fields) {
				// Ignore fields created by the VM
				if (field.isSynthetic())
					continue;

				if (field.getAnnotation(AllowField.class)!=null)
					continue;

				// No fields should be declared. Make an exception only for
				// final primitive fields, enums, or immutable types
				// declared in java.lang.
				if (Modifier.isFinal(field.getModifiers())) {
					if (field.getType().isPrimitive() ||
						field.getType().isEnum() ||
						field.getType()==String.class ||
						field.getType()==Character.class ||
						field.getType()==Boolean.class ||
						field.getType()==Integer.class ||
						field.getType()==Long.class ||
						field.getType()==Short.class ||
						field.getType()==Double.class ||
						field.getType()==Float.class ||
						field.getType()==Byte.class) {
						continue;
					}
				}

				throw new ServiceInstantiationException(serviceClass,
					"Could not instantiate service provider class "+
					providerClass.getName()+" of service type "+
					serviceClass.getName()+" in scope \""+scope+
					"\" because it or the superclass "+clazz.getName()+
					" contains an instance field \""+field.getName()+"\""+
					" that is not annotated with "+
					AllowField.class.getName());
			}

			clazz=clazz.getSuperclass();
		}
	}




	////////////////////////////////////////////////////////////////////////////
	// Inner type
	////////////////////////////////////////////////////////////////////////////

	/**
	 * The result of a service lookup
	 *
	 * @param <S>
	 */
	public static class Result<S> {
		/**
		 *
		 *
		 */
		public Result(Class<S> serviceClass, S provider, Scope scope) {
			super();
			this.serviceClass=serviceClass;
			this.provider=provider;
			this.scope=scope;
		}


		/**
		 *
		 *
		 */
		public Result(Class<S> serviceClass, 
			ServiceProviderFactory<S> factory, Scope scope)
		{
			super();
			this.serviceClass=serviceClass;
			this.factory=factory;
			this.scope=scope;
		}


		/**
		 *
		 *
		 */
		public final Class<S> getServiceClass() {
			return serviceClass;
		}


		/**
		 *
		 *
		 */
		public final S getProvider() {
			if (provider==null)
				initializeProvider();

			return provider;
		}


		/**
		 *
		 *
		 */
		public final Scope getScope() {
			return scope;
		}


		/**
		 *
		 *
		 */
		private void initializeProvider() {
			if (factory==null)
				return;

			synchronized (this) {
				if (factory!=null) {
					// Use the factory to create the instance
					S instance=factory.createInstance();
					provider=instance;

					// Release the factory
					factory=null;
				}
			}
		}

		private final Class<S> serviceClass;
		private volatile ServiceProviderFactory<S> factory;
		private S provider;
		private Scope scope;
	}
}
