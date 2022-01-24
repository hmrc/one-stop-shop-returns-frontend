/*
 * Copyright 2022 HM Revenue & Customs
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

package connectors.corrections

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.WireMockHelper
import models.corrections.CorrectionPayload
import models.responses.{ConflictFound, InvalidJson, NotFound, UnexpectedResponseStatus}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.EitherValues
import play.api.Application
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

class CorrectionConnectorSpec extends SpecBase with WireMockHelper with EitherValues {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()
  val url = "/one-stop-shop-returns/corrections"

  private def application: Application =
    applicationBuilder()
      .configure("microservice.services.one-stop-shop-returns.port" -> server.port)
      .build()

  ".get" - {

    val correctionPayload = arbitrary[CorrectionPayload].sample.value
    val responseJson = Json.toJson(correctionPayload)

    "must return Right(Correction) when the server responds with OK" in {

      running(application) {
        val connector = application.injector.instanceOf[CorrectionConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/${period.toString}"))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson.toString())
            ))

        connector.get(period).futureValue mustBe Right(correctionPayload)
      }
    }

    "must return Left(Invalid JSON) when the server responds with an incorrectly formatted JSON payload" in {

      running(application) {
        val connector = application.injector.instanceOf[CorrectionConnector]

        val responseJson = """{ "foo": "bar" }"""

        server.stubFor(
          get(urlEqualTo(s"$url/${period.toString}"))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson))
            )

        connector.get(period).futureValue mustBe Left(InvalidJson)
      }
    }

    "must return Left(NotFound) when the server responds with NOT_FOUND" in {

      running(application) {
        val connector = application.injector.instanceOf[CorrectionConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/${period.toString}"))
            .willReturn(
              aResponse().withStatus(NOT_FOUND)
            ))

        connector.get(period).futureValue mustBe Left(NotFound)
      }
    }

    "must return Left(ConflictFound) when the server responds with CONFLICT" in {

      running(application) {
        val connector = application.injector.instanceOf[CorrectionConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/${period.toString}"))
            .willReturn(
              aResponse().withStatus(CONFLICT)
            ))

        connector.get(period).futureValue mustBe Left(ConflictFound)
      }
    }

    "must return Left(UnexpectedResponse) when the server responds with an unexpected error from correction" in {

      running(application) {
        val connector = application.injector.instanceOf[CorrectionConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/${period.toString}"))
            .willReturn(
              aResponse().withStatus(IM_A_TEAPOT)
            ))

        connector.get(period).futureValue mustBe Left(UnexpectedResponseStatus(418, "Unexpected response, status 418 returned"))
      }
    }
  }

  ".getForCorrectionPeriod" - {
    val url = "/one-stop-shop-returns/corrections-for-period"
    val correctionPayload = arbitrary[CorrectionPayload].sample.value
    val responseJson = Json.toJson(List(correctionPayload))

    "must return Right(List(Correction)) when the server responds with OK" in {

      running(application) {
        val connector = application.injector.instanceOf[CorrectionConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/${period.toString}"))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson.toString())
            ))

        connector.getForCorrectionPeriod(period).futureValue mustBe Right(List(correctionPayload))
      }
    }

    "must return an empty list when the server responds with NOT_FOUND" in {

      running(application) {
        val connector = application.injector.instanceOf[CorrectionConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/${period.toString}"))
            .willReturn(
              aResponse().withStatus(NOT_FOUND)
            ))

        connector.getForCorrectionPeriod(period).futureValue mustBe Right(List.empty)
      }
    }

    "must return unexpected response when response is not OK" in {

      running(application) {
        val connector = application.injector.instanceOf[CorrectionConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/${period.toString}"))
            .willReturn(
              aResponse().withStatus(123).withBody("")
            ))

        connector.getForCorrectionPeriod(period).futureValue mustBe Left(UnexpectedResponseStatus(123, "Unexpected response, status 123 returned"))
      }
    }

    "must return unexpected response when response is CONFLICT" in {

      running(application) {
        val connector = application.injector.instanceOf[CorrectionConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/${period.toString}"))
            .willReturn(
              aResponse().withStatus(CONFLICT).withBody("")
            ))

        connector.getForCorrectionPeriod(period).futureValue mustBe Left(ConflictFound)
      }
    }

    "must return Left(Invalid JSON) when the server responds with an incorrectly formatted JSON payload" in {

      running(application) {
        val connector = application.injector.instanceOf[CorrectionConnector]

        val responseJson = """{ "foo": "bar" }"""

        server.stubFor(
          get(urlEqualTo(s"$url/${period.toString}"))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson))
        )

        connector.getForCorrectionPeriod(period).futureValue mustBe Left(InvalidJson)
      }
    }
  }
}
