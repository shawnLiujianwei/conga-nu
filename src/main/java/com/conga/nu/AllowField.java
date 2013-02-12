package com.conga.nu;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows the developer to specify that an application-scoped service provider
 * is allowed to have instance fields, with the assumption that the developer
 * has verified that the fields do not pose a synchronization hazard.
 *
 * @author Todd Fast
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AllowField {
}
