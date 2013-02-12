package com.conga.nu.test;

import com.conga.nu.AllowField;

/**
 *
 * @author Todd Fast
 */
public class SuperclassWithImmutableFields {

	private final String field1=
		"This field should NOT prevent instantiation of the service";
	
	@AllowField
	private Object field2;

	@AllowField
	private static Object field3;

	@AllowField
	private static final Object field4=null;
}
