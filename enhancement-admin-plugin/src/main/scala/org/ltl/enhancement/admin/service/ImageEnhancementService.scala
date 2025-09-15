package org.ltl.enhancement.admin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.webp.WebpWriter
import com.tvd12.ezyfox.bean.EzySingletonFactory
import com.tvd12.ezyfox.bean.annotation.{EzyAutoBind, EzySingleton}
import com.tvd12.ezyfox.concurrent.EzyLazyInitializer
import com.tvd12.ezyfox.stream.EzyInputStreamLoader
import com.tvd12.ezyhttp.client.HttpClient
import com.tvd12.ezyhttp.core.exception.HttpNotAcceptableException
import com.tvd12.ezyhttp.core.resources.ResourceDownloadManager
import com.tvd12.ezyhttp.server.core.handler.ResourceRequestHandler
import com.tvd12.ezyhttp.server.core.request.RequestArguments
import com.tvd12.ezyhttp.server.core.resources.FileUploader
import org.apache.tika.config.TikaConfig
import org.youngmonkeys.ezyplatform.converter.{
  HttpModelToResponseConverter,
  HttpRequestToModelConverter
}
import org.youngmonkeys.ezyplatform.data.FileMetadata
import org.youngmonkeys.ezyplatform.entity.{MediaType, UploadFrom}
import org.youngmonkeys.ezyplatform.event.*
import org.youngmonkeys.ezyplatform.exception.MediaNotFoundException
import org.youngmonkeys.ezyplatform.io.FolderProxy
import org.youngmonkeys.ezyplatform.manager.FileSystemManager
import org.youngmonkeys.ezyplatform.model.*
import org.youngmonkeys.ezyplatform.service.*
import org.youngmonkeys.ezyplatform.validator.{CommonValidator, MediaValidator}

import scala.jdk.CollectionConverters.*
import scala.util.Try
import java.io.File
import java.util.function.Predicate
import javax.servlet.http

@EzySingleton
class MediaControllerService @EzyAutoBind() (
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
) {

  private val fileUploaderWrapper =
    new EzyLazyInitializer(() =>
      singletonFactory.getSingletonCast(classOf[FileUploader])
    )

  private val tika =
    new EzyLazyInitializer(() => TikaConfig())

  def addMedia(
      request: http.HttpServletRequest,
      response: http.HttpServletResponse,
      uploadFrom: UploadFrom,
      ownerId: Long,
      avatar: Boolean,
      notPublic: Boolean
  ): Unit = {
    val fileUploader = fileUploaderWrapper.get()
    if (fileUploader == null)
      throw new HttpNotAcceptableException(
        Map("fileUpload" -> "disabled").asJava
      )

    val filePartEither = Option(request.getPart("file"))
      .orElse(request.getParts.asScala.headOption) match {
      case Some(fp) => Right(fp)
      case None =>
        Left(new HttpNotAcceptableException(Map("file" -> "missing").asJava))
    }

    filePartEither match {
      case Right(filePart) =>
        val fileMetadataEither = Try(
          mediaValidator.validateFilePart(filePart, avatar)
        ).toEither.left.map(e =>
          new HttpNotAcceptableException(Map("file" -> e.getMessage).asJava)
        )

        fileMetadataEither match {
          case Right(fileMetadata) =>
            eventHandlerManager.handleEvent(
              new MediaUploadEvent(uploadFrom, ownerId, fileMetadata)
            )

            val submittedFileName = filePart.getSubmittedFileName
            val extension = fileMetadata.getExtension
            val containerFolder = fileMetadata.getMediaType.getFolder
            val newFileName =
              mediaService.generateMediaFileName(submittedFileName, extension)

            val asyncContext = request.getAsyncContext
            val mediaFilePath =
              fileSystemManager.getMediaFilePath(containerFolder, newFileName)

            fileUploader.accept(
              asyncContext,
              filePart,
              mediaFilePath,
              settingService.getMaxUploadFileSize,
              () => {
                val finalFile = if (
                  fileMetadata.getMediaType == MediaType.IMAGE || fileMetadata.getMediaType == MediaType.AVATAR
                ) {
                  convertToWebp(mediaFilePath) match {
                    case Right(webpFile) => webpFile
                    case Left(e) =>
                      println(s"WebP conversion failed: ${e.getMessage}")
                      mediaFilePath
                  }
                } else mediaFilePath

                val model = saveMediaInformation(
                  uploadFrom,
                  ownerId,
                  submittedFileName,
                  newFileName,
                  fileMetadata,
                  notPublic
                )
                notifyMediaEvent(
                  new MediaUploadedEvent(model, finalFile)
                ) match {
                  case Right(_) => ()
                  case Left(e) =>
                    println(
                      s"[WARN] notify media event failed: ${e.getMessage}"
                    )
                }
                val respBytes = objectMapper.writeValueAsBytes(model)
                response.getOutputStream.write(respBytes)
              }
            )
          case Left(e) => throw e
        }
      case Left(e) => throw e
    }
  }

  private def convertToWebp(originalFile: File): Either[Throwable, File] = {
    Try {
      val img = ImmutableImage.loader().fromFile(originalFile)
      val webpFile = new File(
        originalFile.getParent,
        originalFile.getName.replaceAll("\\.[^.]+$", "") + ".webp"
      )
      img.output(WebpWriter.MAX_LOSSLESS_COMPRESSION, webpFile)
      originalFile.delete()
      webpFile
    }.toEither
  }

  def getMedia(
      requestArguments: RequestArguments,
      name: String,
      exposePrivateMedia: Boolean,
      validMediaCondition: Predicate[MediaModel]
  ): Either[MediaNotFoundException, Unit] = {
    mediaValidator.validateMediaName(name)
    Option(mediaService.getMediaByName(name)) match {
      case Some(media)
          if media.isPublicMedia || exposePrivateMedia || validMediaCondition
            .test(media) =>
        notifyMediaEvent(new MediaDownloadEvent(media)) match {
          case Right(_) => ()
          case Left(e) =>
            println(s"[WARN] notify media event failed: ${e.getMessage}")
        }
        val mediaType = media.getType

        convertToWebp(
          fileSystemManager.getMediaFilePath(mediaType.getFolder, name)
        )
          .fold(
            err => Left(MediaNotFoundException(err.getMessage)),
            webp => {
              val extension = FolderProxy.getFileExtension(name)
              val handler = new ResourceRequestHandler(
                webp.toString,
                webp.toString,
                extension,
                inputStreamLoader,
                resourceDownloadManager
              )
              handler.handle(requestArguments)
              Right(())
            }
          )
      case _ => Left(new MediaNotFoundException(name))
    }
  }

  private def saveMediaInformation(
      uploadFrom: UploadFrom,
      ownerId: Long,
      submittedFileName: String,
      fileName: String,
      fileMetadata: FileMetadata,
      notPublic: Boolean
  ) =
    mediaService.addMedia(
      AddMediaModel
        .builder()
        .ownerId(ownerId)
        .fileName(fileName)
        .originalFileName(submittedFileName)
        .mediaType(fileMetadata.getMediaType)
        .mimeType(fileMetadata.getMimeType)
        .notPublic(notPublic)
        .build(),
      uploadFrom
    )

  private def notifyMediaEvent(event: Any): Either[Throwable, Unit] =
    Try(eventHandlerManager.handleEvent(event)).toEither
}
