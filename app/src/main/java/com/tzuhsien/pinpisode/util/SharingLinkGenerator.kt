package com.tzuhsien.pinpisode.util

import android.net.Uri
import androidx.core.net.toUri
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.ktx.androidParameters

const val DYNAMIC_LINK_PREFIX = "https://pinpisode.page.link"
const val DEEPLINK_PATH_YOUTUBE_NOTE = "/youtube_note/"
const val DEEPLINK_PATH_SPOTIFY_NOTE = "/spotify_note/"
const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.tzuhsien.pinpisode"

object SharingLinkGenerator {

    fun generateSharingLink(
        deepLink: Uri,
        previewImageLink: Uri?,
        getShareableLink: (String) -> Unit = {},
    ) {
        FirebaseDynamicLinks.getInstance().createDynamicLink().run {
            // What is this link parameter? You will get to know when we will actually use this function.
            link = deepLink

            // [domainUriPrefix] will be the domain name you added when setting up Dynamic Links at Firebase Console.
            // You can find it in the Dynamic Links dashboard.
            domainUriPrefix = DYNAMIC_LINK_PREFIX

            // Pass your preview Image Link here;
            previewImageLink?.let {
                setSocialMetaTagParameters(
                    DynamicLink.SocialMetaTagParameters.Builder()
                        .setImageUrl(previewImageLink)
                        .build()
                )
            }

            // Required
            androidParameters {
                fallbackUrl = PLAY_STORE_URL.toUri()
                build()
            }

            buildShortDynamicLink()
        }.addOnSuccessListener { dynamicLink ->
            // Retrieve the newly created dynamic link to use it further for sharing via Intent.
            getShareableLink.invoke(dynamicLink.shortLink.toString())
        }.addOnFailureListener {
            // generate a long link if unable to generate a short link
            generateLongSharingLink(deepLink, previewImageLink, getShareableLink)
        }
    }
}

fun generateLongSharingLink(
    deepLink: Uri,
    previewImageLink: Uri?,
    getShareableLink: (String) -> Unit = {},
) {
    val dynamicLink = FirebaseDynamicLinks.getInstance().createDynamicLink().run {
        // What is this link parameter? You will get to know when we will actually use this function.
        link = deepLink

        // [domainUriPrefix] will be the domain name you added when setting up Dynamic Links at Firebase Console.
        // You can find it in the Dynamic Links dashboard.
        domainUriPrefix = DYNAMIC_LINK_PREFIX

        // Pass your preview Image Link here;
        previewImageLink?.let {
            setSocialMetaTagParameters(
                DynamicLink.SocialMetaTagParameters.Builder()
                    .setImageUrl(previewImageLink)
                    .build()
            )
        }

        // Required
        androidParameters {
            fallbackUrl = PLAY_STORE_URL.toUri()
            build()
        }
        buildDynamicLink()
    }

    // Retrieve the newly created dynamic link to use it further for sharing via Intent.
    getShareableLink.invoke(dynamicLink.uri.toString())

}