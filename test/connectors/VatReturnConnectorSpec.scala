/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock._
import models.corrections.CorrectionPayload
import models.domain.VatReturn
import models.etmp.{EtmpObligations, EtmpVatReturn}
import models.external.ExternalEntryUrl
import models.requests.corrections.CorrectionRequest
import models.requests.{VatReturnRequest, VatReturnWithCorrectionRequest}
import models.responses._
import models.{PaymentReference, ReturnReference}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.EitherValues
import play.api.Application
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import java.util.UUID

class VatReturnConnectorSpec extends SpecBase with WireMockHelper with EitherValues {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()
  val url: String = "/one-stop-shop-returns/vat-returns"

  private def application: Application =
    applicationBuilder()
      .configure("microservice.services.one-stop-shop-returns.port" -> server.port)
      .build()

  private val coreErrorResponse = CoreErrorResponse(
    Instant.now(),
    Some(UUID.randomUUID()),
    "error",
    "Error message"
  )
  private val etmpObligations: EtmpObligations = arbitraryObligations.arbitrary.sample.value

  ".submit" - {

    "must return Right(VatReturn) when the server responds with CREATED" in {

      running(application) {
        val vatReturnRequest = VatReturnRequest(vrn, period, None, None, List.empty, List.empty)
        val ref = ReturnReference(vrn, period)
        val payRef = PaymentReference(vrn, period)
        val expectedVatReturn =
          VatReturn(
            vrn, period, ref, payRef, None,
            None, List.empty, List.empty,
            Instant.now(stubClockAtArbitraryDate), Instant.now(stubClockAtArbitraryDate)
          )
        val responseJson = Json.toJson(expectedVatReturn)
        val connector = application.injector.instanceOf[VatReturnConnector]

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(aResponse().withStatus(CREATED).withBody(responseJson.toString()))
        )

        val result = connector.submit(vatReturnRequest).futureValue

        result.value mustEqual expectedVatReturn
      }
    }

    "must return Left(InvalidJson) when the server responds with an incorrectly formatted JSON payload" in {

      running(application) {

        val responseJson = """{ "foo": "bar" }"""

        val connector = application.injector.instanceOf[VatReturnConnector]

        server.stubFor(post(urlEqualTo(url)).willReturn(aResponse().withStatus(OK).withBody(responseJson)))

        val result = connector.submit(vatReturnRequest).futureValue

        result.left.value mustEqual InvalidJson
      }
    }

    "must return Left(ConflictFound) when the server response with CONFLICT" in {

      running(application) {
        val vatReturnRequest = VatReturnRequest(vrn, period, None, None, List.empty, List.empty)

        val connector = application.injector.instanceOf[VatReturnConnector]

        server.stubFor(post(urlEqualTo(url)).willReturn(aResponse().withStatus(CONFLICT)))

        val result = connector.submit(vatReturnRequest).futureValue

        result.left.value mustEqual ConflictFound
      }
    }

    "must return Left(ReceivedErrorFromCore) when the server responds with a core error" in {

      running(application) {
        val vatReturnRequest = VatReturnRequest(vrn, period, None, None, List.empty, List.empty)

        val connector = application.injector.instanceOf[VatReturnConnector]

        server.stubFor(post(urlEqualTo(url)).willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE)
          .withBody(Json.toJson(coreErrorResponse).toString())))

        val result = connector.submit(vatReturnRequest).futureValue

