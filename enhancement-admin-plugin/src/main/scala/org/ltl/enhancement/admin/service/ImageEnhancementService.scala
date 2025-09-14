package org.ltl.enhancement.admin.service

import com.tvd12.ezyfox.bean.annotation.EzyAutoBind
import com.tvd12.ezyfox.bean.annotation.EzySingleton

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@EzySingleton
class ImageEnhancementService @EzyAutoBind() (
    compressionService: ImageCompressionService,
    conversionService: ImageConversionService
) {

  // Supported image MIME types
  private val supportedImageTypes = Set(
    "image/png",
    "image/jpeg",
    "image/vnd.microsoft.icon",
    "image/webp",
    "image/gif",
    "image/bmp",
    "image/tiff"
  )

  def isImage(mimeType: String): Boolean =
    supportedImageTypes.contains(mimeType)

  /** Get image as webp format. If the image is already webp, return as is. If
    * it's another supported format, convert to webp. Applies compression if
    * requested.
    */
  def getImage(
      imageFile: File,
      compress: Boolean = true,
      quality: Int = 80
  ): Either[String, Array[Byte]] = {
    for {
      mimeType <- validateMimeType(imageFile)
      bytes <- processImage(imageFile, mimeType, compress, quality)
    } yield bytes
  }

  private def validateMimeType(file: File): Either[String, String] = {
    val mimeType = getMimeType(file)
    Either.cond(
      isImage(mimeType),
      mimeType,
      s"Unsupported image type: $mimeType"
    )
  }

  private def processImage(
      file: File,
      mimeType: String,
      compress: Boolean,
      quality: Int
  ): Either[String, Array[Byte]] = {
    (mimeType, compress) match {
      case ("image/webp", false) => readFileBytes(file)
      case ("image/webp", true) =>
        compressionService.compressImageToBytes(file, quality)
      case (_, _) => processConversion(file, compress, quality)
    }
  }

  private def processConversion(
      file: File,
      compress: Boolean,
      quality: Int
  ): Either[String, Array[Byte]] = {
    if (compress) {
      for {
        convertedBytes <- conversionService.convertToWebpBytes(file, quality)
        compressedBytes <- compressBytes(convertedBytes, quality)
      } yield compressedBytes
    } else {
      conversionService.convertToWebpBytes(file, quality)
    }
  }

  private def compressBytes(
      bytes: Array[Byte],
      quality: Int
  ): Either[String, Array[Byte]] = {
    val tempFile = File.createTempFile("temp_webp_", ".webp")
    tempFile.deleteOnExit()

    try {
      Files.write(tempFile.toPath, bytes)
      val result = compressionService.compressImageToBytes(tempFile, quality)
      tempFile.delete()
      result
    } catch {
      case e: Exception =>
        tempFile.delete()
        Left(s"Compression failed: ${e.getMessage}")
    }
  }

  private def readFileBytes(file: File): Either[String, Array[Byte]] = {
    try {
      Right(Files.readAllBytes(file.toPath))
    } catch {
      case e: Exception => Left(s"Failed to read file: ${e.getMessage}")
    }
  }

  /** Get image as webp format from byte array
    */
  def getImage(
      imageBytes: Array[Byte],
      mimeType: String
  ): Either[String, Array[Byte]] = {
    getImage(imageBytes, mimeType, true, 80)
  }

  def getImage(
      imageBytes: Array[Byte],
      mimeType: String,
      compress: Boolean
  ): Either[String, Array[Byte]] = {
    getImage(imageBytes, mimeType, compress, 80)
  }

  def getImage(
      imageBytes: Array[Byte],
      mimeType: String,
      compress: Boolean,
      quality: Int
  ): Either[String, Array[Byte]] = {
    for {
      _ <- Either.cond(
        isImage(mimeType),
        (),
        s"Unsupported image type: $mimeType"
      )
      result <- processBytes(imageBytes, mimeType, compress, quality)
    } yield result
  }

  private def processBytes(
      bytes: Array[Byte],
      mimeType: String,
      compress: Boolean,
      quality: Int
  ): Either[String, Array[Byte]] = {
    (mimeType, compress) match {
      case ("image/webp", false) => Right(bytes)
      case ("image/webp", true)  => compressByteArray(bytes, quality)
      case (_, _) => processByteConversion(bytes, compress, quality)
    }
  }

  private def processByteConversion(
      bytes: Array[Byte],
      compress: Boolean,
      quality: Int
  ): Either[String, Array[Byte]] = {
    if (compress) {
      for {
        convertedBytes <- conversionService.convertToWebpBytes(bytes, quality)
        compressedBytes <- compressByteArray(convertedBytes, quality)
      } yield compressedBytes
    } else {
      conversionService.convertToWebpBytes(bytes, quality)
    }
  }

  private def compressByteArray(
      bytes: Array[Byte],
      quality: Int
  ): Either[String, Array[Byte]] = {
    val tempFile = File.createTempFile("temp_webp_", ".webp")
    tempFile.deleteOnExit()

    try {
      Files.write(tempFile.toPath, bytes)
      val result = compressionService.compressImageToBytes(tempFile, quality)
      tempFile.delete()
      result
    } catch {
      case e: Exception =>
        tempFile.delete()
        Left(s"Compression failed: ${e.getMessage}")
    }
  }

  /** Process image upload - convert to webp and compress
    */
  def processImageUpload(
      inputFile: File,
      outputFile: File,
      quality: Int = 80
  ): Either[String, File] = {
    val mimeType = getMimeType(inputFile)

    if (!isImage(mimeType)) {
      return Left(s"Unsupported image type: $mimeType")
    }

    if (mimeType == "image/webp") {
      // Already webp, just compress
      compressionService.compressImage(inputFile, outputFile, quality)
    } else {
      // Convert to webp with compression
      conversionService.convertToWebp(inputFile, outputFile, quality)
    }
  }

  /** Async version of getImage
    */
  def getImageAsync(
      imageFile: File,
      compress: Boolean = true,
      quality: Int = 80
  ): Future[Either[String, Array[Byte]]] = {
    Future(getImage(imageFile, compress, quality))
  }

  /** Async version of getImage from bytes
    */
  def getImageAsync(
      imageBytes: Array[Byte],
      mimeType: String
  ): Future[Either[String, Array[Byte]]] = {
    Future(getImage(imageBytes, mimeType, true, 80))
  }

  def getImageAsync(
      imageBytes: Array[Byte],
      mimeType: String,
      compress: Boolean
  ): Future[Either[String, Array[Byte]]] = {
    Future(getImage(imageBytes, mimeType, compress, 80))
  }

  def getImageAsync(
      imageBytes: Array[Byte],
      mimeType: String,
      compress: Boolean,
      quality: Int
  ): Future[Either[String, Array[Byte]]] = {
    Future(getImage(imageBytes, mimeType, compress, quality))
  }

  private def getMimeType(file: File): String = {
    val mimeType = Files.probeContentType(file.toPath)
    if (mimeType != null) mimeType else "application/octet-stream"
  }
}
