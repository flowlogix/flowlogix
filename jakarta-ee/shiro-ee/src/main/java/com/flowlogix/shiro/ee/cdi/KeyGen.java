package com.flowlogix.shiro.ee.cdi;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.crypto.AesCipherService;
import org.apache.shiro.mgt.AbstractRememberMeManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.omnifaces.util.Beans;

/**
 * Shiro cipher key generator
 *
 * @author lprimak
 */
public class KeyGen {
    public interface CipherKeySupplier extends Supplier<String> {
    }

    public void setSecurityManager(DefaultWebSecurityManager securityManager) {
        var rememberMeManager = securityManager.getRememberMeManager();
        if (rememberMeManager instanceof AbstractRememberMeManager) {
            ((AbstractRememberMeManager) rememberMeManager).setCipherKey(generateCipherKey());
        }
    }

    private byte[] generateCipherKey() {
        String key = Beans.getReference(CipherKeySupplier.class).get();
        if (StringUtils.isBlank(key)) {
            return new AesCipherService().generateNewKey().getEncoded();
        } else {
            return key.getBytes(StandardCharsets.UTF_8);
        }
    }
}
