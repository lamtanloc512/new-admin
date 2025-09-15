package org.ltl.enhancement.admin.controller.api.pagination

import org.youngmonkeys.ezyplatform.entity.MediaType
import org.youngmonkeys.ezyplatform.pagination.MediaFilter
import com.tvd12.ezydata.database.query.EzyQueryConditionBuilder

/** Scala version of DefaultMediaFilter with Builder pattern Converted from Java
  * implementation with Scala idiomatic patterns
  */
final case class DefaultEnhancementMediaFilter(
    `type`: Option[MediaType] = None,
    ownerAdminId: Option[Long] = None,
    ownerUserId: Option[Long] = None,
    prefixKeyword: Option[String] = None,
    likeKeyword: Option[String] = None
) extends MediaFilter {

  override def matchingCondition(): String = {
    val answer = new EzyQueryConditionBuilder()
    `type`.foreach(_ => answer.and("e.type = :type"))
    ownerAdminId.foreach(_ => answer.and("e.ownerAdminId = :ownerAdminId"))
    ownerUserId.foreach(_ => answer.and("e.ownerUserId = :ownerUserId"))
    prefixKeyword.foreach(_ =>
      answer.and("e.originalName LIKE CONCAT(:prefixKeyword,'%')")
    )
    likeKeyword.foreach(_ =>
      answer.and("e.originalName LIKE CONCAT('%',:likeKeyword,'%')")
    )
    answer.build()
  }
}

/** Builder pattern implementation for DefaultEnhancementMediaFilter Provides
  * fluent API for constructing filter instances
  */
class DefaultEnhancementMediaFilterBuilder {
  private var mediaType: Option[MediaType] = None
  private var ownerAdminId: Option[Long] = None
  private var ownerUserId: Option[Long] = None
  private var prefixKeyword: Option[String] = None
  private var likeKeyword: Option[String] = None

  def withMediaType(
      mediaType: MediaType
  ): DefaultEnhancementMediaFilterBuilder = {
    this.mediaType = Some(mediaType)
    this
  }

  def withOwnerAdminId(
      ownerAdminId: Long
  ): DefaultEnhancementMediaFilterBuilder = {
    this.ownerAdminId = Some(ownerAdminId)
    this
  }

  def withOwnerUserId(
      ownerUserId: Long
  ): DefaultEnhancementMediaFilterBuilder = {
    this.ownerUserId = Some(ownerUserId)
    this
  }

  def withPrefixKeyword(
      prefixKeyword: String
  ): DefaultEnhancementMediaFilterBuilder = {
    this.prefixKeyword = Some(prefixKeyword)
    this
  }

  def withLikeKeyword(
      likeKeyword: String
  ): DefaultEnhancementMediaFilterBuilder = {
    this.likeKeyword = Some(likeKeyword)
    this
  }

  def build(): DefaultEnhancementMediaFilter = {
    DefaultEnhancementMediaFilter(
      `type` = mediaType,
      ownerAdminId = ownerAdminId,
      ownerUserId = ownerUserId,
      prefixKeyword = prefixKeyword,
      likeKeyword = likeKeyword
    )
  }
}

/** Companion object with factory methods
  */
object DefaultEnhancementMediaFilter {
  def builder(): DefaultEnhancementMediaFilterBuilder = {
    new DefaultEnhancementMediaFilterBuilder()
  }

  // Example usage methods
  def createImageFilter(): DefaultEnhancementMediaFilter = {
    builder()
      .withMediaType(MediaType.IMAGE)
      .build()
  }

  def createImageFilterWithKeyword(
      keyword: String
  ): DefaultEnhancementMediaFilter = {
    builder()
      .withMediaType(MediaType.IMAGE)
      .withPrefixKeyword(keyword)
      .build()
  }

  def createFilterWithOwner(
      mediaType: MediaType,
      ownerId: Long
  ): DefaultEnhancementMediaFilter = {
    builder()
      .withMediaType(mediaType)
      .withOwnerUserId(ownerId)
      .build()
  }
}
