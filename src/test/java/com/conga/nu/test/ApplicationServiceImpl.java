package com.conga.nu.test;

import com.conga.nu.AllowField;
import com.conga.nu.Scope;
import com.conga.nu.ServiceProvider;



/**
 *
 * @author Todd Fast
 */
@ServiceProvider(scope=Scope.APPLICATION)
public class ApplicationServiceImpl implements ApplicationService {

	public ApplicationServiceImpl() {
		super();
		synchronized (ApplicationServiceImpl.class) {
			id=++INSTANCE_COUNT;
			if (id > 1)
				throw new IllegalStateException("Cannot create more than "+
					"one instance of the singleton (count: "+
					INSTANCE_COUNT+")");
		}
	}

	public String echoValue(int value) {
		return CONSTANT+value;
	}

	public int getID() {
		return id;
	}

	public boolean isCompleted() {
		return false;
	}

	public void requestComplete() {
		throw new IllegalStateException("Cannot request-complete singleton");
	}

	public void resetInstanceCount() {
		INSTANCE_COUNT=0;
	}

	@AllowField
	private int id;

	@AllowField
	private static int INSTANCE_COUNT;
}
