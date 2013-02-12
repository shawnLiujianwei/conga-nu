package com.conga.nu.test;

import com.conga.nu.RequestCompletionListener;
import com.conga.nu.Scope;
import com.conga.nu.ServiceProvider;



/**
 *
 * @author Todd Fast
 */
@ServiceProvider(scope=Scope.REQUEST, priority=1)
public class RequestServiceImpl2 
	implements RequestService, RequestCompletionListener {

	public RequestServiceImpl2() {
		super();

		throw new WrongPriorityException(
			"Should not have found this provider implementation class because "+
			"priority should be less than "+RequestServiceImpl.class.getName());
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
