package org.ltl.new_admin.admin.controller.view

import com.tvd12.ezyfox.bean.annotation.EzySingleton
import com.tvd12.ezyfox.util.EzyLoggable
import com.tvd12.ezyhttp.server.core.view.View
import com.tvd12.ezyhttp.server.core.view.ViewDecorator

import java.util.ArrayList
import java.util.HashSet
import java.util.List
import java.util.Set
import javax.servlet.http.HttpServletRequest
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

@EzySingleton
class AdminDecorator extends ViewDecorator {

  override def decorate(request: HttpServletRequest, view: View): Unit = {
    view.appendValuesToVariable("additionalStyleFiles", cssFiles.asJava)
    view.appendValuesToVariable("finalScriptFiles", scriptFiles.asJava)
    val template = view.getTemplate()
    if (ifExist(template)) {
      view.setTemplate(s"newadmin/$template")
    }
  }

  private def ifExist(template: String): Boolean = {
    filterModules.exists(template.contains(_))
  }

  private val filterModules: Seq[String] =
    Seq(
      "admins/roles",
      "admins/list",
      "admins/profile",
      "users/roles",
      "users/list",
      "users/profile",
      "ezyplatform/history",
      "modules"
    )
  private val cssFiles = Seq("/css/abaculus/index.min.css", "/css/theme.css")
  private val scriptFiles = Seq("/js/theme.js")
}
