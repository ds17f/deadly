package com.grateful.deadly.services.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub release models for fetching the latest dead-metadata release.
 */
@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String,
    val name: String,
    @SerialName("published_at")
    val publishedAt: String,
    val assets: List<GitHubAsset>
)

@Serializable
data class GitHubAsset(
    val name: String,
    val size: Long,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
    @SerialName("content_type")
    val contentType: String
)