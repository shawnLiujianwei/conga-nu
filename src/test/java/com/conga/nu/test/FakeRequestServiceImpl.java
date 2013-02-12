package com.conga.nu.test;

import com.conga.nu.RequestCompletionListener;
import com.conga.nu.Scope;
import com.conga.nu.ServiceProvider;



/**
 *
 * @author Todd Fast
 */
// Intentionally leave off the annotation so this class is not found
public class FakeRequestServiceImpl 
	implements RequestService, RequestCompletionListener {

	public FakeRequestServiceImpl() {
		super();

		throw new IllegalStateException(
			"Should not have found this provider implementation class");
	}

	public int getID() {
		return 0;
	}

	public String echoValue(int value) {
		return CONSTANT+value;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void requestComplete() {
		// System.out.println("Request complete for instance "+getID());
		completed=true;
	}

	private boolean completed;
}
