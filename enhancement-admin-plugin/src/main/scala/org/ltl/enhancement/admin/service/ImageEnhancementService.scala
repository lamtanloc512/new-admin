package org.ltl.enhancement.admin.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.format.{Format, FormatDetector}
import com.sksamuel.scrimage.nio.*
import com.sksamuel.scrimage.webp.WebpWriter
import com.tvd12.ezyfox.bean.EzySingletonFactory
import com.tvd12.ezyfox.bean.annotation.{EzyAutoBind, EzySingleton}
import com.tvd12.ezyfox.concurrent.EzyLazyInitializer
import com.tvd12.ezyfox.stream.EzyInputStreamLoader
import com.tvd12.ezyfox.util.EzyLoggable
import com.tvd12.ezyhttp.client.HttpClient
import com.tvd12.ezyhttp.core.resources.ResourceDownloadManager
import com.tvd12.ezyhttp.server.core.handler.ResourceRequestHandler
import com.tvd12.ezyhttp.server.core.request.RequestArguments
import com.tvd12.ezyhttp.server.core.resources.FileUploader
import org.apache.tika.config.TikaConfig
import org.ltl.enhancement.admin.utils.Common.{replaceWithBmp, replaceWithWebp}
import org.youngmonkeys.ezyplatform.controller.service.MediaControllerService
import org.youngmonkeys.ezyplatform.converter.{
  HttpModelToResponseConverter,
  HttpRequestToModelConverter
}
import org.youngmonkeys.ezyplatform.entity.{MediaType, UploadFrom}
import org.youngmonkeys.ezyplatform.event.*
import org.youngmonkeys.ezyplatform.exception.MediaNotFoundException
import org.youngmonkeys.ezyplatform.io.FolderProxy
import org.youngmonkeys.ezyplatform.manager.FileSystemManager
import org.youngmonkeys.ezyplatform.model.*
import org.youngmonkeys.ezyplatform.pagination.MediaFilter
import org.youngmonkeys.ezyplatform.response.MediaResponse
import org.youngmonkeys.ezyplatform.service.*
import org.youngmonkeys.ezyplatform.validator.{CommonValidator, MediaValidator}

import java.io.{File, FileInputStream}
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.function.Predicate
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Using}

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

  private given ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  private val fileUploaderWrapper =
    EzyLazyInitializer(() =>
      singletonFactory.getSingletonCast(classOf[FileUploader])
    )

  private val tika =
    EzyLazyInitializer(() => TikaConfig())

  def getCompressedImage(
      requestArguments: RequestArguments,
      name: String,
      exposePrivateMedia: Boolean,
      validMediaCondition: java.util.function.Predicate[MediaModel]
  ): Future[Either[scala.Throwable, Unit]] = {
    Future {
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
          else Left(MediaNotFoundException(s"Not found $name"))
        _ <- writeAsyncImageToResponse(
          resourceDownloadManager,
          inputStreamLoader,
          requestArguments,
          name,
          cachedFile
        )
      } yield ()
    }
  }

  def getPlaceholderImage(
      args: RequestArguments,
      name: String,
      exposePrivateMedia: Boolean,
      validMediaCondition: java.util.function.Predicate[MediaModel]
  ): Future[Either[scala.Throwable, Unit]] = {
    Future {
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
        bmpFile = File(uploadFolder, s"images/bmp/${replaceWithBmp(name)}")
        cachedFile <-
          if (bmpFile.exists()) Right(bmpFile)
          else Left(MediaNotFoundException(s"Image not found $name"))
        _ <- writeAsyncImageToResponse(
          resourceDownloadManager,
          inputStreamLoader,
          args,
          name,
          cachedFile
        )
      } yield ()
    }
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

  def removeMedia(imageId: Long): Future[Either[scala.Throwable, Unit]] = {
    Future {
      for {
        media <- Try(mediaService.removeMedia(imageId)).toEither
        file <- Try(
          fileSystemManager.getMediaFilePath(
            media.getType.getFolder,
            media.getName
          )
        ).toEither
        _ <- Try(FolderProxy.deleteFile(file)).toEither
        _ <- removeRelatedImageFiles(media)
        _ <- notifyMediaEvent(MediaRemovedEvent(media))
      } yield ()
    }
  }

  def convertImage(
      format: String = "webp"
  ): Future[Either[scala.Throwable, Unit]] = {
    Future {
      Try {
        Paths
          .get(s"${fileSystemManager.getUploadFolder.getName}/images")
          .toFile
          .listFiles()
          .filter(file => !file.isDirectory && file.isFile)
          .foreach(photo => {
            val newPhoto =
              File(photo.getParentFile, s"${replaceWithWebp(photo.getName)}")
            ImmutableImage
              .loader()
              .fromFile(photo)
              .output(WebpWriter.DEFAULT, newPhoto)
          })
      }.toEither
    }
  }

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

  private def convertToWebpAndCache(
      original: File,
      cached: File
  ): Either[Throwable, File] =
    Try {
      cached.getParentFile.mkdirs()
      logger.info(s"cached: $cached")
      ImmutableImage
        .loader()
        .fromFile(original)
        .output(WebpWriter.DEFAULT, cached)
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
      ResourceRequestHandler(
        cachedFile.toString,
        cachedFile.toString,
        FolderProxy.getFileExtension(cachedFile.getName),
        inputStreamLoader,
        resourceDownloadManager
      ).handle(requestArguments)
    }.toEither
  }

  private def removeRelatedImageFiles(
      image: MediaModel
  ): Either[Throwable, Unit] = {
    Try {
      val webpFile = File(
        fileSystemManager.getUploadFolder,
        s"images/webp/${replaceWithWebp(image.getName)}"
      )
      val bmpFile = File(
        fileSystemManager.getUploadFolder,
        s"images/bmp/${replaceWithBmp(image.getName)}"
      )

      if (webpFile.exists()) {
        FolderProxy.deleteFile(webpFile)
        logger.info(s"Deleted webp file: ${webpFile.getAbsolutePath}")
      }

      if (bmpFile.exists()) {
        FolderProxy.deleteFile(bmpFile)
        logger.info(s"Deleted bmp file: ${bmpFile.getAbsolutePath}")
      }
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
