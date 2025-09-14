package org.ltl.enhancement.admin.service

import com.tvd12.ezyfox.bean.annotation.EzySingleton

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import scala.sys.process.*
import scala.concurrent.ExecutionContext.Implicits.global

@EzySingleton
class ImageCompressionService {
  private val ffmpegPath: String = "ffmpeg"

  private val supportedTypes = Set(
    "image/png",
    "image/jpeg",
    "image/vnd.microsoft.icon",
    "image/webp"
  )

  def isSupported(mimeType: String): Boolean = supportedTypes.contains(mimeType)

  def compressImage(inputFile: File, outputFile: File): Either[String, File] = {
    compressImage(inputFile, outputFile, 80)
  }

  def compressImage(
      inputFile: File,
      outputFile: File,
      quality: Int
  ): Either[String, File] = {
    val mimeType = getMimeType(inputFile)
    if (!isSupported(mimeType)) {
      return Left(s"Unsupported image type: $mimeType")
    }

    runFFmpegCompression(inputFile, outputFile, quality)
  }

  def compressImage(
      inputPath: String,
      outputPath: String
  ): Either[String, File] = {
    compressImage(new File(inputPath), new File(outputPath), 80)
  }

  def compressImage(
      inputPath: String,
      outputPath: String,
      quality: Int
  ): Either[String, File] = {
    compressImage(new File(inputPath), new File(outputPath), quality)
  }

  def compressImageToBytes(inputFile: File): Either[String, Array[Byte]] = {
    compressImageToBytes(inputFile, 80)
  }

  def compressImageToBytes(
      inputFile: File,
      quality: Int
  ): Either[String, Array[Byte]] = {
    val tempOutput = File.createTempFile("compressed_", ".webp")
    tempOutput.deleteOnExit()

    compressImage(inputFile, tempOutput, quality) match {
      case Right(_) =>
        val bytes = Files.readAllBytes(tempOutput.toPath)
        tempOutput.delete()
        Right(bytes)
      case Left(error) =>
        tempOutput.delete()
        Left(error)
    }
  }

  private def runFFmpegCompression(
      inputFile: File,
      outputFile: File,
      quality: Int
  ): Either[String, File] = {
    val command = Seq(
      ffmpegPath,
      "-i",
      inputFile.getAbsolutePath,
      "-vf",
      s"scale='min(1920,iw)':'min(1080,ih)':force_original_aspect_ratio=decrease",
      "-c:v",
      "libwebp",
      "-quality",
      quality.toString,
      "-y",
      outputFile.getAbsolutePath
    )

    try {
      val process = Process(command).run()
      val exitCode = process.exitValue()

      if (exitCode == 0 && outputFile.exists()) {
        Right(outputFile)
      } else {
        Left(s"FFmpeg compression failed with exit code: $exitCode")
      }
    } catch {
      case e: Exception =>
        Left(s"FFmpeg execution failed: ${e.getMessage}")
    }
  }

  private def getMimeType(file: File): String = {
    val mimeType = Files.probeContentType(file.toPath)
    if (mimeType != null) mimeType else "application/octet-stream"
  }
}
