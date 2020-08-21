/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package com.microsoft.did.sdk.credential.service.models.oidc

import com.microsoft.did.sdk.crypto.models.webCryptoApi.JsonWebKey
import com.microsoft.did.sdk.util.Constants
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Contents of an OpenID Self-Issued Token Response.
 *
 * @see [OpenID Spec](https://openid.net/specs/openid-connect-core-1_0.html#JWTRequests)
 */
@Serializable
data class OidcResponseContentForIssuance(

    val sub: String = "",

    val aud: String = "",

    val did: String = "",

    @SerialName("sub_jwk")
    val publicKeyJwk: JsonWebKey = JsonWebKey(),

    @SerialName("iat")
    val responseCreationTime: Long = 0,

    @SerialName("exp")
    val expirationTime: Long = 0,

    @SerialName("jti")
    val responseId: String = "",

    @Required
    @SerialName("iss")
    val issuer: String = Constants.SELF_ISSUED
) {
    var contract: String = ""

    var attestations: AttestationClaimModel = AttestationClaimModel()
}