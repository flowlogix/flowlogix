package com.flowlogix.web.mixins;

import javax.validation.constraints.NotNull;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.annotations.Parameter;

/**
 * @deprecated ** Left for backward compatibility
 * @see ColorHighlightOverride
 * <a href="http://code.google.com/p/flowlogix/wiki/TLColorHighlight"
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
@Deprecated
public class ColorHighlight
{
    private @Parameter(required=true, defaultPrefix=BindingConstants.LITERAL) 
            @NotNull String highlightColor;
}
