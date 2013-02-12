/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.conga.nu;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author Todd Fast
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ServiceProvider {

	/**
	 * The scope that the service provider requires
	 * 
	 * @return
	 */
	public Scope scope();

	/**
	 *
	 *
	 */
	public int priority() default 0;
}
