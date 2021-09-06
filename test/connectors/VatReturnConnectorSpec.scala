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

package connectors

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock._
import models.requests.VatReturnRequest
import models.responses.{ConflictFound, UnexpectedResponseStatus}
import models.domain.VatReturn
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.EitherValues
import play.api.Application
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

class VatReturnConnectorSpec extends SpecBase with WireMockHelper with EitherValues {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()
  val url = "/one-stop-shop-returns/vat-returns"

  private def application: Application =
    applicationBuilder()
      .configure("microservice.services.one-stop-shop-returns.port" -> server.port)
      .build()

  ".submit" - {

    "must return Right when the server responds with CREATED" in {

      running(application) {
        val vatReturnRequest = VatReturnRequest(vrn, period, None, None, List.empty, List.empty)

        val connector = application.injector.instanceOf[VatReturnConnector]

        server.stubFor(post(urlEqualTo(url)).willReturn(aResponse().withStatus(CREATED)))

        val result = connector.submit(vatReturnRequest).futureValue

        result.value mustEqual()
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

  ".get" - {

    val vatReturn = arbitrary[VatReturn].sample.value

    val responseJson = Json.toJson(vatReturn)

    "must return Right when the server responds with OK" in {

      running(application) {
        val connector = application.injector.instanceOf[VatReturnConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/${period.toString}"))
          .willReturn(
            aResponse().withStatus(OK).withBody(responseJson.toString())
          ))

        val result = connector.get(period).futureValue

        val expectedResult = vatReturn

        result.value mustEqual expectedResult
      }
    }

  }
}
