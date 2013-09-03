package com.flowlogix.web.services;

import org.apache.tapestry5.Asset;

/**
 * remove whitespaces, etc. from scripts/CSS<br>
 * 
 * <a href="http://code.google.com/p/flowlogix/wiki/TLAssetMinimizer"
 *    target="_blank">See Documentation</a>
 * 
 * @author lprimak
 */
public interface AssetMinimizer
{
    String minimize(Asset asset);
}
