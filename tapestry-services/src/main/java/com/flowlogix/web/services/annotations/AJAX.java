package com.flowlogix.web.services.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a tapestry Ajax event method,
 * graceful degradation to a non-ajax call<br>
 * 
 * <a href="http://code.google.com/p/flowlogix/wiki/TLAJAXAnnotation"
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
@Documented
@Target(ElementType.METHOD) 
@Retention(RetentionPolicy.RUNTIME) 
public @interface AJAX 
{ 
    /**
     * Whether to discard @Persist variables after the Ajax call
     */
    boolean discardAfter() default false;
    
    /**
     * Redirects to the this same page if a web session is not established
     * in an attempt to establish a session or log in, if required
     */
    boolean requireSession() default true;
} 
