/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package com.microsoft.portableIdentity.sdk

import com.microsoft.portableIdentity.sdk.crypto.CryptoOperations
import com.microsoft.portableIdentity.sdk.identifier.Identifier
import com.microsoft.portableIdentity.sdk.identifier.PayloadGenerator
import com.microsoft.portableIdentity.sdk.identifier.models.PatchData
import com.microsoft.portableIdentity.sdk.identifier.models.SuffixData
import com.microsoft.portableIdentity.sdk.registrars.Registrar
import com.microsoft.portableIdentity.sdk.registrars.RegistrationDocument
import com.microsoft.portableIdentity.sdk.repository.PortableIdentityRepository
import com.microsoft.portableIdentity.sdk.resolvers.Resolver
import com.microsoft.portableIdentity.sdk.utilities.Base64Url
import com.microsoft.portableIdentity.sdk.utilities.Constants.INITIAL_STATE_LONGFORM
import com.microsoft.portableIdentity.sdk.utilities.Constants.METHOD_NAME
import com.microsoft.portableIdentity.sdk.utilities.Constants.IDENTITY_SECRET_KEY_NAME
import com.microsoft.portableIdentity.sdk.utilities.SdkLog
import com.microsoft.portableIdentity.sdk.utilities.Serializer
import com.microsoft.portableIdentity.sdk.utilities.byteArrayToString
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Class for creating identifiers and
 * sending and parsing OIDC Requests and Responses.
 * @class
 */
@Singleton
class IdentityManager @Inject constructor(
    val identityRepository: PortableIdentityRepository,
    private val cryptoOperations: CryptoOperations,
    private val resolver: Resolver,
    private val registrar: Registrar,
    @Named("signatureKeyReference") private val signatureKeyReference: String,
    @Named("encryptionKeyReference") private val encryptionKeyReference: String,
    @Named("recoveryKeyReference") private val recoveryKeyReference: String,
    @Named("resolverUrl") resolverUrl: String
) {

    private val url = resolverUrl

    val did: Identifier by lazy { initLongFormDid() }
//    val did = initLongFormDid()

    private suspend fun registerPortableIdentity(url: String): Identifier {
        val alias = Base64Url.encode(Random.nextBytes(16))
        val personaEncKeyRef = "$alias.$encryptionKeyReference"
        val personaSigKeyRef = "$alias.$signatureKeyReference"
        val personaRecKeyRef = "$alias.$recoveryKeyReference"
        val payloadGenerator = PayloadGenerator(
            cryptoOperations,
            signatureKeyReference,
            encryptionKeyReference,
            recoveryKeyReference
        )
        val registrationDocumentEncoded = payloadGenerator.generateCreatePayload(alias)
        val registrationDocument =
            Serializer.parse(RegistrationDocument.serializer(), byteArrayToString(Base64Url.decode(registrationDocumentEncoded)))

        val uniqueSuffix = payloadGenerator.computeUniqueSuffix(registrationDocument.suffixData)
        val portableIdentity = "did:$METHOD_NAME:test:$uniqueSuffix"

        val identifier = "$portableIdentity?$INITIAL_STATE_LONGFORM=$registrationDocumentEncoded"
        val identifierDocument = identityRepository.resolveIdentifier(url, identifier)

        val patchDataJson = byteArrayToString(Base64Url.decode(registrationDocument.patchData))
        val nextUpdateCommitmentHash = Serializer.parse(PatchData.serializer(), patchDataJson).nextUpdateCommitmentHash
        val suffixDataJson = byteArrayToString(Base64Url.decode(registrationDocument.suffixData))
        val nextRecoveryCommitmentHash = Serializer.parse(SuffixData.serializer(), suffixDataJson).nextRecoveryCommitmentHash

        val longformIdentifier =
            Identifier(
                identifier,
                alias,
                personaSigKeyRef,
                personaEncKeyRef,
                personaEncKeyRef,
                nextUpdateCommitmentHash,
                nextRecoveryCommitmentHash,
                identifierDocument!!,
                IDENTITY_SECRET_KEY_NAME
            )
        identityRepository.insert(longformIdentifier)
        return identityRepository.queryById(identifier)
    }

    private fun initLongFormDid(): Identifier {
        val did = if (identityRepository.queryByName(IDENTITY_SECRET_KEY_NAME) != null) {
            SdkLog.d("Identifier found, de-serializing")
            identityRepository.queryByName(IDENTITY_SECRET_KEY_NAME)
        } else {
            SdkLog.d("No identifier found, registering new DID")
            val identifier = registerPortableIdentity()
            identifier
        }
        SdkLog.d("Using identifier ${did.document.id}")
        return did
    }

    private fun registerPortableIdentity(): Identifier {
        var did: Identifier? = null
        // TODO: Verify runBlocking is proper here
        runBlocking {
            did = createPortableIdentity()
        }
        SdkLog.d("Created ${did!!.document.id}")
        return did!!
    }

    fun createPortableIdentity(callback: (Identifier) -> Unit) {
        GlobalScope.launch {
            callback.invoke(createPortableIdentity())
        }
    }

    /**
     * Creates an Identifier.
     */
    private suspend fun createPortableIdentity(): Identifier {
        return withContext(Dispatchers.Default) {
            registerPortableIdentity(url)
        }
    }
}