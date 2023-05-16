package com.company.fifcorex_toma_de_pedidos.mediaresource

import com.sap.cloud.mobile.odata.EntitySet
import com.sap.cloud.mobile.odata.EntityValue

/*
 * Utility class to support the use of Glide to download media resources.
 */
object EntityMediaResource {
    /**
     * Determine if an entity set has media resource
     * @param entitySet
     * @return true if entity type is a Media Linked Entry (MLE) or it has stream properties
     */
    @JvmStatic
    fun hasMediaResources(entitySet: EntitySet) = entitySet.entityType.isMedia || entitySet.entityType.streamProperties.length() > 0
    /**
     * Return download Url for one of the media resource associated with the entity parameter.
     * @param entityValue
     * @param rootUrl
     * @return If the entity type associated with the entity parameter is a Media Linked Entry,
     * the MLE url will be returned. Otherwise, download url for one of the stream
     * properties will be returned.
     */
    @JvmStatic
    fun getMediaResourceUrl(entityValue: EntityValue, rootUrl: String): String? {
        if (entityValue.entityType.isMedia) {
            return mediaLinkedEntityUrl(entityValue, rootUrl)
        } else {
            if (entityValue.entityType.streamProperties.length() > 0) {
                return namedResourceUrl(entityValue, rootUrl)
            }
        }
        return null
    }

    /**
     * Get the media linked entity url
     * @param entityValue entity whose MLE url is to return
     * @param rootUrl OData Service base url
     * @return the media linked entity url or null if one cannot be constructed from the entity
     */
    private fun mediaLinkedEntityUrl(entityValue: EntityValue, rootUrl: String): String? {
        val mediaLink = entityValue.mediaStream.readLink
        return if (mediaLink != null) {
            rootUrl + mediaLink
        } else null
    }

    /**
     * Get the named resource url. If there are more than one named resources, only one will be returned
     * @param entityValue entity whose MLE url is to return
     * @param rootUrl
     * @return
     */
    private fun namedResourceUrl(entityValue: EntityValue, rootUrl: String): String? {
        val namedResourceProp = entityValue.entityType.streamProperties.first()
        val streamLink = namedResourceProp.getStreamLink(entityValue)
        var mediaLink: String? = streamLink.readLink
        if (mediaLink != null) {
            return rootUrl + mediaLink
        } else {
            // This is to get around the problem that after we writeToParcel and read it back, we lost the url for stream link
            // To be removed when bug is fixed
            if (entityValue.readLink != null) {
                mediaLink = entityValue.readLink + '/'.toString() + namedResourceProp.name
                return rootUrl + mediaLink
            }
        }
        return null
    }
}