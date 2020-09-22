/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.webgwt.base;

import com.google.common.collect.Lists;
import java.util.List;


/**
 *
 * @author lprimak
 */
public abstract class SmartGWTModule extends GwtSupport
{
    @Override
    protected List<String> getJavaScriptInitialization()
    {
        final String modulePathValue = String.format("%s/sc/", getGwtModulePath());
        return Lists.newArrayList(String.format("var isomorphicDir = \"%s\";", modulePathValue));
    }
}
