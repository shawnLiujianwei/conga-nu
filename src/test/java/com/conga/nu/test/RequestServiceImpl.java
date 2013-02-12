package com.conga.nu.test;

import com.conga.nu.RequestCompletionListener;
import com.conga.nu.Scope;
import com.conga.nu.ServiceProvider;



/**
 *
 * @author Todd Fast
 */
@ServiceProvider(scope=Scope.REQUEST, priority=100)
public class RequestServiceImpl 
	implements RequestService, RequestCompletionListener {

	public RequestServiceImpl() {
		super();
		this.id=++INSTANCE_COUNT;
	}

	public int getID() {
		return id;
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

	private int id;
	private boolean completed;
	private static int INSTANCE_COUNT;
}
