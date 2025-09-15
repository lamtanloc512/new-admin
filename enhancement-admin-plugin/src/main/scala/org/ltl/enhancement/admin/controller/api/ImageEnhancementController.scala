package org.ltl.enhancement.admin.controller.api

import com.tvd12.ezyfox.annotation.EzyFeature
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind
import com.tvd12.ezyhttp.server.core.annotation.*
import org.ltl.enhancement.admin.controller.api.pagination.DefaultEnhancementMediaFilter
import org.youngmonkeys.ezyplatform.admin.controller.service.AdminMediaControllerService
import org.youngmonkeys.ezyplatform.admin.service.AdminAdminService
import org.youngmonkeys.ezyplatform.annotation.{AdminId, AdminRoles}
import org.youngmonkeys.ezyplatform.data.AdminRolesProxy
import org.youngmonkeys.ezyplatform.entity.MediaType
import org.youngmonkeys.ezyplatform.manager.FileSystemManager
import org.youngmonkeys.ezyplatform.model.PaginationModel
import org.youngmonkeys.ezyplatform.response.MediaResponse
import org.youngmonkeys.ezyplatform.util.StringConverters
import org.youngmonkeys.ezyplatform.pagination.DefaultMediaFilter

@Api
@Authenticated
@Controller("/api/v1")
@EzyFeature("enhancement_admin")
class ImageEnhancementController @EzyAutoBind() (
    fileSystemManager: FileSystemManager,
    adminService: AdminAdminService,
    mediaControllerService: AdminMediaControllerService
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
    val allowAccessAllMedia = isAllowAccessAllMedia(adminId, adminRoles)

    val filter = DefaultMediaFilter
      .builder()
      .`type`(t)
      .prefixKeyword(StringConverters.trimOrNull(keyword))
      .build()

    mediaControllerService.getMediaList(
      filter,
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

  // @DoGet("/media/{id}/details")
  // def getMediaDetails(@PathVariable("id") id: Long): ResponseEntity = {
  //   try {
  //     // For now, return placeholder data
  //     // In a real implementation, you would query the database by ID
  //     val mediaDetails = AdminMediaDetailsResponse(
  //       id = id,
  //       name = s"media_$id",
  //       originalName = s"original_media_$id.jpg",
  //       mimeType = "image/jpeg",
  //       size = 1024000L,
  //       width = 1920,
  //       height = 1080,
  //       url = s"/api/v1/media/media_$id.jpg",
  //       publicMedia = true,
  //       `type` = "IMAGE",
  //       alternativeText = "",
  //       title = "",
  //       caption = "",
  //       description = "",
  //       createdAt = System.currentTimeMillis(),
  //       ownerAdmin = Some(AdminInfo("admin", "Administrator"))
  //     )

  //     ResponseEntity.ok(AdminMediaDetailsResponse.toJava(mediaDetails))

  //   } catch {
  //     case e: Exception =>
  //       ResponseEntity.badRequest(
  //         Map("error" -> s"Failed to get media details: ${e.getMessage}").asJava
  //       )
  //   }
  // }

  // @DoGet("/media/{name}")
  // def getMediaFile(@PathVariable("name") name: String): ResponseEntity = {
  //   try {
  //     // Try processed file first, then original
  //     val processedFile = new File(s"$processedStorageDir/$name")
  //     val originalFile = new File(s"$mediaStorageDir/$name")

  //     val fileToServe =
  //       if (processedFile.exists()) processedFile else originalFile

  //     if (!fileToServe.exists()) {
  //       return ResponseEntity.notFound()
  //     }

  //     // Read file bytes
  //     val fileBytes = Files.readAllBytes(fileToServe.toPath)

  //     // Determine content type
  //     val contentType = Files.probeContentType(fileToServe.toPath)
  //     val finalContentType =
  //       if (contentType != null) contentType else "application/octet-stream"

  //     ResponseEntity.ok(fileBytes)

  //   } catch {
  //     case e: Exception =>
  //       e.printStackTrace()
  //       ResponseEntity.notFound()
  //   }
  // }

  // @DoPut("/media/{id}")
  // def updateMedia(
  //     @PathVariable("id") id: Long,
  //     @RequestBody request: java.util.Map[String, Any]
  // ): ResponseEntity = {
  //   result match {
  //     case Right(response) => response
  //     case Left(error) =>
  //       ResponseEntity.badRequest(Map("error" -> error).asJava)
  //   }
  // }

}
