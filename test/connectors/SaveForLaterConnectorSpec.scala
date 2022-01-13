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
import models.requests.SaveForLaterRequest
import models.responses.{ConflictFound, NotFound, UnexpectedResponseStatus}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.EitherValues
import play.api.Application
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant

class SaveForLaterConnectorSpec extends SpecBase with WireMockHelper with EitherValues {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()
  val url = "/one-stop-shop-returns/save-for-later"

  private def application: Application =
    applicationBuilder()
      .configure("microservice.services.one-stop-shop-returns.port" -> server.port)
      .build()

  ".submit" - {

    "must return Right(SavedUserAnswers) when the server responds with CREATED" in {

      running(application) {
        val saveForLaterRequest = SaveForLaterRequest(vrn, period, Json.toJson("test"))
        val expectedSavedUserAnswers =
          SavedUserAnswers(
            vrn, period, Json.toJson("Hello"),
            Instant.now(stubClockAtArbitraryDate)
          )
        val responseJson = Json.toJson(expectedSavedUserAnswers)
        val connector = application.injector.instanceOf[SaveForLaterConnector]

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(aResponse().withStatus(CREATED).withBody(responseJson.toString()))
        )

        val result = connector.submit(saveForLaterRequest).futureValue

        result.value mustEqual expectedSavedUserAnswers
      }
    }

    "must return Left(ConflictFound) when the server response with CONFLICT" in {

      running(application) {
        val saveForLaterRequest = SaveForLaterRequest(vrn, period, Json.toJson("test"))

        val connector = application.injector.instanceOf[SaveForLaterConnector]

        server.stubFor(post(urlEqualTo(url)).willReturn(aResponse().withStatus(CONFLICT)))

        val result = connector.submit(saveForLaterRequest).futureValue

        result.left.value mustEqual ConflictFound
      }
    }

    "must return Left(UnexpectedResponseStatus) when the server response with an error code" in {

      running(application) {
        val saveForLaterRequest = SaveForLaterRequest(vrn, period, Json.toJson("test"))

        val connector = application.injector.instanceOf[SaveForLaterConnector]

        server.stubFor(post(urlEqualTo(url)).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR)))

        val result = connector.submit(saveForLaterRequest).futureValue

        result.left.value mustBe an[UnexpectedResponseStatus]
      }
    }
  }

  ".get" - {

    val savedUserAnswers = arbitrary[SavedUserAnswers].sample.value
    val responseJson = Json.toJson(savedUserAnswers)

    "must return Right(SavedUserAnswers) when the server responds with OK" in {

      running(application) {
        val connector = application.injector.instanceOf[SaveForLaterConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/${vrn}/${period.toString}"))
          .willReturn(
            aResponse().withStatus(OK).withBody(responseJson.toString())
          ))

        connector.get(vrn, period).futureValue mustBe Right(savedUserAnswers)
      }
    }

    "must return Left(NotFound) when the server responds with NOT_FOUND" in {

      running(application) {
        val connector = application.injector.instanceOf[SaveForLaterConnector]

        server.stubFor(
          get(urlEqualTo(s"$url/${vrn}/${period.toString}"))
            .willReturn(
              aResponse().withStatus(NOT_FOUND)
            ))

        connector.get(vrn, period).futureValue mustBe Left(NotFound)
      }
    }
  }
}
