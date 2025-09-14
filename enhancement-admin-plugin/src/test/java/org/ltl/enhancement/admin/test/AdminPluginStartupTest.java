package org.ltl.enhancement.admin.test;

import com.tvd12.ezyhttp.server.boot.EzyHttpApplicationBootstrap;
import com.tvd12.ezyhttp.server.core.annotation.ComponentsScan;
import com.tvd12.ezyhttp.server.core.annotation.PropertiesSources;

@PropertiesSources({
    "config.properties",
    "setup.properties"
})
@ComponentsScan({
    "org.youngmonkeys.ezyplatform",
    "org.ltl.enhancement"
})
public class AdminPluginStartupTest {

    public static void main(String[] args) throws Exception {
        EzyHttpApplicationBootstrap.start(AdminPluginStartupTest.class);
    }
}
