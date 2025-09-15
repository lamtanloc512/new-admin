package org.ltl.enhancement.admin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.format.{Format, FormatDetector}
import com.sksamuel.scrimage.nio.{GifWriter, ImageWriter, JpegWriter, PngWriter}
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
import org.youngmonkeys.ezyplatform.converter.{
  HttpModelToResponseConverter,
  HttpRequestToModelConverter
}
import org.youngmonkeys.ezyplatform.event.*
import org.youngmonkeys.ezyplatform.exception.MediaNotFoundException
import org.youngmonkeys.ezyplatform.io.FolderProxy
import org.youngmonkeys.ezyplatform.manager.FileSystemManager
import org.youngmonkeys.ezyplatform.model.*
import org.youngmonkeys.ezyplatform.service.*
import org.youngmonkeys.ezyplatform.validator.{CommonValidator, MediaValidator}

import java.io.{File, FileInputStream}
import java.nio.file.Files
import javax.servlet.http.HttpServletResponse
import scala.util.{Try, Using}

@EzySingleton
class ImageEnhancementService @EzyAutoBind() (
    httpClient: HttpClient,
    eventHandlerManager: EventHandlerManager,
    fileSystemManager: FileSystemManager,
    resourceDownloadManager: ResourceDownloadManager,
    mediaService: MediaService,
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
    new EzyLazyInitializer(() =>
      singletonFactory.getSingletonCast(classOf[FileUploader])
    )

  private val tika =
    new EzyLazyInitializer(() => TikaConfig())

  def getCompressedMedia(
      requestArguments: RequestArguments,
      name: String,
      exposePrivateMedia: Boolean,
      validMediaCondition: java.util.function.Predicate[MediaModel]
  ): Either[Throwable, Unit] = {

    def writerFor(format: Format): ImageWriter = format match {
      case Format.PNG  => PngWriter.MaxCompression
      case Format.GIF  => GifWriter.Default
      case Format.JPEG => JpegWriter.Default
      case Format.WEBP => WebpWriter.DEFAULT
    }

    def detectFormat(file: File): Either[Throwable, Format] =
      Using(new FileInputStream(file)) { in =>
        FormatDetector.detect(in).orElse(Format.JPEG)
      }.toEither

    def compressAndCache(
        original: File,
        cached: File
    ): Either[Throwable, File] =
      Try {
        val image = ImmutableImage.loader().fromFile(original)
        cached.getParentFile.mkdirs()
        detectFormat(original) match {
          case Left(error) => new MediaNotFoundException(error.getMessage)
          case Right(format) =>
            val writer = writerFor(format)
            image.output(writer, cached)
        }
        cached
      }.toEither

    for {
      _ <- Try(mediaValidator.validateMediaName(name)).toEither

      media <- Option(mediaService.getMediaByName(name))
        .toRight(new MediaNotFoundException(s"Media not found: $name"))

      _ <- Either.cond(
        media.isPublicMedia || exposePrivateMedia || validMediaCondition.test(
          media
        ),
        (),
        new MediaNotFoundException(s"Media not accessible: $name")
      )

      resourceFile <- Try(
        fileSystemManager.getMediaFilePath(media.getType.getFolder, name)
      ).toEither
        .flatMap(f =>
          Either.cond(
            f.exists(),
            f,
            new MediaNotFoundException(s"File not found on disk: $name")
          )
        )

      uploadFolder <- Try(fileSystemManager.getUploadFolder).toEither
      compressedFile = new File(uploadFolder, s"images/compressed/$name")

      cachedFile <-
        if (compressedFile.exists()) Right(compressedFile)
        else compressAndCache(resourceFile, compressedFile)

      _ <- Try {
        val response: HttpServletResponse = requestArguments.getResponse
        val mimeType = Files.probeContentType(cachedFile.toPath) match {
          case null => "application/octet-stream"
          case mt   => mt
        }
        logger.info("Image path: {}", cachedFile.toPath)
        response.setContentType(mimeType)

        val handler = new ResourceRequestHandler(
          cachedFile.toString,
          cachedFile.toString,
          FolderProxy.getFileExtension(name),
          inputStreamLoader,
          resourceDownloadManager
        )
        handler.handle(requestArguments)

      }.toEither

    } yield {
      notifyMediaEvent(new MediaDownloadEvent(media))
      ()
    }
  }

  private def notifyMediaEvent(event: Any): Either[Throwable, Unit] =
    Try(eventHandlerManager.handleEvent(event)).toEither
}
