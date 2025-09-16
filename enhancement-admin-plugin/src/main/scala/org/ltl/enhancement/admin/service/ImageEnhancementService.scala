package org.ltl.enhancement.admin.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.format.Format
import com.sksamuel.scrimage.format.FormatDetector
import com.sksamuel.scrimage.nio.GifWriter
import com.sksamuel.scrimage.nio.ImageWriter
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.webp.WebpWriter
import com.tvd12.ezyfox.bean.EzySingletonFactory
import com.tvd12.ezyfox.bean.annotation.EzyAutoBind
import com.tvd12.ezyfox.bean.annotation.EzySingleton
import com.tvd12.ezyfox.concurrent.EzyLazyInitializer
import com.tvd12.ezyfox.stream.EzyInputStreamLoader
import com.tvd12.ezyfox.util.EzyLoggable
import com.tvd12.ezyhttp.client.HttpClient
import com.tvd12.ezyhttp.core.resources.ResourceDownloadManager
import com.tvd12.ezyhttp.server.core.handler.ResourceRequestHandler
import com.tvd12.ezyhttp.server.core.request.RequestArguments
import com.tvd12.ezyhttp.server.core.resources.FileUploader
import net.bytebuddy.implementation.bytecode.Throw
import org.apache.tika.config.TikaConfig
import org.apache.tika.mime.MimeType
import org.youngmonkeys.ezyplatform.controller.service.MediaControllerService
import org.youngmonkeys.ezyplatform.converter.HttpModelToResponseConverter
import org.youngmonkeys.ezyplatform.converter.HttpRequestToModelConverter
import org.youngmonkeys.ezyplatform.entity.MediaType
import org.youngmonkeys.ezyplatform.entity.UploadFrom
import org.youngmonkeys.ezyplatform.event.*
import org.youngmonkeys.ezyplatform.exception.MediaNotFoundException
import org.youngmonkeys.ezyplatform.io.FolderProxy
import org.youngmonkeys.ezyplatform.manager.FileSystemManager
import org.youngmonkeys.ezyplatform.model.*
import org.youngmonkeys.ezyplatform.pagination.MediaFilter
import org.youngmonkeys.ezyplatform.response.MediaResponse
import org.youngmonkeys.ezyplatform.service.*
import org.youngmonkeys.ezyplatform.validator.CommonValidator
import org.youngmonkeys.ezyplatform.validator.MediaValidator

import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.util.function.Predicate
import scala.jdk.CollectionConverters.*
import scala.util.Try
import scala.util.Using

case class ImageResponse(
    @JsonProperty("id") id: Long,
    @JsonProperty("name") name: String,
    @JsonProperty("url") url: String,
    @JsonProperty("originalName") originalName: String,
    @JsonProperty("uploadFrom") uploadFrom: UploadFrom,
    @JsonProperty("type") `type`: MediaType,
    @JsonProperty("mimeType") mimeType: String,
    @JsonProperty("title") title: String,
    @JsonProperty("caption") caption: String,
    @JsonProperty("alternativeText") alternativeText: String,
    @JsonProperty("description") description: String,
    @JsonProperty("publicMedia") publicMedia: Boolean,
    @JsonProperty("createdAt") createdAt: Long,
    @JsonProperty("updatedAt") updatedAt: Long
)

