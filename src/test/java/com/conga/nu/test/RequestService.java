package com.conga.nu.test;

/**
 *
 * @author Todd Fast
 */
public interface RequestService {

	public static final String CONSTANT="request";

	public int getID();

	public String echoValue(int value);

	public boolean isCompleted();
}
