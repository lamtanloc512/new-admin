package org.ltl.new_admin.admin;

import com.tvd12.ezyhttp.server.boot.EzyHttpApplicationBootstrap;
import com.tvd12.ezyhttp.server.core.annotation.ComponentsScan;
import com.tvd12.ezyhttp.server.core.annotation.PropertiesSources;

@PropertiesSources({"config.properties", "setup.properties"})
@ComponentsScan({"org.youngmonkeys.ezyplatform", "org.ltl.new_admin"})
public class App {
  public static void main(String[] args) throws Exception {
    EzyHttpApplicationBootstrap.start(App.class);
  }
}
