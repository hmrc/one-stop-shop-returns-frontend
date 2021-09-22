/*
 * Copyright 2021 HM Revenue & Customs
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

package connectors.payments

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.{PaymentConnector, WireMockHelper}
import models.responses.UnexpectedResponseStatus
import models.requests.{PaymentPeriod, PaymentRequest, PaymentResponse}
import org.scalatest.EitherValues
import play.api.Application
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

class PaymentConnectorSpec extends SpecBase with WireMockHelper with EitherValues {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()
  val url = "/pay-api/vat-oss/ni-eu-vat-oss/journey/start"

  private def application: Application =
    applicationBuilder()
      .configure("microservice.services.pay-api.port" -> server.port)
      .build()

  private val paymentRequest =
    PaymentRequest(
      vrn,
      PaymentPeriod(period),
      10000000
    )

  ".submit" - {

    "must return Right(PaymentResponse) when the server responds with CREATED" in {
      running(application) {

        val expectedReturnPaymentResponse =
          PaymentResponse(
            "journeyId",
            "nextUrl"
          )
        val responseJson = Json.toJson(expectedReturnPaymentResponse)
        val connector = application.injector.instanceOf[PaymentConnector]

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(aResponse().withStatus(CREATED).withBody(responseJson.toString()))
        )

        val result = connector.submit(paymentRequest).futureValue

        result.value mustEqual expectedReturnPaymentResponse
      }
    }

    "must return Left(UnexpectedResponseStatus) when the server response with an error code" in {

      running(application) {
        val connector = application.injector.instanceOf[PaymentConnector]

        server.stubFor(post(urlEqualTo(url)).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR)))

        val result = connector.submit(paymentRequest).futureValue

        result.left.value mustBe an[UnexpectedResponseStatus]
      }
    }
  }
}
