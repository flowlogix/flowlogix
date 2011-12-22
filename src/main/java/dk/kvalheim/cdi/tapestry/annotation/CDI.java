/**
 * @(#)CDI.java
 *
 * Copyright 2009 by Movellas ApS
 * All rights reserved. 
 */
package dk.kvalheim.cdi.tapestry.annotation;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
/**
 * 
 * @author Magnus
 */
@Target(
{ FIELD})
@Retention(RUNTIME)
public @interface CDI {

}
