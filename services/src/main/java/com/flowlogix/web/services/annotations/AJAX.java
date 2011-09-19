/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a tapestry Ajax event method,
 * graceful degradation to a non-ajax call
 * @author lprimak
 */
@Documented
@Target(ElementType.METHOD) 
@Retention(RetentionPolicy.RUNTIME) 
public @interface AJAX 
{ 
    boolean discardAfter() default false;
    boolean requireSession() default true;
} 
