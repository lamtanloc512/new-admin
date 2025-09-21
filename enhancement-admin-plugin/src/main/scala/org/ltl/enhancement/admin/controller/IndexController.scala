package org.ltl.enhancement.admin.controller

import com.tvd12.ezyfox.annotation.EzyFeature
import com.tvd12.ezyhttp.server.core.annotation.{Authenticated, Controller}
import org.youngmonkeys.ezyplatform.admin.controller.view.AdminModuleIndexController
import org.youngmonkeys.ezyplatform.admin.manager.AdminMenuManager

@Controller
@Authenticated
@EzyFeature("enhancement_admin") class IndexController(
    menuManager: AdminMenuManager
) extends AdminModuleIndexController(menuManager) {
  override protected def getModuleName = "enhancement_admin"
}
