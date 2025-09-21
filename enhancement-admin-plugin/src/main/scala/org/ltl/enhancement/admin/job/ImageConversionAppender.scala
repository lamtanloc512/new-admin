package org.ltl.enhancement.admin.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.color.Colors
import com.sksamuel.scrimage.composite.AlphaComposite
import com.sksamuel.scrimage.filter.LensBlurFilter
import com.sksamuel.scrimage.nio.BmpWriter
import com.sksamuel.scrimage.webp.WebpWriter
import com.tvd12.ezyfox.bean.annotation.{EzyAutoBind, EzySingleton}
import org.youngmonkeys.ezyplatform.admin.appender.AdminDataAppender
import org.youngmonkeys.ezyplatform.admin.service.AdminSettingService
import org.youngmonkeys.ezyplatform.controller.service.MediaControllerService
import org.youngmonkeys.ezyplatform.entity.MediaType
import org.youngmonkeys.ezyplatform.manager.FileSystemManager
import org.youngmonkeys.ezyplatform.pagination.DefaultMediaFilter
import org.youngmonkeys.ezyplatform.service.MediaService
import com.sksamuel.scrimage.color.RGBColor

import java.awt.Color
import java.io.File
import java.util
import java.util.Base64
import scala.jdk.CollectionConverters.*
import scala.util.Try

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
        val bmpFile = File(uploadFolder, s"images/bmp/${replaceWithBmp(value)}")
        if (webpFile.exists() && bmpFile.exists()) {
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

          bmpFile.getParentFile.mkdirs()
          val x = ImmutableImage
            .loader()
            .fromFile(resourceFile)
            .scale(0.01)
            .autocrop()
            .output(BmpWriter(), bmpFile)

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

  private def replaceWithBmp(fileName: String): String = {
    fileName.lastIndexOf('.') match {
      case -1 => s"$fileName.bmp"
      case i  => fileName.substring(0, i) + ".bmp"
    }
  }
}
