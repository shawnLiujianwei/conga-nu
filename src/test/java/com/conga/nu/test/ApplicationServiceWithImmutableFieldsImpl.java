package com.conga.nu.test;

import com.conga.nu.AllowField;
import com.conga.nu.Scope;
import com.conga.nu.ServiceProvider;

/**
 *
 * @author Todd Fast
 */
@ServiceProvider(scope=Scope.APPLICATION)
public class ApplicationServiceWithImmutableFieldsImpl extends SuperclassWithImmutableFields
	implements ApplicationServiceWithImmutableFields {

	public ApplicationServiceWithImmutableFieldsImpl() {
		super();
	}

	@AllowField
	private static Object field1;
}
