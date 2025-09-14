package org.ltl.enhancement.admin.controller.api

import com.tvd12.ezyfox.annotation.EzyFeature
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind
import com.tvd12.ezyhttp.core.codec.BodyDeserializer
import com.tvd12.ezyhttp.core.constant.HttpMethod
import com.tvd12.ezyhttp.core.constant.StatusCodes
import com.tvd12.ezyhttp.core.response.ResponseEntity
import com.tvd12.ezyhttp.server.core.annotation.*
import org.ltl.enhancement.admin.controller.api.AdminInfo
import org.ltl.enhancement.admin.controller.api.AdminMediaDetailsResponse
import org.ltl.enhancement.admin.controller.api.ContinuationInfo
import org.ltl.enhancement.admin.controller.api.ErrorResponse
import org.ltl.enhancement.admin.controller.api.MediaListResponse
import org.ltl.enhancement.admin.controller.api.MediaResponse
import org.ltl.enhancement.admin.controller.api.PageToken
import org.ltl.enhancement.admin.controller.api.PaginationModel
import org.ltl.enhancement.admin.controller.api.SuccessResponse

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

@Api
@Authenticated
@Controller("/api/v1")
@EzyFeature("enhancement_admin")
class ImageEnhancementController {

  // Directory for storing uploaded media files
  private val mediaStorageDir = "uploads/media"
  private val processedStorageDir = "uploads/processed"

  // Ensure directories exist
  ensureDirectoriesExist()

  private def ensureDirectoriesExist(): Unit = {
    val dirs = List(mediaStorageDir, processedStorageDir)
    dirs.foreach { dir =>
      val path = Paths.get(dir)
      if (!Files.exists(path)) {
        Files.createDirectories(path)
      }
    }
  }

  private def validateFile(file: Array[Byte]): Either[String, Unit] = {
    Either.cond(file != null && file.nonEmpty, (), "No file provided")
  }

  private def generateUniqueFilename(fileName: String): String = {
    val fileExtension = getFileExtension(fileName)
    s"${System.currentTimeMillis()}_${scala.util.Random.nextInt(1000)}$fileExtension"
  }

  private def saveOriginalFile(
      file: File,
      bytes: Array[Byte]
  ): Either[String, Unit] = {
    try {
      Files.write(file.toPath, bytes)
      Right(())
    } catch {
      case e: Exception => Left(s"Failed to save file: ${e.getMessage}")
    }
  }

  private def createMediaResponse(
      uniqueFilename: String,
      fileName: String,
      mimeType: String,
      processedFile: File,
      notPublic: Boolean
  ): MediaResponse = {
    MediaResponse(
      id = scala.util.Random.nextLong().abs,
      name = uniqueFilename,
      originalName = fileName,
      mimeType = mimeType,
      size = processedFile.length(),
      url = s"/api/v1/media/$uniqueFilename",
      publicMedia = !notPublic,
      `type` = getMediaType(mimeType),
      createdAt = System.currentTimeMillis()
    )
  }

  /** Add media from URL POST /api/v1/media/add-from-url
    */
  @DoPost("/media/add-from-url")
  def addMediaFromUrl(
      @RequestBody request: java.util.Map[String, Any],
      @RequestParam(
        value = "notPublic",
        defaultValue = "false"
      ) notPublic: Boolean
  ): ResponseEntity = {
    try {
      val url = request.get("url").asInstanceOf[String]
      val mediaType = request.get("type").asInstanceOf[String]
      val originalName = request.get("originalName").asInstanceOf[String]

      if (url == null || url.trim.isEmpty) {
        return ResponseEntity.badRequest("URL is required")
      }

      // For now, just create a placeholder response
      // In a real implementation, you would download the file from the URL
      val mediaResponse = Map(
        "id" -> scala.util.Random.nextLong().abs,
        "name" -> s"url_${System.currentTimeMillis()}",
        "originalName" -> originalName,
        "mimeType" -> getMimeTypeFromUrl(url),
        "size" -> 0L,
        "url" -> url,
        "publicMedia" -> !notPublic,
        "type" -> mediaType,
        "createdAt" -> System.currentTimeMillis()
      ).asJava

      ResponseEntity.ok(mediaResponse)

    } catch {
      case e: Exception =>
        ResponseEntity.badRequest(
          Map(
            "error" -> s"Failed to add media from URL: ${e.getMessage}"
          ).asJava
        )
    }
  }

