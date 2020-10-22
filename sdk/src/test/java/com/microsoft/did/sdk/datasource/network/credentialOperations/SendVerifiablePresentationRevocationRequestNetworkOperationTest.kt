// Copyright (c) Microsoft Corporation. All rights reserved

package com.microsoft.did.sdk.datasource.network.credentialOperations

import com.microsoft.did.sdk.datasource.network.apis.ApiProvider
import com.microsoft.did.sdk.util.controlflow.SignatureException
import com.microsoft.did.sdk.util.defaultTestSerializer
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class SendVerifiablePresentationRevocationRequestNetworkOperationTest {
    private val apiProvider: ApiProvider = mockk()
    private val sendPresentationResponseNetworkOperation: SendVerifiablePresentationRevocationRequestNetworkOperation =
        SendVerifiablePresentationRevocationRequestNetworkOperation("", "", apiProvider, defaultTestSerializer)
    private val suppliedRevocationReceipt =
        "eyJraWQiOiJkaWQ6aW9uOkVpQ2ZlT2NpRWp3dXB3UlFzSkMzd01aenozX00zWElvNmJoeTdhSmtDRzZDQVE_LWlvbi1pbml0aWFsLXN0YXRlPWV5SmtaV3gwWVY5b1lYTm9Jam9pUldsRU1EUXdZMmxRYWtVeFIweHFMWEV5V21SeUxWSmFYelZsY1U4eU5GbERNRkk1YlRsRWQyWkhNa2RHUVNJc0luSmxZMjkyWlhKNVgyTnZiVzFwZEcxbGJuUWlPaUpGYVVNeVJtUTVVRTkwZW1GTmNVdE1hRE5SVEZwMFdrNDNWMFJEUkhKamRrTjRlVE52ZGxORVJEaEtSR1ZSSW4wLmV5SjFjR1JoZEdWZlkyOXRiV2wwYldWdWRDSTZJa1ZwUTJndGFURkRNVzFmTTJONFNHSk5NM3BYZW1SUmRFeHhNbkJ2UmxkYVgyNUZWRUpUYjBOaFQySlpUV2NpTENKd1lYUmphR1Z6SWpwYmV5SmhZM1JwYjI0aU9pSnlaWEJzWVdObElpd2laRzlqZFcxbGJuUWlPbnNpY0hWaWJHbGpYMnRsZVhNaU9sdDdJbWxrSWpvaWMybG5YekJtT1RkbFpXWmpJaXdpZEhsd1pTSTZJa1ZqWkhOaFUyVmpjREkxTm1zeFZtVnlhV1pwWTJGMGFXOXVTMlY1TWpBeE9TSXNJbXAzYXlJNmV5SnJkSGtpT2lKRlF5SXNJbU55ZGlJNkluTmxZM0F5TlRack1TSXNJbmdpT2lKb1EweHNiM0pKYkd4Mk0yRldTa1JpWWtOeE0wVkhielUyYldWNlEzUkxXa1pHY1V0dlMzUlZjM0J6SWl3aWVTSTZJbWgxVkc1aVRFYzNNV1UwTkRORWVWSmtlVTVEWDNkZmMzcGFSMGhWWVVjeFVIZHNNSHBYYjBoMkxVRWlmU3dpY0hWeWNHOXpaU0k2V3lKaGRYUm9JaXdpWjJWdVpYSmhiQ0pkZlYxOWZWMTkjc2lnXzBmOTdlZWZjIiwidHlwIjoiSldUIiwiYWxnIjoiRVMyNTZLIn0.eyJqdGkiOiIxYTlkOTI1ZjliMjM0NzY2YjI0N2JlNjg5NTBkMWU0NCIsImlzcyI6ImRpZDppb246RWlBaEl4bW5KRk5IZTRnX1ZJQk01a01QVlZBdmc4VmpFeHpVSWYzejFzbzllZz8taW9uLWluaXRpYWwtc3RhdGU9ZXlKa1pXeDBZVjlvWVhOb0lqb2lSV2xCYkc1cWJXdHJiV3ROY0RaU1pqSmlZakZsYmxKUGJtWnZMV3g1V0dGRGVsWjBabmhRTlMxNWJWQktVU0lzSW5KbFkyOTJaWEo1WDJOdmJXMXBkRzFsYm5RaU9pSkZhVUo1YzNsME1qa3phR2xLVUdWcFgyb3pTRVp1VEZSbVpTMXdZVWRHT1c4MWVXNDFZMFpWUjJGa2IwRkJJbjAuZXlKMWNHUmhkR1ZmWTI5dGJXbDBiV1Z1ZENJNklrVnBRbmx6ZVhReU9UTm9hVXBRWldsZmFqTklSbTVNVkdabExYQmhSMFk1YnpWNWJqVmpSbFZIWVdSdlFVRWlMQ0p3WVhSamFHVnpJanBiZXlKaFkzUnBiMjRpT2lKeVpYQnNZV05sSWl3aVpHOWpkVzFsYm5RaU9uc2ljSFZpYkdsalgydGxlWE1pT2x0N0ltbGtJam9pVkY5clgzTnBaMjVmYVV3MFEzRkZSa2xmTVNJc0luUjVjR1VpT2lKRlkyUnpZVk5sWTNBeU5UWnJNVlpsY21sbWFXTmhkR2x2Ymt0bGVUSXdNVGtpTENKcWQyc2lPbnNpYTNSNUlqb2lSVU1pTENKamNuWWlPaUp6WldOd01qVTJhekVpTENKNElqb2lTMWxsUVc4NFoyVlJhRkpVU1hOWE9FUlZabTEzUkROSFJuSjJjMDVLVTNsVVpEaHhhbDlMUzA5ZmR5SXNJbmtpT2lKeGRsbHhja1pwUkhNeVUyZFhWVm81Y0VWRmVYVjJaVFIwWTNReVkxcE5PV1ZYUWxKc00wcEJVREIzSW4wc0luQjFjbkJ2YzJVaU9sc2lZWFYwYUNJc0ltZGxibVZ5WVd3aVhYMWRmWDFkZlEiLCJjcmVkZW50aWFsU3RhdHVzIjp7ImlkIjoidXJuOnBpYzowZjAzMjM5OS1lMTcxLTRmZjEtYWI4Mi04N2U0MjM5ZGMzZTUiLCJyZWFzb24iOiJ0ZXN0aW5nIHJldm9jYXRpb24iLCJzdGF0dXMiOiJyZXZva2VkIn0sInJwIjpbXSwiaWF0IjoxNTk0NzEzMzcwfQ.MEYCIQDF6WTCaqWXA_EIUBDUJH4BfZhGc3bfgtQju99smp4-HgIhAIpTVRAa8zUyWgHsKjbkUhGpLY5MiBn72AbQqRH5Gcwl"
    private val expectedIssuer =
        "did:ion:EiAhIxmnJFNHe4g_VIBM5kMPVVAvg8VjExzUIf3z1so9eg?-ion-initial-state=eyJkZWx0YV9oYXNoIjoiRWlBbG5qbWtrbWtNcDZSZjJiYjFlblJPbmZvLWx5WGFDelZ0ZnhQNS15bVBKUSIsInJlY292ZXJ5X2NvbW1pdG1lbnQiOiJFaUJ5c3l0MjkzaGlKUGVpX2ozSEZuTFRmZS1wYUdGOW81eW41Y0ZVR2Fkb0FBIn0.eyJ1cGRhdGVfY29tbWl0bWVudCI6IkVpQnlzeXQyOTNoaUpQZWlfajNIRm5MVGZlLXBhR0Y5bzV5bjVjRlVHYWRvQUEiLCJwYXRjaGVzIjpbeyJhY3Rpb24iOiJyZXBsYWNlIiwiZG9jdW1lbnQiOnsicHVibGljX2tleXMiOlt7ImlkIjoiVF9rX3NpZ25faUw0Q3FFRklfMSIsInR5cGUiOiJFY2RzYVNlY3AyNTZrMVZlcmlmaWNhdGlvbktleTIwMTkiLCJqd2siOnsia3R5IjoiRUMiLCJjcnYiOiJzZWNwMjU2azEiLCJ4IjoiS1llQW84Z2VRaFJUSXNXOERVZm13RDNHRnJ2c05KU3lUZDhxal9LS09fdyIsInkiOiJxdllxckZpRHMyU2dXVVo5cEVFeXV2ZTR0Y3QyY1pNOWVXQlJsM0pBUDB3In0sInB1cnBvc2UiOlsiYXV0aCIsImdlbmVyYWwiXX1dfX1dfQ"
    private val expectedJti = "1a9d925f9b234766b247be68950d1e44"
    private val expectedIssuedAtTime = 1594713370L
    private val expectedCredentialStatus = "revoked"
    private val expectedRevocationReason = "testing revocation"
    private val expectedRevokedVCId = "urn:pic:0f032399-e171-4ff1-ab82-87e4239dc3e5"
    private val expectedRevokedRps = emptyArray<String>()
    private val invalidRevocationReceipt =
        "eyJraWQiOiJkaWQ6aW9uOkVpQ2ZlT2NpRWp3dXB3UlFzSkMzd01aenozX00zWElvNmJoeTdhSmtDRzZDQVE_LWlvbi1pbml0aWFsLXN0YXRlPWV5SmtaV3gwWVY5b1lYTm9Jam9pUldsRU1EUXdZMmxRYWtVeFIweHFMWEV5V21SeUxWSmFYelZsY1U4eU5GbERNRkk1YlRsRWQyWkhNa2RHUVNJc0luSmxZMjkyWlhKNVgyTnZiVzFwZEcxbGJuUWlPaUpGYVVNeVJtUTVVRTkwZW1GTmNVdE1hRE5SVEZwMFdrNDNWMFJEUkhKamRrTjRlVE52ZGxORVJEaEtSR1ZSSW4wLmV5SjFjR1JoZEdWZlkyOXRiV2wwYldWdWRDSTZJa1ZwUTJndGFURkRNVzFmTTJONFNHSk5NM3BYZW1SUmRFeHhNbkJ2UmxkYVgyNUZWRUpUYjBOaFQySlpUV2NpTENKd1lYUmphR1Z6SWpwYmV5SmhZM1JwYjI0aU9pSnlaWEJzWVdObElpd2laRzlqZFcxbGJuUWlPbnNpY0hWaWJHbGpYMnRsZVhNaU9sdDdJbWxrSWpvaWMybG5YekJtT1RkbFpXWmpJaXdpZEhsd1pTSTZJa1ZqWkhOaFUyVmpjREkxTm1zeFZtVnlhV1pwWTJGMGFXOXVTMlY1TWpBeE9TSXNJbXAzYXlJNmV5SnJkSGtpT2lKRlF5SXNJbU55ZGlJNkluTmxZM0F5TlRack1TSXNJbmdpT2lKb1EweHNiM0pKYkd4Mk0yRldTa1JpWWtOeE0wVkhielUyYldWNlEzUkxXa1pHY1V0dlMzUlZjM0J6SWl3aWVTSTZJbWgxVkc1aVRFYzNNV1UwTkRORWVWSmtlVTVEWDNkZmMzcGFSMGhWWVVjeFVIZHNNSHBYYjBoMkxVRWlmU3dpY0hWeWNHOXpaU0k2V3lKaGRYUm9JaXdpWjJWdVpYSmhiQ0pkZlYxOWZWMTkjc2lnXzBmOTdlZWZjIiwidHlwIjoiSldUIiwiYWxnIjoiRVMyNTZLIn0.eyJqdGkiOiIxYTlkOTI1ZjliMjM0NzY2YjI0N2JlNjg5NTBkMWU0NCIsImlzcyI6ImRpZDppb246RWlBaEl4bW5KRk5IZTRnX1ZJQk01a01QVlZBdmc4VmpFeHpVSWYzejFzbzllZz8taW9uLWluaXRpYWwtc3RhdGU9ZXlKa1pXeDBZVjlvWVhOb0lqb2lSV2xCYkc1cWJXdHJiV3ROY0RaU1pqSmlZakZsYmxKUGJtWnZMV3g1V0dGRGVsWjBabmhRTlMxNWJWQktVU0lzSW5KbFkyOTJaWEo1WDJOdmJXMXBkRzFsYm5RaU9pSkZhVUo1YzNsME1qa3phR2xLVUdWcFgyb3pTRVp1VEZSbVpTMXdZVWRHT1c4MWVXNDFZMFpWUjJGa2IwRkJJbjAuZXlKMWNHUmhkR1ZmWTI5dGJXbDBiV1Z1ZENJNklrVnBRbmx6ZVhReU9UTm9hVXBRWldsZmFqTklSbTVNVkdabExYQmhSMFk1YnpWNWJqVmpSbFZIWVdSdlFVRWlMQ0p3WVhSamFHVnpJanBiZXlKaFkzUnBiMjRpT2lKeVpYQnNZV05sSWl3aVpHOWpkVzFsYm5RaU9uc2ljSFZpYkdsalgydGxlWE1pT2x0N0ltbGtJam9pVkY5clgzTnBaMjVmYVV3MFEzRkZSa2xmTVNJc0luUjVjR1VpT2lKRlkyUnpZVk5sWTNBeU5UWnJNVlpsY21sbWFXTmhkR2x2Ymt0bGVUSXdNVGtpTENKcWQyc2lPbnNpYTNSNUlqb2lSVU1pTENKamNuWWlPaUp6WldOd01qVTJhekVpTENKNElqb2lTMWxsUVc4NFoyVlJhRkpVU1hOWE9FUlZabTEzUkROSFJuSjJjMDVLVTNsVVpEaHhhbDlMUzA5ZmR5SXNJbmtpT2lKeGRsbHhja1pwUkhNeVUyZFhWVm81Y0VWRmVYVjJaVFIwWTNReVkxcE5PV1ZYUWxKc00wcEJVREIzSW4wc0luQjFjbkJ2YzJVaU9sc2lZWFYwYUNJc0ltZGxibVZ5WVd3aVhYMWRmWDFkZlEiLCJjcmVkZW50aWFsU3RhdHVzIjp7ImlkIjoidXJuOnBpYzowZjAzMjM5OS1lMTcxLTRmZjEtYWI4Mi04N2U0MjM5ZGMzZTUiLCJyZWFzb24iOiJ0ZXN0aW5nIHJldm9jYXRpb24iLCJzdGF0dXMiOiJyZXZva2VkIn0sInJwIjpbXSwiaWF0IjoxNTk0NzEzMzcwfQ"

    @Test
    fun `test unwrapping revocation receipt successfully`() {
        val revocationReceipt =
            sendPresentationResponseNetworkOperation.unwrapRevocationReceipt(suppliedRevocationReceipt, defaultTestSerializer)
        assertThat(revocationReceipt.issuer).isEqualTo(expectedIssuer)
        assertThat(revocationReceipt.jwtId).isEqualTo(expectedJti)
        assertThat(revocationReceipt.issuedTime).isEqualTo(expectedIssuedAtTime)
        assertThat(revocationReceipt.credentialStatus.status).isEqualTo(expectedCredentialStatus)
        assertThat(revocationReceipt.credentialStatus.reason).isEqualTo(expectedRevocationReason)
        assertThat(revocationReceipt.credentialStatus.id).isEqualTo(expectedRevokedVCId)
        assertThat(revocationReceipt.relyingPartyList).isEqualTo(expectedRevokedRps)
    }

    @Test
    fun `test unwrapping revocation receipt with no signature in jwt`() {
        assertThatThrownBy {
            sendPresentationResponseNetworkOperation.unwrapRevocationReceipt(
                invalidRevocationReceipt,
                defaultTestSerializer
            )
        }.isInstanceOf(
            SignatureException::class.java
        )
    }

}