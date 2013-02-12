package com.conga.nu.test;

import com.conga.nu.Scope;
import com.conga.nu.ServiceProvider;



/**
 *
 * @author Todd Fast
 */
public interface ApplicationService {

	public static final String CONSTANT="singleton";

	public int getID();

	public String echoValue(int value);

	public boolean isCompleted();

	public void resetInstanceCount();
}
