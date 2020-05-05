/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/
package com.microsoft.portableIdentity.sdk.identifier

import com.microsoft.portableIdentity.sdk.crypto.CryptoOperations
import com.microsoft.portableIdentity.sdk.crypto.keys.KeyType
import com.microsoft.portableIdentity.sdk.crypto.keys.PublicKey
import com.microsoft.portableIdentity.sdk.identifier.models.payload.RegistrationPayload
import com.microsoft.portableIdentity.sdk.utilities.Base64Url
import com.microsoft.portableIdentity.sdk.utilities.Constants
import com.microsoft.portableIdentity.sdk.utilities.Constants.RECOVERY_KEYREFERENCE
import com.microsoft.portableIdentity.sdk.utilities.Constants.SIGNATURE_KEYREFERENCE
import com.microsoft.portableIdentity.sdk.utilities.SdkLog
import com.microsoft.portableIdentity.sdk.utilities.Serializer
import com.microsoft.portableIdentity.sdk.utilities.controlflow.IdentifierCreatorException
import com.microsoft.portableIdentity.sdk.utilities.controlflow.RegistrarException
import com.microsoft.portableIdentity.sdk.utilities.controlflow.Result
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Creates Identifiers
 * @class
 */
@Singleton
class IdentifierCreator @Inject constructor(private val cryptoOperations: CryptoOperations,
                                            private val payloadProcessor: SidetreePayloadProcessor) {

    fun create(peerId: String): Result<Identifier> {
        //TODO(use different variable than peerId for end)
        val signatureKeyReference = "${SIGNATURE_KEYREFERENCE}_$peerId"
        val recoveryKeyReference = "${RECOVERY_KEYREFERENCE}_$peerId"
        return try {
            val alias = Base64Url.encode(Random.nextBytes(2))
            val signingPublicKey = cryptoOperations.generateKeyPair("${alias}_$signatureKeyReference", KeyType.EllipticCurve)
            val recoveryPublicKey = cryptoOperations.generateKeyPair("${alias}_$recoveryKeyReference", KeyType.EllipticCurve)
            val registrationPayload = payloadProcessor.generateCreatePayload(signingPublicKey, recoveryPublicKey)
            val identifierLongForm = computeLongFormIdentifier(payloadProcessor, registrationPayload)

            Result.Success(
                transformIdentifierDocumentToIdentifier(
                    payloadProcessor,
                    registrationPayload,
                    identifierLongForm,
                    alias,
                    signatureKeyReference,
                    recoveryKeyReference
                )
            )
        } catch (exception: Exception) {
            Result.Failure(IdentifierCreatorException("Unable to create an identifier", exception))
        }
    }

    private fun computeUniqueSuffix(payloadProcessor: SidetreePayloadProcessor, registrationPayload: RegistrationPayload): String {
        val uniqueSuffix = payloadProcessor.computeUniqueSuffix(registrationPayload.suffixData)
        return "did:${Constants.METHOD_NAME}:$uniqueSuffix"
    }

    private fun computeLongFormIdentifier(
        payloadProcessor: SidetreePayloadProcessor,
        registrationPayload: RegistrationPayload
    ): String {
        val registrationPayloadEncoded = registrationPayload.suffixData+"."+registrationPayload.patchData
        val identifierShortForm = computeUniqueSuffix(payloadProcessor, registrationPayload)
        return "$identifierShortForm?${Constants.INITIAL_STATE_LONGFORM}=$registrationPayloadEncoded"
    }

    private fun transformIdentifierDocumentToIdentifier(
        payloadProcessor: SidetreePayloadProcessor,
        registrationPayload: RegistrationPayload,
        identifierLongForm: String,
        alias: String,
        signatureKeyReference: String,
        recoveryKeyReference: String
    ): Identifier {
        val nextUpdateCommitmentHash = payloadProcessor.extractNextUpdateCommitmentHash(registrationPayload)
        val nextRecoveryCommitmentHash = payloadProcessor.extractNextRecoveryCommitmentHash(registrationPayload)

        val personaSigKeyRef = "${alias}_$signatureKeyReference"
        val personaRecKeyRef = "${alias}_$recoveryKeyReference"

        return Identifier(
            identifierLongForm,
            alias,
            personaSigKeyRef,
            "",
            personaRecKeyRef,
            nextUpdateCommitmentHash,
            nextRecoveryCommitmentHash,
            Constants.IDENTIFIER_SECRET_KEY_NAME
        )
    }
}