        result.left.value mustEqual ReceivedErrorFromCore
      }
    }

    "must return Left(UnexpectedResponseStatus) when the server response with an error code" in {

      running(application) {
        val vatReturnRequest = VatReturnRequest(vrn, period, None, None, List.empty, List.empty)

        val connector = application.injector.instanceOf[VatReturnConnector]

        server.stubFor(post(urlEqualTo(url)).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR)))

        val result = connector.submit(vatReturnRequest).futureValue

        result.left.value mustBe an[UnexpectedResponseStatus]
      }
    }
  }

  ".submitWithCorrection" - {

    val url = "/one-stop-shop-returns/vat-return-with-corrections"

    "must return Right(VatReturn, Correction) when the server responds with CREATED" in {

      running(application) {
        val vatReturnRequest = VatReturnRequest(vrn, period, None, None, List.empty, List.empty)
        val correctionRequest = CorrectionRequest(vrn, period, List.empty)
        val vatReturnWithCorrectionRequest = VatReturnWithCorrectionRequest(vatReturnRequest, correctionRequest)
        val ref = ReturnReference(vrn, period)
        val payRef = PaymentReference(vrn, period)
        val expectedVatReturn = VatReturn(
          vrn, period, ref, payRef, None,
          None, List.empty, List.empty,
          Instant.now(stubClockAtArbitraryDate), Instant.now(stubClockAtArbitraryDate)
        )
        val expectedCorrection = CorrectionPayload(
          vrn,
          period,
          List.empty,
          Instant.now(stubClockAtArbitraryDate),
          Instant.now(stubClockAtArbitraryDate)
        )
        val expectedVatReturnWithCorrection = (expectedVatReturn, expectedCorrection)
        val responseJson = Json.toJson(expectedVatReturnWithCorrection)
        val connector = application.injector.instanceOf[VatReturnConnector]

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(aResponse().withStatus(CREATED).withBody(responseJson.toString()))
        )

        val result = connector.submitWithCorrections(vatReturnWithCorrectionRequest).futureValue

        result.value mustEqual expectedVatReturnWithCorrection
      }
    }

    "must return Left(InvalidJson) when the server responds with an incorrectly formatted JSON payload" in {

      running(application) {

        val vatReturnWithCorrectionRequest = VatReturnWithCorrectionRequest(vatReturnRequest, correctionRequest)

        val responseJson = """{ "foo": "bar" }"""

        val connector = application.injector.instanceOf[VatReturnConnector]

        server.stubFor(post(urlEqualTo(url)).willReturn(aResponse().withStatus(OK).withBody(responseJson)))

        val result = connector.submitWithCorrections(vatReturnWithCorrectionRequest).futureValue

        result.left.value mustEqual InvalidJson
      }
    }

    "must return Left(NotFound) when the server response with NOT_FOUND" in {

      val vatReturnWithCorrectionRequest = VatReturnWithCorrectionRequest(vatReturnRequest, correctionRequest)

      running(application) {

        val connector = application.injector.instanceOf[VatReturnConnector]

        server.stubFor(post(urlEqualTo(url)).willReturn(aResponse().withStatus(NOT_FOUND)))

        val result = connector.submitWithCorrections(vatReturnWithCorrectionRequest).futureValue

        result.left.value mustEqual NotFound
      }
    }

    "must return Left(ConflictFound) when the server response with CONFLICT" in {

      running(application) {
        val vatReturnRequest = VatReturnRequest(vrn, period, None, None, List.empty, List.empty)
        val correctionRequest = CorrectionRequest(vrn, period, List.empty)
        val vatReturnWithCorrectionRequest = VatReturnWithCorrectionRequest(vatReturnRequest, correctionRequest)

        val connector = application.injector.instanceOf[VatReturnConnector]

        server.stubFor(post(urlEqualTo(url)).willReturn(aResponse().withStatus(CONFLICT)))

        val result = connector.submitWithCorrections(vatReturnWithCorrectionRequest).futureValue

        result.left.value mustEqual ConflictFound
      }
    }

    "must return Left(ReceivedErrorFromCore) when the server responds with a core error" in {

      running(application) {
        val vatReturnRequest = VatReturnRequest(vrn, period, None, None, List.empty, List.empty)
        val correctionRequest = CorrectionRequest(vrn, period, List.empty)
        val vatReturnWithCorrectionRequest = VatReturnWithCorrectionRequest(vatReturnRequest, correctionRequest)

        val connector = application.injector.instanceOf[VatReturnConnector]

        server.stubFor(post(urlEqualTo(url)).willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE)
          .withBody(Json.toJson(coreErrorResponse).toString())))

        val result = connector.submitWithCorrections(vatReturnWithCorrectionRequest).futureValue

        result.left.value mustEqual ReceivedErrorFromCore
      }
    }

    "must return Left(UnexpectedResponseStatus) when the server response with an error code" in {

      running(application) {
        val vatReturnRequest = VatReturnRequest(vrn, period, None, None, List.empty, List.empty)
        val correctionRequest = CorrectionRequest(vrn, period, List.empty)
        val vatReturnWithCorrectionRequest = VatReturnWithCorrectionRequest(vatReturnRequest, correctionRequest)

        val connector = application.injector.instanceOf[VatReturnConnector]

        server.stubFor(post(urlEqualTo(url)).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR)))

        val result = connector.submitWithCorrections(vatReturnWithCorrectionRequest).futureValue

        result.left.value mustBe an[UnexpectedResponseStatus]
      }
    }
  }

  ".get" - {

    val vatReturn = arbitrary[VatReturn].sample.value
    val responseJson = Json.toJson(vatReturn)

    "must return Right(VatReturn) when the server responds with OK" in {

      running(application) {
        val connector = application.injector.instanceOf[VatReturnConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/period/${period.toString}"))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson.toString())
            ))

        connector.get(period).futureValue mustBe Right(vatReturn)
      }
    }

    "must return Left(NotFound) when the server responds with NOT_FOUND" in {

      running(application) {
        val connector = application.injector.instanceOf[VatReturnConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/period/${period.toString}"))
            .willReturn(
              aResponse().withStatus(NOT_FOUND)
            ))

        connector.get(period).futureValue mustBe Left(NotFound)
      }
    }
  }

  "getSavedExternalEntry" - {

    val url = s"/one-stop-shop-returns/external-entry"

    "must return information when the backend returns some" in {

      running(application) {
        val connector = application.injector.instanceOf[VatReturnConnector]

        val externalEntryResponse = ExternalEntryUrl(Some("/url"))

        val responseBody = Json.toJson(externalEntryResponse).toString

        server.stubFor(get(urlEqualTo(url)).willReturn(ok().withBody(responseBody)))

        val result = connector.getSavedExternalEntry().futureValue

        result mustBe Right(externalEntryResponse)
      }
    }

    "must return invalid json when the backend returns some" in {

      running(application) {
        val connector = application.injector.instanceOf[VatReturnConnector]

        val responseBody = Json.obj("test" -> "test").toString()

        server.stubFor(get(urlEqualTo(url)).willReturn(ok().withBody(responseBody)))

        val result = connector.getSavedExternalEntry().futureValue

        result mustBe Right(ExternalEntryUrl(None))
      }
    }

    "must return Left(NotFound) when the backend returns NOT_FOUND" in {

      running(application) {
        val connector = application.injector.instanceOf[VatReturnConnector]

        server.stubFor(get(urlEqualTo(url)).willReturn(notFound()))

        val result = connector.getSavedExternalEntry().futureValue

        result mustBe Left(NotFound)
      }
    }

    "must return Left(UnexpectedStatus) when the backend returns another error code" in {

      val status = Gen.oneOf(400, 500, 501, 502, 503).sample.value

      running(application) {
        val connector = application.injector.instanceOf[VatReturnConnector]

        server.stubFor(get(urlEqualTo(url)).willReturn(aResponse().withStatus(status)))

        val result = connector.getSavedExternalEntry().futureValue

        result mustBe Left(UnexpectedResponseStatus(status, s"Received unexpected response code $status with body "))
      }
    }
  }

  ".getSubmittedVatReturns" - {

    "must return Seq(VatReturn) when connector returns a successful payload" in {

      val vatReturns: Seq[VatReturn] = Gen.listOfN(4, arbitraryVatReturn.arbitrary).sample.value

      running(application) {
        val connector = application.injector.instanceOf[VatReturnConnector]

        val responseBody = Json.toJson(vatReturns).toString()

        server.stubFor(get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(responseBody)
        ))

        val result = connector.getSubmittedVatReturns().futureValue

        result mustBe vatReturns
      }
    }

    "must return Seq.empty when JSON cannot be parsed correctly" in {

      running(application) {
        val connector = application.injector.instanceOf[VatReturnConnector]

        val responseBody: String = """{ "foo": "bar" }"""

        server.stubFor(get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(responseBody)
        ))

        val result = connector.getSubmittedVatReturns().futureValue

        result mustBe Seq.empty
      }
    }

    "must return Seq.empty when connector returns a NotFound" in {

      running(application) {
        val connector = application.injector.instanceOf[VatReturnConnector]

        server.stubFor(get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
        ))

        val result = connector.getSubmittedVatReturns().futureValue

        result mustBe Seq.empty
      }
    }

    "must throw an Exception when connector returns an error" in {

      val error: Int = INTERNAL_SERVER_ERROR

      running(application) {
        val connector = application.injector.instanceOf[VatReturnConnector]

        server.stubFor(get(urlEqualTo(url)).willReturn(
          aResponse()
            .withStatus(error)
        ))

        whenReady(connector.getSubmittedVatReturns().failed) { exp =>
          exp mustBe a[Exception]
          exp.getMessage mustBe s"Received unexpected error from vat returns with status: $error"
        }
      }
    }
  }

  ".getObligations" - {

    val getObligationsUrl: String = s"/one-stop-shop-returns/obligations/$vrn"

    "must return Ok with a Payload of EtmpObligations" in {

      running(application) {
        val connector = application.injector.instanceOf[VatReturnConnector]

        val responseBody = Json.toJson(etmpObligations).toString()

        server.stubFor(get(urlEqualTo(getObligationsUrl)).willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(responseBody)
        ))

        val result = connector.getObligations(vrn).futureValue

        result mustBe etmpObligations
      }
    }
  }

  ".getEtmpVatReturn" - {

    val url = s"/one-stop-shop-returns/etmp-vat-returns/period"

    val etmpVatReturn: EtmpVatReturn = arbitraryEtmpVatReturn.arbitrary.sample.value

    "must return Right(EtmpVatReturn) when server responds with OK" in {

      val jsonResponseBody = Json.toJson(etmpVatReturn)

      running(application) {

        server.stubFor(
          get(urlEqualTo(s"$url/$period"))
            .willReturn(
              aResponse().withStatus(OK)
                .withBody(jsonResponseBody.toString())
            )
        )

        val connector = application.injector.instanceOf[VatReturnConnector]

        val result = connector.getEtmpVatReturn(period).futureValue

        result mustBe Right(etmpVatReturn)
      }
    }

    "must return Left(InvalidJson) when JSON cannot be parsed correctly" in {

      val responseBody: String = """{ "foo": "bar" }"""

      running(application) {

        server.stubFor(
          get(urlEqualTo(s"$url/$period"))
            .willReturn(
              aResponse().withStatus(OK)
                .withBody(responseBody)
            )
        )

        val connector = application.injector.instanceOf[VatReturnConnector]

        val result = connector.getEtmpVatReturn(period).futureValue

        result mustBe Left(InvalidJson)
      }
    }

    "must return Left(UnexpectedResponseStatus) when server responds with any other error response" in {

      running(application) {

        server.stubFor(get(urlEqualTo(s"$url/$period"))
          .willReturn(
            aResponse().withStatus(INTERNAL_SERVER_ERROR)
              .withBody("error")
          )
        )

        val connector = application.injector.instanceOf[VatReturnConnector]

        val result = connector.getEtmpVatReturn(period).futureValue

        result mustBe Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "error"))
      }
    }
  }
}
