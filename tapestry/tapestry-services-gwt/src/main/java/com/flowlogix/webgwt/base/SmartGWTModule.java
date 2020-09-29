/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.webgwt.base;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
        return Stream.of(String.format("var isomorphicDir = \"%s\";", modulePathValue)).collect(Collectors.toList());
    }
}
