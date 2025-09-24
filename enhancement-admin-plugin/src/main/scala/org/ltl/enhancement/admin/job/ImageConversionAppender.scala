package org.ltl.enhancement.admin.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.webp.WebpWriter
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
import com.sksamuel.scrimage.ScaleMethod

case class ImageConversionRecord(
    mediaId: Long,
    originalName: String,
    convertedName: String,
    status: String,
    timestamp: Long
)

@EzySingleton
class ImageConversionAppender @EzyAutoBind() (
    objectMapper: ObjectMapper,
    adminSettingService: AdminSettingService,
    mediaService: MediaService,
    mediaControllerService: MediaControllerService,
    fileSystemManager: FileSystemManager
) extends AdminDataAppender[String, ImageConversionRecord, String](
      objectMapper,
      adminSettingService
    ) {

  override protected def getAppenderNamePrefix: String = "image-conversion"

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

  override protected def toDataRecord(value: String): ImageConversionRecord = {
    val media = Option(mediaService.getMediaByName(value))
    media match {
      case Some(m) =>
        val uploadFolder = fileSystemManager.getUploadFolder
        val webpName = replaceWithWebp(value)
        val webpFile = File(uploadFolder, s"images/webp/$webpName")
        val webpCropFile =
          File(uploadFolder, s"images/crop/${replaceWithWebp(value)}")
        if (webpFile.exists() && webpCropFile.exists()) {
          ImageConversionRecord(
            m.getId,
            value,
            webpName,
            "already_converted",
            System.currentTimeMillis()
          )
        } else {
          val resourceFile =
            fileSystemManager.getMediaFilePath(m.getType.getFolder, value)
          webpFile.getParentFile.mkdirs()
          ImmutableImage
            .loader()
            .fromFile(resourceFile)
            .output(WebpWriter.DEFAULT, webpFile)

          webpCropFile.getParentFile.mkdirs()
          ImmutableImage
            .loader()
            .fromFile(resourceFile)
            .scaleTo(16, 16, ScaleMethod.FastScale)
            .output(WebpWriter.DEFAULT, webpCropFile)
          ImageConversionRecord(
            m.getId,
            value,
            webpName,
            "converted",
            System.currentTimeMillis()
          )
        }
      case None =>
        ImageConversionRecord(
          -1L,
          value,
          "",
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

}
