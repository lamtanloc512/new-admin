package org.ltl.enhancement.admin.controller.api

import com.tvd12.ezyfox.annotation.EzyFeature
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind
import com.tvd12.ezyhttp.core.constant.Headers
import com.tvd12.ezyhttp.core.response.ResponseEntity
import com.tvd12.ezyhttp.server.core.annotation.*
import com.tvd12.ezyhttp.server.core.request.RequestArguments
import org.eclipse.jetty.http.HttpStatus
import org.ltl.enhancement.admin.service.{
  ImageEnhancementService,
  ImageResponse
}
import org.youngmonkeys.ezyplatform.admin.controller.service.AdminMediaControllerService
import org.youngmonkeys.ezyplatform.admin.service.AdminAdminService
import org.youngmonkeys.ezyplatform.annotation.{AdminId, AdminRoles}
import org.youngmonkeys.ezyplatform.data.AdminRolesProxy
import org.youngmonkeys.ezyplatform.entity.MediaType
import org.youngmonkeys.ezyplatform.model.PaginationModel
import org.youngmonkeys.ezyplatform.pagination.DefaultMediaFilter
import org.youngmonkeys.ezyplatform.response.MediaResponse
import org.youngmonkeys.ezyplatform.util.StringConverters

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Api
@Authenticated
@Controller("/api/v1")
@EzyFeature("enhancement_admin")
class ImageEnhancementController @EzyAutoBind() (
    adminService: AdminAdminService,
    mediaControllerService: AdminMediaControllerService,
    imageEnhancementService: ImageEnhancementService
) {

  @DoGet("/media/list")
  def listMedia(
      @AdminId adminId: Long,
      @AdminRoles adminRoles: AdminRolesProxy,
      @RequestParam("type") t: MediaType,
      @RequestParam("keyword") keyword: String,
      @RequestParam("nextPageToken") nextPageToken: String,
      @RequestParam("prevPageToken") prevPageToken: String,
      @RequestParam("lastPage") lastPage: Boolean,
      @RequestParam(value = "limit", defaultValue = "30") limit: Int
  ): PaginationModel[MediaResponse] = {
    val filterBuilder = DefaultMediaFilter
      .builder()
      .`type`(t)
      .prefixKeyword(StringConverters.trimOrNull(keyword))
    if (!isAllowAccessAllMedia(adminId, adminRoles)) {
      filterBuilder
        .ownerAdminId(adminId)
    }
    mediaControllerService.getMediaList(
      filterBuilder.build(),
      nextPageToken,
      prevPageToken,
      lastPage,
      limit
    )
  }

  @DoGet("/media/list/webp")
  def listMediaWebp(
      @AdminId adminId: Long,
      @AdminRoles adminRoles: AdminRolesProxy,
      @RequestParam("type") t: MediaType,
      @RequestParam("keyword") keyword: String,
      @RequestParam("nextPageToken") nextPageToken: String,
      @RequestParam("prevPageToken") prevPageToken: String,
      @RequestParam("lastPage") lastPage: Boolean,
      @RequestParam(value = "limit", defaultValue = "30") limit: Int
  ): PaginationModel[ImageResponse] = {
    val filterBuilder = DefaultMediaFilter
      .builder()
      .`type`(t)
      .prefixKeyword(StringConverters.trimOrNull(keyword))
    if (!isAllowAccessAllMedia(adminId, adminRoles)) {
      filterBuilder
        .ownerAdminId(adminId)
    }
    imageEnhancementService.getMediaListWebp(
      filterBuilder.build(),
      nextPageToken,
      prevPageToken,
      lastPage,
      limit
    )
  }

  @Async
  @DoGet("/media/{name}")
  def getMediaFile(
      args: RequestArguments,
      @PathVariable name: String
  ): Future[Either[Throwable, Unit]] = {
    imageEnhancementService.getCompressedImage(
      args,
      name,
      exposePrivateMedia = true,
      _ => true
    )
  }

  @Async
  @DoGet("/media/preload/{name}")
  def getPreloadImage(
      args: RequestArguments,
      @PathVariable name: String
  ): Future[Either[Throwable, Unit]] = {
    imageEnhancementService
      .getPlaceholderImage(
        args,
        name,
        exposePrivateMedia = true,
        _ => true
      )
  }

  private def isAllowAccessAllMedia(
      adminId: Long,
      adminRoles: AdminRolesProxy
  ) = {
    if (adminRoles.isSuperAdmin) true
    else adminService.isAllowAccessAllMedia(adminId)
  }

}
