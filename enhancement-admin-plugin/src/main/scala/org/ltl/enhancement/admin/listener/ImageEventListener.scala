package org.ltl.enhancement.admin.listener

import com.tvd12.ezyfox.bean.annotation.{EzyAutoBind, EzySingleton}
import org.ltl.enhancement.admin.utils.Common.{replaceWithBmp, replaceWithWebp}
import org.youngmonkeys.ezyplatform.event.{
  AbstractEventHandler,
  MediaUpdatedEvent
}
import org.youngmonkeys.ezyplatform.io.FolderProxy
import org.youngmonkeys.ezyplatform.manager.FileSystemManager
import org.youngmonkeys.ezyplatform.service.MediaService

import java.io.File
import scala.util.Try

@EzySingleton
class ImageEventListener @EzyAutoBind() (
    mediaService: MediaService,
    fileSystemManager: FileSystemManager
) extends AbstractEventHandler[MediaUpdatedEvent, Unit] {
  override def processEventData(data: MediaUpdatedEvent): Unit = {
    for {
      media <- Option(mediaService.getMediaById(data.getMediaId))
      _ <- Try(removeRelatedImage(media.getName)).toOption
    } yield ()
  }

  private def removeRelatedImage(name: String): Unit = {
    val webpFile =
      File(
        fileSystemManager.getUploadFolder,
        s"images/webp/${replaceWithWebp(name)}"
      )
    if (webpFile.exists()) {
      FolderProxy.deleteFile(webpFile)
    }

    val bmpFile =
      File(
        fileSystemManager.getUploadFolder,
        s"images/crop/${replaceWithWebp(name)}"
      )
    if (bmpFile.exists()) {
      FolderProxy.deleteFile(bmpFile)
    }
  }
}