  /** Get media list GET /api/v1/media/list
    */
  @DoGet("/media/list")
  def getMediaList(
      @RequestParam(value = "limit", defaultValue = "30") limit: Int,
      @RequestParam(value = "keyword") keyword: String,
      @RequestParam(value = "type") mediaType: String,
      @RequestParam(value = "nextPageToken") nextPageToken: String
  ): ResponseEntity = {
    try {
      // For now, return empty list
      // In a real implementation, you would query the database
      val mediaListResponse = MediaListResponse(
        items = List.empty[MediaResponse],
        count = 0,
        total = 0,
        pageToken = PageToken(None),
        continuation = ContinuationInfo(false)
      )

      ResponseEntity.ok(MediaListResponse.toJava(mediaListResponse))

    } catch {
      case e: Exception =>
        ResponseEntity.badRequest(
          Map("error" -> s"Failed to get media list: ${e.getMessage}").asJava
        )
    }
  }

  /** Get media details GET /api/v1/media/{id}/details
    */
  @DoGet("/media/{id}/details")
  def getMediaDetails(@PathVariable("id") id: Long): ResponseEntity = {
    try {
      // For now, return placeholder data
      // In a real implementation, you would query the database by ID
      val mediaDetails = AdminMediaDetailsResponse(
        id = id,
        name = s"media_$id",
        originalName = s"original_media_$id.jpg",
        mimeType = "image/jpeg",
        size = 1024000L,
        width = 1920,
        height = 1080,
        url = s"/api/v1/media/media_$id.jpg",
        publicMedia = true,
        `type` = "IMAGE",
        alternativeText = "",
        title = "",
        caption = "",
        description = "",
        createdAt = System.currentTimeMillis(),
        ownerAdmin = Some(AdminInfo("admin", "Administrator"))
      )

      ResponseEntity.ok(AdminMediaDetailsResponse.toJava(mediaDetails))

    } catch {
      case e: Exception =>
        ResponseEntity.badRequest(
          Map("error" -> s"Failed to get media details: ${e.getMessage}").asJava
        )
    }
  }

  /** Get media file (serve the actual file) GET /api/v1/media/{name}
    */
  @DoGet("/media/{name}")
  def getMediaFile(@PathVariable("name") name: String): ResponseEntity = {
    try {
      // Try processed file first, then original
      val processedFile = new File(s"$processedStorageDir/$name")
      val originalFile = new File(s"$mediaStorageDir/$name")

      val fileToServe =
        if (processedFile.exists()) processedFile else originalFile

      if (!fileToServe.exists()) {
        return ResponseEntity.notFound()
      }

      // Read file bytes
      val fileBytes = Files.readAllBytes(fileToServe.toPath)

      // Determine content type
      val contentType = Files.probeContentType(fileToServe.toPath)
      val finalContentType =
        if (contentType != null) contentType else "application/octet-stream"

      ResponseEntity.ok(fileBytes)

    } catch {
      case e: Exception =>
        e.printStackTrace()
        ResponseEntity.notFound()
    }
  }

  /** Update media PUT /api/v1/media/{id}
    */
  @DoPut("/media/{id}")
  def updateMedia(
      @PathVariable("id") id: Long,
      @RequestBody request: java.util.Map[String, Any]
  ): ResponseEntity = {
    try {
      // For now, just return success
      // In a real implementation, you would update the media in the database
      val successResponse = SuccessResponse("Media updated successfully")
      ResponseEntity.ok(SuccessResponse.toJava(successResponse))

    } catch {
      case e: Exception =>
        ResponseEntity.badRequest(
          Map("error" -> s"Failed to update media: ${e.getMessage}").asJava
        )
    }
  }

  /** Delete media DELETE /api/v1/media/{id}
    */
  @DoDelete("/media/{id}")
  def deleteMedia(@PathVariable("id") id: Long): ResponseEntity = {
    try {
      // For now, just return success
      // In a real implementation, you would delete the media from storage and database
      val successResponse = SuccessResponse("Media deleted successfully")
      ResponseEntity.ok(SuccessResponse.toJava(successResponse))

    } catch {
      case e: Exception =>
        ResponseEntity.badRequest(
          Map("error" -> s"Failed to delete media: ${e.getMessage}").asJava
        )
    }
  }

  // Helper methods
  private def getFileExtension(filename: String): String = {
    val lastDotIndex = filename.lastIndexOf('.')
    if (lastDotIndex > 0) filename.substring(lastDotIndex) else ""
  }

  private def getMediaType(mimeType: String): String = {
    if (mimeType.startsWith("image/")) "IMAGE"
    else if (mimeType.startsWith("video/")) "VIDEO"
    else if (mimeType.startsWith("audio/")) "AUDIO"
    else "FILE"
  }

  private def getMimeTypeFromUrl(url: String): String = {
    // Simple MIME type detection from URL extension
    if (url.contains(".jpg") || url.contains(".jpeg")) "image/jpeg"
    else if (url.contains(".png")) "image/png"
    else if (url.contains(".gif")) "image/gif"
    else if (url.contains(".webp")) "image/webp"
    else if (url.contains(".mp4")) "video/mp4"
    else if (url.contains(".mp3")) "audio/mpeg"
    else "application/octet-stream"
  }

}
