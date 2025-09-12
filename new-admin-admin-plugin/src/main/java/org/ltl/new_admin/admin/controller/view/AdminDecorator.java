package org.ltl.new_admin.admin.controller.view;

import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import com.tvd12.ezyhttp.server.core.view.View;
import com.tvd12.ezyhttp.server.core.view.ViewDecorator;
import javax.servlet.http.HttpServletRequest;

@EzySingleton
public class AdminDecorator implements ViewDecorator {

  @Override
  public void decorate(HttpServletRequest request, View view) {
    String template = view.getTemplate();
    if (!template.equals("default-menus")) {
      view.setTemplate("newadmin/" + template);
    }
  }
}
