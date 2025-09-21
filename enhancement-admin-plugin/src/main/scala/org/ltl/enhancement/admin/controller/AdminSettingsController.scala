package org.ltl.enhancement.admin.controller

import com.tvd12.ezyfox.annotation.EzyFeature
import com.tvd12.ezyhttp.server.core.annotation.{Api, Authenticated, Controller}

@Api
@Authenticated
@Controller("/api/v1")
@EzyFeature("settings_management")
class AdminSettingsController {}