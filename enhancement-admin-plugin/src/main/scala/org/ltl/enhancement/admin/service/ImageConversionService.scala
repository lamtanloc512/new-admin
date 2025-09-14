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
class ImageConversionService {
  private val ffmpegPath: String = "ffmpeg"
  private val supportedInputTypes = Set(
    "image/png",
    "image/jpeg",
    "image/vnd.microsoft.icon",
    "image/gif",
    "image/bmp",
    "image/tiff"
  )

  def isSupportedInput(mimeType: String): Boolean =
    supportedInputTypes.contains(mimeType)

  def convertToWebp(inputFile: File, outputFile: File): Either[String, File] = {
    convertToWebp(inputFile, outputFile, 80)
  }

  def convertToWebp(
      inputFile: File,
      outputFile: File,
      quality: Int
  ): Either[String, File] = {
    val mimeType = getMimeType(inputFile)
    if (!isSupportedInput(mimeType)) {
      return Left(s"Unsupported input image type: $mimeType")
    }

    runFFmpegConversion(inputFile, outputFile, quality)
  }

  def convertToWebp(
      inputPath: String,
      outputPath: String
  ): Either[String, File] = {
    convertToWebp(new File(inputPath), new File(outputPath), 80)
  }

  def convertToWebp(
      inputPath: String,
      outputPath: String,
      quality: Int
  ): Either[String, File] = {
    convertToWebp(new File(inputPath), new File(outputPath), quality)
  }

  def convertToWebpBytes(inputFile: File): Either[String, Array[Byte]] = {
    convertToWebpBytes(inputFile, 80)
  }

  def convertToWebpBytes(
      inputFile: File,
      quality: Int
  ): Either[String, Array[Byte]] = {
    val tempOutput = File.createTempFile("converted_", ".webp")
    tempOutput.deleteOnExit()

    convertToWebp(inputFile, tempOutput, quality) match {
      case Right(_) =>
        val bytes = Files.readAllBytes(tempOutput.toPath)
        tempOutput.delete()
        Right(bytes)
      case Left(error) =>
        tempOutput.delete()
        Left(error)
    }
  }

  def convertToWebpBytes(
      inputBytes: Array[Byte]
  ): Either[String, Array[Byte]] = {
    convertToWebpBytes(inputBytes, 80)
  }

  def convertToWebpBytes(
      inputBytes: Array[Byte],
      quality: Int
  ): Either[String, Array[Byte]] = {
    val tempInput = File.createTempFile("input_", ".tmp")
    val tempOutput = File.createTempFile("converted_", ".webp")
    tempInput.deleteOnExit()
    tempOutput.deleteOnExit()

    try {
      Files.write(tempInput.toPath, inputBytes)
      convertToWebp(tempInput, tempOutput, quality) match {
        case Right(_) =>
          val resultBytes = Files.readAllBytes(tempOutput.toPath)
          tempInput.delete()
          tempOutput.delete()
          Right(resultBytes)
        case Left(error) =>
          tempInput.delete()
          tempOutput.delete()
          Left(error)
      }
    } catch {
      case e: Exception =>
        tempInput.delete()
        tempOutput.delete()
        Left(s"Conversion failed: ${e.getMessage}")
    }
  }

  private def runFFmpegConversion(
      inputFile: File,
      outputFile: File,
      quality: Int
  ): Either[String, File] = {
    val command = Seq(
      ffmpegPath,
      "-i",
      inputFile.getAbsolutePath,
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
        Left(s"FFmpeg conversion failed with exit code: $exitCode")
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
