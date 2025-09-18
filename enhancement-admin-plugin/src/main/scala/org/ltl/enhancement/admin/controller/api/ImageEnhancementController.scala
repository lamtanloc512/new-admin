package org.ltl.enhancement.admin.controller.api

import com.tvd12.ezyfox.annotation.EzyFeature
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind
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

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

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

  private def isAllowAccessAllMedia(
      adminId: Long,
      adminRoles: AdminRolesProxy
  ) = {
    if (adminRoles.isSuperAdmin) true
    else adminService.isAllowAccessAllMedia(adminId)
  }

  @Async
  @DoGet("/media/{name}")
  def getMediaFile(
      args: RequestArguments,
      @PathVariable name: String
  ): Future[Either[Throwable, Unit]] = {
    imageEnhancementService.getWebpImage(
      args,
      name,
      exposePrivateMedia = true,
      _ => true
    )
  }

  @DoGet("/media/image/convert")
  def convert(
      @RequestParam format: String
  ): Future[ResponseEntity] = {
    imageEnhancementService.convertImage(format).map {
      case Right(_) =>
        ResponseEntity.status(HttpStatus.NO_CONTENT_204).build()
      case Left(err) =>
        ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR_500)
          .body(err.getMessage)
          .build()
    }
  }
}
