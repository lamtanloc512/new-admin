# Media Image Services

This package provides comprehensive image processing services for the enhancement application using FFmpeg.

## Services Overview

### 1. ImageCompressionService
Handles image compression for supported formats (PNG, JPEG, ICO, WebP).

**Features:**
- Compress images to reduce file size
- Convert to WebP format for better compression
- Resize images to maximum dimensions (1920x1080)
- Configurable quality settings

### 2. ImageConversionService
Converts various image formats to WebP format.

**Features:**
- Convert PNG, JPEG, ICO, GIF, BMP, TIFF to WebP
- Maintain image quality during conversion
- Configurable quality settings

### 3. ImageEnhancementService
Main service that integrates compression and conversion functionality.

**Features:**
- Unified interface for image processing
- Automatic format detection and conversion
- getImage method that always returns WebP format
- Support for both file and byte array inputs
- Asynchronous processing support

## Usage Examples

### Basic Setup
```scala
import org.ltl.enhancement.admin.service._
import scala.concurrent.ExecutionContext

// Initialize services
val ffmpegPath = "/usr/bin/ffmpeg"  // Adjust path as needed
val ffprobePath = "/usr/bin/ffprobe" // Adjust path as needed

val compressionService = new ImageCompressionService(ffmpegPath, ffprobePath)
val conversionService = new ImageConversionService(ffmpegPath, ffprobePath)

implicit val ec: ExecutionContext = ExecutionContext.global
val mediaService = new ImageEnhancementService(compressionService, conversionService)
```

### Compress an Image
```scala
val inputFile = new File("/path/to/input/image.png")
val outputFile = new File("/path/to/output/compressed.webp")

compressionService.compressImage(inputFile, outputFile, 80) match {
  case Success(file) => println(s"Compressed: ${file.getAbsolutePath}")
  case Failure(e) => println(s"Compression failed: ${e.getMessage}")
}
```

### Convert Image to WebP
```scala
val inputFile = new File("/path/to/input/image.jpg")
val outputFile = new File("/path/to/output/image.webp")

conversionService.convertToWebp(inputFile, outputFile, 85) match {
  case Success(file) => println(s"Converted: ${file.getAbsolutePath}")
  case Failure(e) => println(s"Conversion failed: ${e.getMessage}")
}
```

### Get Image as WebP (Main Method)
```scala
// From file
val imageFile = new File("/path/to/image.png")
mediaService.getImage(imageFile, compress = true, quality = 80) match {
  case Success(bytes) => println(s"Got WebP image: ${bytes.length} bytes")
  case Failure(e) => println(s"Failed to get image: ${e.getMessage}")
}

// From byte array
val imageBytes = Files.readAllBytes(Paths.get("/path/to/image.jpg"))
mediaService.getImage(imageBytes, "image/jpeg", compress = true, quality = 80) match {
  case Success(webpBytes) => println(s"Converted to WebP: ${webpBytes.length} bytes")
  case Failure(e) => println(s"Failed to convert: ${e.getMessage}")
}
```

### Process Image Upload
```scala
val uploadedFile = new File("/tmp/uploaded_image.png")
val processedFile = new File("/var/media/processed_image.webp")

mediaService.processImageUpload(uploadedFile, processedFile, 80) match {
  case Success(file) => println(s"Upload processed: ${file.getAbsolutePath}")
  case Failure(e) => println(s"Processing failed: ${e.getMessage}")
}
```

## Supported Image Types

- `image/png`
- `image/jpeg`
- `image/vnd.microsoft.icon` (ICO)
- `image/webp`
- `image/gif`
- `image/bmp`
- `image/tiff`

## Dependencies

- FFmpeg CLI tool must be installed on the system
- No additional Maven dependencies required (uses ProcessBuilder to call FFmpeg)

## Configuration

- **ffmpegPath**: Path to FFmpeg executable (default: "ffmpeg")
- **ffprobePath**: Path to FFprobe executable (default: "ffprobe")
- **quality**: Compression quality (0-100, default: 80)
- **maxWidth**: Maximum image width (default: 1920)
- **maxHeight**: Maximum image height (default: 1080)

## Error Handling

All methods return `Try[T]` for proper error handling:
- `Success(result)`: Operation completed successfully
- `Failure(exception)`: Operation failed with the provided exception

## Async Support

All services support asynchronous operations using Scala Futures:
```scala
mediaService.getImageAsync(imageFile).map { bytes =>
  // Handle the WebP bytes
}.recover {
  case e => println(s"Async operation failed: ${e.getMessage}")
}
```

## Integration with AdminApiMediaController

These services are designed to integrate with the existing `AdminApiMediaController` to provide:
- Automatic image optimization on upload
- Consistent WebP output format
- Reduced storage and bandwidth usage
- Better web performance

## Testing

Run the test suite with:
```bash
mvn test
```

The test file `ImageEnhancementServiceTest.java` provides examples of service usage and validation.