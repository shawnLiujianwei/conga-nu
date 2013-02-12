package com.conga.nu.test;

import com.conga.nu.AllowField;
import com.conga.nu.Scope;
import com.conga.nu.ServiceProvider;

/**
 *
 * @author Todd Fast
 */
@ServiceProvider(scope=Scope.APPLICATION)
public class ApplicationServiceWithMutableFieldsImpl extends SuperclassWithMutableFields
	implements ApplicationServiceWithMutableFields {

	public ApplicationServiceWithMutableFieldsImpl() {
		super();
	}

	@AllowField
	private static Object field1;
}