@EzySingleton
class ImageEnhancementService @EzyAutoBind() (
    httpClient: HttpClient,
    eventHandlerManager: EventHandlerManager,
    fileSystemManager: FileSystemManager,
    resourceDownloadManager: ResourceDownloadManager,
    mediaService: MediaService,
    mediaControllerService: MediaControllerService,
    paginationMediaService: PaginationMediaService,
    settingService: SettingService,
    commonValidator: CommonValidator,
    mediaValidator: MediaValidator,
    requestToModelConverter: HttpRequestToModelConverter,
    modelToResponseConverter: HttpModelToResponseConverter,
    singletonFactory: EzySingletonFactory,
    inputStreamLoader: EzyInputStreamLoader,
    objectMapper: ObjectMapper
) extends EzyLoggable {

  private val fileUploaderWrapper =
    EzyLazyInitializer(() =>
      singletonFactory.getSingletonCast(classOf[FileUploader])
    )

  private val tika =
    EzyLazyInitializer(() => TikaConfig())

  def getCompressedMedia(
      requestArguments: RequestArguments,
      name: String,
      exposePrivateMedia: Boolean,
      validMediaCondition: java.util.function.Predicate[MediaModel]
  ): Either[Throwable, Unit] = {
    for {
      _ <- Try(mediaValidator.validateMediaName(name)).toEither
      media <- getImageByName(mediaService, name)
      _ <- checkMediaAccessible(
        name,
        exposePrivateMedia,
        validMediaCondition,
        media
      )
      _ <- notifyMediaEvent(MediaDownloadEvent(media))
      extension = FolderProxy.getFileExtension(name)
      resourceFile <- getResourceFile(fileSystemManager, name, media)
      uploadFolder <- Try(fileSystemManager.getUploadFolder).toEither
      cachedFile <- extension match {
        case "webp" =>
          val webpFile = File(uploadFolder, s"images/webp/$name")
          if (webpFile.exists()) Right(webpFile)
          else convertToWebpAndCache(resourceFile, webpFile)
        case _ =>
          val compressedFile = File(uploadFolder, s"images/compressed/$name")
          if (compressedFile.exists()) Right(compressedFile)
          else compressAndCache(resourceFile, compressedFile)
      }
      _ <- writeAsyncImageToResponse(
        resourceDownloadManager,
        inputStreamLoader,
        requestArguments,
        name,
        cachedFile
      )
    } yield ()

  }

  def getWebpImage(
      requestArguments: RequestArguments,
      name: String,
      exposePrivateMedia: Boolean,
      validMediaCondition: java.util.function.Predicate[MediaModel]
  ): Either[Throwable, Unit] = {
    for {
      _ <- Try(mediaValidator.validateMediaName(name)).toEither
      media <- getImageByName(mediaService, name)
      _ <- checkMediaAccessible(
        name,
        exposePrivateMedia,
        validMediaCondition,
        media
      )
      _ <- notifyMediaEvent(MediaDownloadEvent(media))
      resourceFile <- getResourceFile(fileSystemManager, name, media)
      uploadFolder <- Try(fileSystemManager.getUploadFolder).toEither
      webpFile = File(uploadFolder, s"images/webp/${replaceWithWebp(name)}")
      cachedFile <-
        if (webpFile.exists()) Right(webpFile)
        else convertToWebpAndCache(resourceFile, webpFile)
      _ <- writeAsyncImageToResponse(
        resourceDownloadManager,
        inputStreamLoader,
        requestArguments,
        name,
        cachedFile
      )
    } yield ()

  }

  def getMediaListWebp(
      filter: MediaFilter,
      nextPageToken: String,
      prevPageToken: String,
      lastPage: Boolean,
      limit: Int
  ): PaginationModel[ImageResponse] = {
    mediaControllerService
      .getMediaList(
        filter,
        nextPageToken,
        prevPageToken,
        lastPage,
        limit
      )
      .map(toImageResponse)
  }

  private def replaceWithWebp(fileName: String): String =
    fileName.lastIndexOf('.') match
      case -1 => s"$fileName.webp"
      case i  => fileName.substring(0, i) + ".webp"

  private def writerFor(format: Format): ImageWriter = format match {
    case Format.PNG  => PngWriter.MaxCompression
    case Format.GIF  => GifWriter.Progressive
    case Format.JPEG => JpegWriter.CompressionFromMetaData
    case Format.WEBP => WebpWriter.MAX_LOSSLESS_COMPRESSION
  }

  private def detectFormat(file: File): Either[Throwable, Format] =
    Using(FileInputStream(file)) { in =>
      FormatDetector.detect(in).orElse(Format.JPEG)
    }.toEither

  private def compressAndCache(
      original: File,
      cached: File
  ): Either[Throwable, File] =
    Try {
      val image = ImmutableImage.loader().fromFile(original)
      cached.getParentFile.mkdirs()
      detectFormat(original) match {
        case Left(error) => throw MediaNotFoundException(error.getMessage)
        case Right(format) =>
          image.output(writerFor(format), cached)
      }
      cached
    }.toEither

  private def convertToWebpAndCache(
      original: File,
      cached: File
  ): Either[Throwable, File] =
    Try {
      cached.getParentFile.mkdirs()
      ImmutableImage
        .loader()
        .fromFile(original)
        .output(WebpWriter.MAX_LOSSLESS_COMPRESSION, cached)
      cached
    }.toEither

  private def notifyMediaEvent(event: Any): Either[Throwable, Unit] =
    Try(eventHandlerManager.handleEvent(event)).toEither

  private def getResourceFile(
      fileSystemManager: FileSystemManager,
      name: String,
      media: MediaModel
  ) = {
    Try(
      fileSystemManager.getMediaFilePath(media.getType.getFolder, name)
    ).toEither
      .flatMap(f =>
        Either.cond(
          f.exists(),
          f,
          MediaNotFoundException(s"File not found on disk: $name")
        )
      )
  }

  private def checkMediaAccessible(
      name: String,
      exposePrivateMedia: Boolean,
      validMediaCondition: Predicate[MediaModel],
      media: MediaModel
  ) = {
    Either.cond(
      media.isPublicMedia || exposePrivateMedia || validMediaCondition.test(
        media
      ),
      (),
      MediaNotFoundException(s"Media not accessible: $name")
    )
  }

  private def getImageByName(mediaService: MediaService, name: String) = {
    Option(mediaService.getMediaByName(name))
      .toRight(MediaNotFoundException(s"Media not found: $name"))
  }

  private def writeAsyncImageToResponse(
      resourceDownloadManager: ResourceDownloadManager,
      inputStreamLoader: EzyInputStreamLoader,
      requestArguments: RequestArguments,
      name: String,
      cachedFile: File
  ) = {
    Try {
      val response = requestArguments.getResponse
      val mimeType = Files.probeContentType(cachedFile.toPath) match {
        case null => "application/octet-stream"
        case mt   => mt
      }

      response.setContentType(mimeType)
      response.setHeader("Content-Disposition", s"""inline; filename="$name"""")
      ResourceRequestHandler(
        cachedFile.toString,
        cachedFile.toString,
        FolderProxy.getFileExtension(name),
        inputStreamLoader,
        resourceDownloadManager
      ).handle(requestArguments)
    }.toEither
  }

  private def toImageResponse(media: MediaResponse): ImageResponse = {
    ImageResponse(
      id = media.getId,
      name = replaceWithWebp(media.getName),
      url = media.getUrl,
      originalName = media.getOriginalName,
      uploadFrom = media.getUploadFrom,
      `type` = media.getType,
      mimeType = media.getMimeType,
      title = media.getTitle,
      caption = media.getCaption,
      alternativeText = media.getAlternativeText,
      description = media.getDescription,
      publicMedia = media.isPublicMedia,
      createdAt = media.getCreatedAt,
      updatedAt = media.getUpdatedAt
    )
  }

}
