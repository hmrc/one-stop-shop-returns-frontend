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

package connectors

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock._
import formats.Format
import models.{Period, PeriodWithStatus}
import models.responses.{InvalidJson, UnexpectedResponseStatus}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.EitherValues
import play.api.Application
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.yourAccount.{Return, OpenReturns}

import java.time.LocalDate

class ReturnStatusConnectorSpec extends SpecBase with WireMockHelper with EitherValues {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()
  val url = "/one-stop-shop-returns/vat-returns"

  private def application: Application =
    applicationBuilder()
      .configure("microservice.services.one-stop-shop-returns.port" -> server.port)
      .build()

  ".listStatuses" - {

    val periodWithStatus = arbitrary[PeriodWithStatus].sample.value
    val responseJson = Json.toJson(Seq(periodWithStatus))
    val commencementDate = LocalDate.now()

    "return a list of statuses for a single period" in {

      running(application) {
        val connector = application.injector.instanceOf[ReturnStatusConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/statuses/${Format.dateTimeFormatter.format(commencementDate)}"))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson.toString())
            ))

        connector.listStatuses(commencementDate).futureValue mustBe Right(Seq(periodWithStatus))
      }
    }

    "must return Left(InvalidJson) when the server responds with an incorrectly formatted JSON payload" in {

      running(application) {
        val connector = application.injector.instanceOf[ReturnStatusConnector]

        val responseJson = """{ "foo": "bar" }"""

        server.stubFor(
          get(urlEqualTo(s"$url/statuses/${Format.dateTimeFormatter.format(commencementDate)}"))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson)
            )
        )

        connector.listStatuses(commencementDate).futureValue mustBe Left(InvalidJson)
      }
    }

    "must return Left(UnexpectedResponseStatus) when the server responds with an error code" in {

      running(application) {
        val connector = application.injector.instanceOf[ReturnStatusConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/statuses/${Format.dateTimeFormatter.format(commencementDate)}"))
            .willReturn(
              aResponse().withStatus(INTERNAL_SERVER_ERROR)
            )
        )

        val result = connector.listStatuses(commencementDate).futureValue
        result.left.value mustBe an[UnexpectedResponseStatus]
      }
    }
  }

  ".getCurrentReturns" - {

    val period = arbitrary[Period].sample.value
    val responseJson = Json.toJson(OpenReturns(None, Some(Return.fromPeriod(period)), Seq.empty))
    val commencementDate = LocalDate.now()

    "return a Returns model" in {

      running(application) {
        val connector = application.injector.instanceOf[ReturnStatusConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/current-returns/${vrn.vrn}"))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson.toString())
            ))

        connector.getCurrentReturns(vrn).futureValue mustBe Right(OpenReturns(None, Some(Return.fromPeriod(period)), Seq.empty))
      }
    }

    "must return Left(InvalidJson) when the server responds with an incorrectly formatted JSON payload" in {

      running(application) {
        val connector = application.injector.instanceOf[ReturnStatusConnector]

        val responseJson = """{ "foo": "bar" }"""

        server.stubFor(
          get(urlEqualTo(s"$url/current-returns/${vrn.vrn}"))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson)
            )
        )

        connector.getCurrentReturns(vrn).futureValue mustBe Left(InvalidJson)
      }
    }

    "must return Left(UnexpectedResponseStatus) when the server responds with an error code" in {

      running(application) {
        val connector = application.injector.instanceOf[ReturnStatusConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/current-returns/${vrn.vrn}"))
            .willReturn(
              aResponse().withStatus(INTERNAL_SERVER_ERROR)
            )
        )

        val result = connector.getCurrentReturns(vrn).futureValue
        result.left.value mustBe an[UnexpectedResponseStatus]
      }
    }
  }
}
