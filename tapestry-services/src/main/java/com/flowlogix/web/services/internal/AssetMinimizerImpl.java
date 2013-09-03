package com.flowlogix.web.services.internal;

import com.flowlogix.util.Streams;
import com.flowlogix.web.services.AssetMinimizer;
import com.flowlogix.web.services.ServicesModule.Symbols;
import java.io.IOException;
import java.io.InputStream;
import lombok.Cleanup;
import lombok.SneakyThrows;
import org.apache.tapestry5.Asset;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.assets.ResourceDependencies;
import org.apache.tapestry5.services.assets.ResourceMinimizer;
import org.apache.tapestry5.services.assets.StreamableResource;
import org.apache.tapestry5.services.assets.StreamableResourceProcessing;
import org.apache.tapestry5.services.assets.StreamableResourceSource;

/**
 * TODO still necessary?
 * @author lprimak
 */
public class AssetMinimizerImpl implements AssetMinimizer
{
    @Override
    @SneakyThrows(IOException.class)
    public String minimize(Asset asset)
    {
        @Cleanup InputStream strm = openStream(asset);
        return Streams.readString(strm);
    }   
    
    
    private InputStream openStream(Asset asset) throws IOException
    {
        if(doMinimize)
        {
            StreamableResource rs = srs.getStreamableResource(asset.getResource(),
                StreamableResourceProcessing.COMPRESSION_DISABLED, rdp);
            return minimizer.minimize(rs).openStream();
        }
        else
        {
            return asset.getResource().openStream();
        }
    }
    
    
    private @Inject StreamableResourceSource srs;
    private @Inject ResourceMinimizer minimizer;
    private @Inject ResourceDependencies rdp;
    private @Inject @Symbol(Symbols.MINIMIZE_ASSETS) boolean doMinimize;
}
