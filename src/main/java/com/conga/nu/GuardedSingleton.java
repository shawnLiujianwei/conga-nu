package com.conga.nu;

import com.conga.nu.AllowField;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests whether the leaf subclass has been instantiated more than once, 
 * throwing an exception if so. This class is useful to help ensure there is
 * no bug in the singleton caching logic in this package. Service providers
 * in scope Scope.APPLICATION should extend from this class and be marked
 * final to help ensure correctness.
 *
 * @author Todd Fast
 */
public class GuardedSingleton {

	/**
	 *
	 *
	 */
	public GuardedSingleton() {
		super();

		synchronized (INSTANCE_COUNTS) {
			Integer count=INSTANCE_COUNTS.get(getClass());

			if (count==null) {
				INSTANCE_COUNTS.put(getClass(),1);
//				new Exception("Normal singleton instantiation of "+
//					getClass()).printStackTrace();
			}
			else
			if (count >= 1) {
				count+=1;
				INSTANCE_COUNTS.put(getClass(),count);
				throw new IllegalStateException(
					"Singleton was instantiated more than once. "+
					"Instance count: "+count);
			}
		}
	}

	@AllowField
	private static Map<Class,Integer> INSTANCE_COUNTS=
		Collections.synchronizedMap(new HashMap<Class,Integer>());
}
