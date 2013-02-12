package com.conga.nu.test;

/**
 *
 * @author Todd Fast
 */
public class SuperclassWithMutableFields {

	private final String field1=
		"This field should NOT prevent instantiation of the service";
	private String field2=
		"This field should prevent instantiation of the service";
	private final Object field3=null;
}
