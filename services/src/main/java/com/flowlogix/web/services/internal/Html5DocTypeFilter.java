/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.web.services.internal;

import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.services.MarkupRenderer;
import org.apache.tapestry5.services.MarkupRendererFilter;

/**
 *
 * @author lprimak
 */
public class Html5DocTypeFilter implements MarkupRendererFilter
{
    @Override
    public void renderMarkup(MarkupWriter writer, MarkupRenderer renderer)
    {
        writer.getDocument().dtd("html", null, null);
        renderer.renderMarkup(writer);
    }   
}
