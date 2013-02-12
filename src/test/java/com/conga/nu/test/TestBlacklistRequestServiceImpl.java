package com.conga.nu.test;

import com.conga.nu.RequestCompletionListener;
import com.conga.nu.Scope;
import com.conga.nu.ServiceProvider;



/**
 * Priority of this class is lowest, but it should be found if
 * RequestServiceImpl and RequestServiceImpl2 are blacklisted
 *
 * @author Todd Fast
 */
@ServiceProvider(scope=Scope.REQUEST, priority=Integer.MIN_VALUE)
public class TestBlacklistRequestServiceImpl
	implements RequestService, RequestCompletionListener {

	public TestBlacklistRequestServiceImpl() {
		super();
	}

	public int getID() {
		return 0;
	}

	public String echoValue(int value) {
		return BLACKLISTED;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void requestComplete() {
		// System.out.println("Request complete for instance "+getID());
		completed=true;
	}

	private boolean completed;
	public static final String BLACKLISTED="BLACKLISTED!";
}
