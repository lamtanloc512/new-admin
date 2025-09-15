package org.ltl.enhancement.admin.controller.api

import scala.jdk.CollectionConverters._

// Media Response Types - Scala case classes for type safety

case class MediaResponse(
  id: Long,
  name: String,
  originalName: String,
  mimeType: String,
  size: Long,
  url: String,
  publicMedia: Boolean,
  `type`: String,
  createdAt: Long
)

case class AdminMediaDetailsResponse(
  id: Long,
  name: String,
  originalName: String,
  mimeType: String,
  size: Long,
  width: Int,
  height: Int,
  url: String,
  publicMedia: Boolean,
  `type`: String,
  alternativeText: String,
  title: String,
  caption: String,
  description: String,
  createdAt: Long,
  ownerAdmin: Option[AdminInfo] = None,
  ownerUser: Option[UserInfo] = None
)

case class AdminInfo(
  username: String,
  name: String
)

case class UserInfo(
  username: String,
  name: String
)

case class PageToken(
  next: Option[String] = None
)

case class ContinuationInfo(
  hasNext: Boolean = false
)

case class PaginationModel[T](
  items: List[T],
  count: Int,
  total: Int,
  pageToken: PageToken,
  continuation: ContinuationInfo
)

case class MediaListResponse(
  items: List[MediaResponse],
  count: Int,
  total: Int,
  pageToken: PageToken,
  continuation: ContinuationInfo
)

case class ErrorResponse(
  error: String
)

case class SuccessResponse(
  message: String
)

// Companion objects for Java compatibility
object AdminMediaDetailsResponse {
  def toJava(details: AdminMediaDetailsResponse): java.util.Map[String, Any] = {
    val baseMap = Map(
      "id" -> details.id,
      "name" -> details.name,
      "originalName" -> details.originalName,
      "mimeType" -> details.mimeType,
      "size" -> details.size,
      "width" -> details.width,
      "height" -> details.height,
      "url" -> details.url,
      "publicMedia" -> details.publicMedia,
      "type" -> details.`type`,
      "alternativeText" -> details.alternativeText,
      "title" -> details.title,
      "caption" -> details.caption,
      "description" -> details.description,
      "createdAt" -> details.createdAt
    )

    val withOwner = details.ownerAdmin match {
      case Some(admin) => baseMap + ("ownerAdmin" -> Map(
        "username" -> admin.username,
        "name" -> admin.name
      ).asJava)
      case None => baseMap
    }

    details.ownerUser match {
      case Some(user) => withOwner + ("ownerUser" -> Map(
        "username" -> user.username,
        "name" -> user.name
      ).asJava)
      case None => withOwner
    }
  }.asJava
}

object PaginationModel {
  def toJava[T](model: PaginationModel[T], itemConverter: T => java.util.Map[String, Any]): java.util.Map[String, Any] = Map(
    "items" -> model.items.map(itemConverter).asJava,
    "count" -> model.count,
    "total" -> model.total,
    "pageToken" -> Map(
      "next" -> model.pageToken.next.orNull
    ).asJava,
    "continuation" -> Map(
      "hasNext" -> model.continuation.hasNext
    ).asJava
  ).asJava
}

object ErrorResponse {
  def toJava(error: ErrorResponse): java.util.Map[String, Any] = Map(
    "error" -> error.error
  ).asJava
}

object SuccessResponse {
  def toJava(success: SuccessResponse): java.util.Map[String, Any] = Map(
    "message" -> success.message
  ).asJava
}

object MediaListResponse {
  def toJava(response: MediaListResponse): java.util.Map[String, Any] = Map(
    "items" -> response.items.map(item => Map(
      "id" -> item.id,
      "name" -> item.name,
      "originalName" -> item.originalName,
      "mimeType" -> item.mimeType,
      "size" -> item.size,
      "url" -> item.url,
      "publicMedia" -> item.publicMedia,
      "type" -> item.`type`,
      "createdAt" -> item.createdAt
    ).asJava).asJava,
    "count" -> response.count,
    "total" -> response.total,
    "pageToken" -> Map(
      "next" -> response.pageToken.next.orNull
    ).asJava,
    "continuation" -> Map(
      "hasNext" -> response.continuation.hasNext
    ).asJava
  ).asJava
}

// // Companion objects for Java compatibility
// object MediaResponse {
//   def toJava(media: MediaResponse): java.util.Map[String, Any] = Map(
//     "id" -> media.id,
//     "name" -> media.name,
//     "originalName" -> media.originalName,
//     "mimeType" -> media.mimeType,
//     "size" -> media.size,
//     "url" -> media.url,
//     "publicMedia" -> media.publicMedia,
//     "type" -> media.`type`,
//     "createdAt" -> media.createdAt
//   ).asJava
// }

// object AdminMediaDetailsResponse {
//   def toJava(details: AdminMediaDetailsResponse): java.util.Map[String, Any] = {
//     val baseMap = Map(
//       "id" -> details.id,
//       "name" -> details.name,
//       "originalName" -> details.originalName,
//       "mimeType" -> details.mimeType,
//       "size" -> details.size,
//       "width" -> details.width,
//       "height" -> details.height,
//       "url" -> details.url,
//       "publicMedia" -> details.publicMedia,
//       "type" -> details.`type`,
//       "alternativeText" -> details.alternativeText,
//       "title" -> details.title,
//       "caption" -> details.caption,
//       "description" -> details.description,
//       "createdAt" -> details.createdAt
//     )

//     val withOwner = details.ownerAdmin match {
//       case Some(admin) => baseMap + ("ownerAdmin" -> Map(
//         "username" -> admin.username,
//         "name" -> admin.name
//       ).asJava)
//       case None => baseMap
//     }

//     details.ownerUser match {
//       case Some(user) => withOwner + ("ownerUser" -> Map(
//         "username" -> user.username,
//         "name" -> user.name
//       ).asJava)
//       case None => withOwner
//     }
//   }.asJava
// }

// object PaginationModel {
//   def toJava[T](model: PaginationModel[T], itemConverter: T => java.util.Map[String, Any]): java.util.Map[String, Any] = Map(
//     "items" -> model.items.map(itemConverter).asJava,
//     "count" -> model.count,
//     "total" -> model.total,
//     "pageToken" -> Map(
//       "next" -> model.pageToken.next.orNull
//     ).asJava,
//     "continuation" -> Map(
//       "hasNext" -> model.continuation.hasNext
//     ).asJava
//   ).asJava
// }

// object ErrorResponse {
//   def toJava(error: ErrorResponse): java.util.Map[String, Any] = Map(
//     "error" -> error.error
//   ).asJava
// }

// object SuccessResponse {
//   def toJava(success: SuccessResponse): java.util.Map[String, Any] = Map(
//     "message" -> success.message
//   ).asJava
// }

// object MediaListResponse {
//   def toJava(response: MediaListResponse): java.util.Map[String, Any] = Map(
//     "items" -> response.items.map(MediaResponse.toJava).asJava,
//     "count" -> response.count,
//     "total" -> response.total,
//     "pageToken" -> Map(
//       "next" -> response.pageToken.next.orNull
//     ).asJava,
//     "continuation" -> Map(
//       "hasNext" -> response.continuation.hasNext
//     ).asJava
//   ).asJava
// }