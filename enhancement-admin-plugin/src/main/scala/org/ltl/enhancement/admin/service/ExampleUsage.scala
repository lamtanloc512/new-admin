package org.ltl.enhancement.admin.service

import scala.concurrent.ExecutionContext
import java.io.File

object ExampleUsage {

  def main(args: Array[String]): Unit = {
  //   // Initialize services with default ffmpeg path
  //   val compressionService = new ImageCompressionService()
  //   val conversionService = new ImageConversionService()

  //   implicit val ec: ExecutionContext = ExecutionContext.global
  //   val mediaService = new ImageEnhancementService(compressionService, conversionService)

  //   println("Media Image Services Example")
  //   println("============================")

  //   // Example 1: Check supported image types
  //   println("\n1. Supported Image Types:")
  //   val testTypes = List("image/png", "image/jpeg", "image/webp", "image/gif", "text/plain")
  //   testTypes.foreach { mimeType =>
  //     val isSupported = mediaService.isImage(mimeType)
  //     println(s"  $mimeType: ${if (isSupported) "✓" else "✗"}")
  //   }

  //   // Example 2: Process an image file (if it exists)
  //   val exampleImage = new File("example.png")
  //   if (exampleImage.exists()) {
  //     println(s"\n2. Processing image: ${exampleImage.getName}")

  //     // Get image as WebP bytes
  //     mediaService.getImage(exampleImage, compress = true, quality = 80) match {
  //       case Right(bytes) =>
  //         println(s"  ✓ Successfully converted to WebP: ${bytes.length} bytes")
  //       case Left(error) =>
  //         println(s"  ✗ Failed to process image: $error")
  //     }

  //     // Process upload (convert and save)
  //     val outputFile = new File("processed.webp")
  //     mediaService.processImageUpload(exampleImage, outputFile, 85) match {
  //       case Right(file) =>
  //         println(s"  ✓ Upload processed: ${file.getAbsolutePath}")
  //       case Left(error) =>
  //         println(s"  ✗ Upload processing failed: $error")
  //     }
  //   } else {
  //     println("\n2. No example.png found - skipping file processing example")
  //   }

  //   // Example 3: Async processing
  //   println("\n3. Async Processing Example:")
  //   val asyncResult = mediaService.getImageAsync(Array[Byte](1, 2, 3), "image/png")
  //   asyncResult.onComplete { result =>
  //     result match {
  //       case scala.util.Success(eitherResult) =>
  //         eitherResult match {
  //           case Right(bytes) =>
  //             println(s"  ✓ Async processing completed: ${bytes.length} bytes")
  //           case Left(error) =>
  //             println(s"  ✗ Async processing failed: $error")
  //         }
  //       case scala.util.Failure(e) =>
  //         println(s"  ✗ Async processing failed: ${e.getMessage}")
  //     }
  //   }

  //   println("\n4. Service Capabilities:")
  //   println("  ✓ Image compression with quality control")
  //   println("  ✓ Format conversion to WebP")
  //   println("  ✓ Automatic image optimization")
  //   println("  ✓ Async processing support")
  //   println("  ✓ Error handling with Try monad")
  //   println("  ✓ Support for PNG, JPEG, ICO, WebP, GIF, BMP, TIFF")

  //   println("\nNote: FFmpeg must be installed on the system for these services to work.")
  //   println("Install FFmpeg: apt-get install ffmpeg (Ubuntu/Debian)")
  //   println("               or brew install ffmpeg (macOS)")
  // 
  }
}