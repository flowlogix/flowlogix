package com.flowlogix.web.services;

import com.flowlogix.session.internal.SessionTrackerUtil;
import com.flowlogix.web.mixins.AutoDisableAfterSubmit;
import com.flowlogix.web.mixins.FormHorizontal;
import com.flowlogix.web.services.internal.AjaxAnnotationWorker;
import com.flowlogix.web.services.internal.AssetMinimizerImpl;
import com.flowlogix.web.services.internal.MixinAdderWorker;
import java.io.IOException;
import org.apache.tapestry5.beanvalidator.BeanValidatorConfigurer;
import org.apache.tapestry5.beanvalidator.BeanValidatorSource;
import org.apache.tapestry5.corelib.components.BeanEditForm;
import org.apache.tapestry5.corelib.components.BeanEditor;
import org.apache.tapestry5.corelib.components.Submit;
import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Contribute;
import org.apache.tapestry5.ioc.annotations.ImportModule;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Match;
import org.apache.tapestry5.ioc.annotations.Primary;
import org.apache.tapestry5.ioc.services.FactoryDefaults;
import org.apache.tapestry5.ioc.services.SymbolProvider;
import org.apache.tapestry5.services.*;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;

/**
 * Put it all together, integrate with Tapestry
 * 
 * @author lprimak
 */
@ImportModule({ EjbModule.class, GwtModule.class, SecurityModule.class, CDIModule.class })
public class ServicesModule 
{    
    @Contribute(SymbolProvider.class)
    @FactoryDefaults
    public static void setFactoryDefaults(MappedConfiguration<String, String> configuration)
    {
        configuration.add(Symbols.MINIMIZE_ASSETS, "true");
        configuration.add(FormHorizontal.Symbols.LABEL_CLASS_DEFAULT, "col-md-2");
        configuration.add(FormHorizontal.Symbols.INPUT_CLASS_DEFAULT, "col-md-10");
        configuration.add(FormHorizontal.Symbols.FORM_HORIZONTAL_DISABLED, "false");
    }


    @Contribute(ComponentClassResolver.class)
    public static void addLibrary(Configuration<LibraryMapping> configuration)
    {
        configuration.add(new LibraryMapping("flowlogix", "com.flowlogix.web"));
    }
    
        
    @Contribute(ComponentClassTransformWorker2.class)
    @Primary
    public static void provideClassTransformWorkers(OrderedConfiguration<ComponentClassTransformWorker2> configuration)
    {
        configuration.addInstance("AJAX", AjaxAnnotationWorker.class, "before:Property");
        configuration.add("DisableAfterSubmit", 
                new MixinAdderWorker(Submit.class, AutoDisableAfterSubmit.class), "after:AJAX");
        configuration.add("FormHorizontalBEFSupport",
                new MixinAdderWorker(BeanEditForm.class, FormHorizontal.class), "after:AJAX");
        configuration.add("FormHorizontalBESupport",
                new MixinAdderWorker(BeanEditor.class, FormHorizontal.class), "after:AJAX");
    }

    
    public static void bind(ServiceBinder binder)
    {
        binder.bind(AssetMinimizer.class, AssetMinimizerImpl.class);
        binder.bind(ExternalPageLink.class);
    }
    
    
    /**
     * silently redirect the user to the intended page when browsing through
     * tapestry forms through browser history
     */
    @Match("RequestExceptionHandler")
    public RequestExceptionHandler decorateInvalidFormRedirection(
            final ComponentSource componentSource,
            final Response response,
            final RequestExceptionHandler oldHandler)
    {
        return new RequestExceptionHandler()
        {
            @Override
            public void handleRequestException(Throwable exception) throws IOException
            {
                String msg = exception.getMessage();
                if(msg == null)
                {
                    msg = "";
                }
                if (!msg.contains("Forms require that the request method be POST and that the t:formdata query parameter have values"))
                {
                    oldHandler.handleRequestException(exception);
                    return;
                }
                SessionTrackerUtil.redirectToSelf(rg, linkSource);
            }
        };
    }
    
    
    @Contribute(BeanValidatorSource.class)
    public static void provideBeanValidatorConfigurer(OrderedConfiguration<BeanValidatorConfigurer> configuration)
    {
        configuration.add("FlowLogixBeanConfigurer", new BeanValidatorConfigurer()
        {
            @Override
            public void configure(javax.validation.Configuration<?> configuration)
            {
                configuration.ignoreXmlConfiguration();
            }
        });
    }


    public static class Symbols
    {
        public static final String MINIMIZE_ASSETS = "flowlogix.minimize-assets";
    }
    
    
    private @Inject RequestGlobals rg;
    private @Inject PageRenderLinkSource linkSource;
}
