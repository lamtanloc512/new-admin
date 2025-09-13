package org.ltl.new_admin.admin.controller.view;

import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import com.tvd12.ezyhttp.server.core.view.View;
import com.tvd12.ezyhttp.server.core.view.ViewDecorator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletContext;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@EzySingleton
public class AdminDecorator implements ViewDecorator {

  @Override
  public void decorate(HttpServletRequest request, View view) {
    List<String> cssFiles = new ArrayList<>();
    cssFiles.add("/css/abaculus.css");
    cssFiles.add("/css/theme.css");
    view.appendValuesToVariable("additionalStyleFiles", cssFiles);
    // String template = view.getTemplate();
    // System.out.println("Template: " + template);

    // // Always force newadmin layout for all templates
    // String newAdminTemplate = "newadmin/" + template;
    // if (templateExists(request.getServletContext(), newAdminTemplate)) {
    //   // Use newadmin template if it exists
    //   view.setTemplate(newAdminTemplate);
    // } else {
    //   // Use newadmin wrapper for plugin templates
    //   view.setTemplate("newadmin/wrapper");
    //   // Pass original template path to the wrapper
    //   request.setAttribute("originalTemplate", template);
    // }
  }

  private boolean templateExists(ServletContext servletContext, String templatePath) {
    try {
      // Convert template path to resource path
      String resourcePath = "/templates/" + templatePath + ".html";
      InputStream resource = servletContext.getResourceAsStream(resourcePath);
      if (resource != null) {
        resource.close();
        return true;
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }
}
