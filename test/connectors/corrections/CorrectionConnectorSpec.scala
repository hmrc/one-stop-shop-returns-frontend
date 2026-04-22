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

package connectors.corrections

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.WireMockHelper
import models.Country
import models.corrections.ReturnCorrectionValue
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
  
  ".getReturnCorrectionValue" - {
    val url = "/one-stop-shop-returns/max-correction-value"
    val returnCorrectionValue = arbitrary[ReturnCorrectionValue].sample.value
    val responseJson = Json.toJson(returnCorrectionValue)

    val country1 = arbitrary[Country].sample.value

    "must return ReturnCorrectionValue when the server responds with OK" in {

      running(application) {
        val connector = application.injector.instanceOf[CorrectionConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/${country1.code}/${period.toString}"))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson.toString())
            ))

        connector.getReturnCorrectionValue(country1.code, period).futureValue mustBe returnCorrectionValue
      }
    }

    "must return an error when the server responds with NOT_FOUND" in {

      running(application) {
        val connector = application.injector.instanceOf[CorrectionConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/${country1.code}/${period.toString}"))
            .willReturn(
              aResponse().withStatus(NOT_FOUND)
            ))

        whenReady(connector.getReturnCorrectionValue(country1.code, period).failed) { exp =>
          exp mustBe a[Exception]
        }
      }
    }

    "must return unexpected response when response is not OK" in {

      running(application) {
        val connector = application.injector.instanceOf[CorrectionConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/${country1.code}/${period.toString}"))
            .willReturn(
              aResponse().withStatus(123).withBody("")
            ))

        whenReady(connector.getReturnCorrectionValue(country1.code, period).failed) { exp =>
          exp mustBe a[Exception]
        }
      }
    }

    "must return Invalid JSON when the server responds with an incorrectly formatted JSON payload" in {

      running(application) {
        val connector = application.injector.instanceOf[CorrectionConnector]

        val responseJson = """{ "foo": "bar" }"""

        server.stubFor(
          get(urlEqualTo(s"$url/${country1.code}/${period.toString}"))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson))
        )

        whenReady(connector.getReturnCorrectionValue(country1.code, period).failed) { exp =>
          exp mustBe a[Exception]
        }
      }
    }
  }
}
