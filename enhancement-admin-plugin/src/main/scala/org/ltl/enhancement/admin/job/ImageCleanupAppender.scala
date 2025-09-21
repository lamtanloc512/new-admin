package org.ltl.enhancement.admin.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.tvd12.ezyfox.bean.annotation.{EzyAutoBind, EzySingleton}
import org.youngmonkeys.ezyplatform.admin.appender.AdminDataAppender
import org.youngmonkeys.ezyplatform.admin.service.AdminSettingService
import org.youngmonkeys.ezyplatform.controller.service.MediaControllerService
import org.youngmonkeys.ezyplatform.entity.MediaType
import org.youngmonkeys.ezyplatform.manager.FileSystemManager
import org.youngmonkeys.ezyplatform.pagination.DefaultMediaFilter
import org.youngmonkeys.ezyplatform.service.MediaService

import java.io.File
import java.util
import scala.jdk.CollectionConverters.*

case class ImageCleanupRecord(
    mediaId: Long,
    originalName: String,
    webpDeleted: Boolean,
    bmpDeleted: Boolean,
    status: String,
    timestamp: Long
)

@EzySingleton
class ImageCleanupAppender @EzyAutoBind() (
    objectMapper: ObjectMapper,
    adminSettingService: AdminSettingService,
    mediaService: MediaService,
    mediaControllerService: MediaControllerService,
    fileSystemManager: FileSystemManager
) extends AdminDataAppender[String, ImageCleanupRecord, String](
      objectMapper,
      adminSettingService
    ) {

  override protected def getAppenderNamePrefix: String = "image-cleanup"

  override protected def defaultPageToken: String = ""

  override protected def pageTokenType: Class[String] = classOf[String]

  override protected def getValueList(pageToken: String): util.List[String] = {
    val filter = DefaultMediaFilter.builder().`type`(MediaType.IMAGE).build()
    val pagination =
      mediaControllerService.getMediaList(filter, pageToken, "", false, 10)
    pagination.getItems.asScala.map(_.getName).asJava
  }

  override protected def extractNewLastPageToken(
      values: util.List[String],
      lastPageToken: String
  ): String = {
    val filter = DefaultMediaFilter.builder().`type`(MediaType.IMAGE).build()
    val pagination =
      mediaControllerService.getMediaList(filter, lastPageToken, "", false, 10)
    pagination.getPageToken.getNext
  }

  override protected def toDataRecord(value: String): ImageCleanupRecord = {
    val media = Option(mediaService.getMediaByName(value))
    media match {
      case Some(m) =>
        val uploadFolder = fileSystemManager.getUploadFolder

        val webpFile =
          File(uploadFolder, s"images/webp/${replaceWithWebp(value)}")
        val bmpFile = File(uploadFolder, s"images/bmp/${replaceWithBmp(value)}")

        val originalFile =
          fileSystemManager.getMediaFilePath(m.getType.getFolder, value)

        var webpDeleted = false
        var bmpDeleted = false

        if (!originalFile.exists()) {
          if (webpFile.exists()) {
            webpFile.delete()
            webpDeleted = true
          }
          if (bmpFile.exists()) {
            bmpFile.delete()
            bmpDeleted = true
          }
        }

        ImageCleanupRecord(
          m.getId,
          value,
          webpDeleted,
          bmpDeleted,
          if (webpDeleted || bmpDeleted) "cleaned" else "no_action",
          System.currentTimeMillis()
        )
      case None =>
        ImageCleanupRecord(
          -1L,
          value,
          false,
          false,
          "media_not_found",
          System.currentTimeMillis()
        )
    }
  }

  override protected def serializePageToken(pageToken: String): String =
    pageToken

  override protected def loadLastPageToken: String = {
    defaultPageToken
  }

  private def replaceWithWebp(fileName: String): String = {
    fileName.lastIndexOf('.') match {
      case -1 => s"$fileName.webp"
      case i  => fileName.substring(0, i) + ".webp"
    }
  }

  private def replaceWithBmp(fileName: String): String = {
    fileName.lastIndexOf('.') match {
      case -1 => s"$fileName.bmp"
      case i  => fileName.substring(0, i) + ".bmp"
    }
  }
}
