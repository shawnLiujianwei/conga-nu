package com.conga.nu.test;

import com.conga.nu.Scope;
import com.conga.nu.ServiceProvider;

/**
 * Should not implement the interface, so should trigger an error when requested
 *
 * @author Todd Fast
 */
@ServiceProvider(scope=Scope.REQUEST, priority=100)
public class BogusServiceImpl /*intentional! implements RequestService*/ {

	public BogusServiceImpl() {
		super();
	}
}
