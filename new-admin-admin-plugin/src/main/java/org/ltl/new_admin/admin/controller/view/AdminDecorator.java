package org.ltl.new_admin.admin.controller.view;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import com.tvd12.ezyhttp.server.core.view.View;
import com.tvd12.ezyhttp.server.core.view.ViewDecorator;

@EzySingleton
public class AdminDecorator implements ViewDecorator {

  @Override
  public void decorate(HttpServletRequest request, View view) {
    List<String> cssFiles = new ArrayList<>();
    cssFiles.add("/css/abaculus/index.css");
    cssFiles.add("/css/theme.css");
    view.appendValuesToVariable("additionalStyleFiles", cssFiles);
    String template = view.getTemplate();
    if(template.startsWith("admins/roles")) {
      String newAdminsRolesTemplate = "newadmin/" + template;
      view.setTemplate(newAdminsRolesTemplate);
    }
  }
}
