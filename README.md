Nu - A fast, featherweight service locator framework for Java
==============================================================

Nu makes your code modular by separating interface from implementation, allowing clean separation of API/SPI and pluggable implementation using the Service Locator pattern. Wiring between interface and implementation is nominally declarative via Java's ServiceLoader mechanism plus supporting annotations, but can also be controlled manually.

Nu doesn't currently provide dependency injection (DI), and as a result, it's considerably faster than frameworks that add DI on top of service location. For example, Nu is 4 - 7.5x faster than Google Guice's service locator. Use Nu when you need a service locator rather than full dependency injection.

Nu has been run in both client and high-concurrency server environments for several years without issues. Last but not least, Nu has no dependencies other than JDK 1.6.

		
Example use:
------------
```java
/**
 * Declare a service interface
 *
 */
public interface MyService {
	public String helloWorld();
}


/**
 * Provide an implementation of the service. Wire it up using
 * Java's standard META-INF/services mechanism.
 *
 */
@ServiceProvider(Scope.APPLICATION)
public class MyServiceImpl implements MyService {
	public String helloWorld() {
		return "Hello, world!";
	}
}


/**
 * Obtain and use the service instance using Nu's sugary $() method
 * (and there is a sugar-free version as well)
 *
 */
import static com.conga.nu.Services.$;

public class Main {
	public static void main(String[] args) {
		String message = $(MyService.class).helloWorld();

		System.out.println(message);
	}
}
```