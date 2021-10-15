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

package connectors.financialdata

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.WireMockHelper
import models.Period
import models.Quarter.Q3
import models.financialdata.Charge
import models.responses.InvalidJson
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.EitherValues
import play.api.Application
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

class FinancialDataConnectorSpec extends SpecBase with WireMockHelper with EitherValues {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  val url = s"/one-stop-shop-returns/financial-data/charge/$period"

  private def application: Application =
    applicationBuilder()
      .configure("microservice.services.one-stop-shop-returns.port" -> server.port)

      .build()

  ".get" - {

    val charge = arbitrary[Charge].sample.value
    val responseJson = Json.toJson(charge)

    "must return a charge when successful" in {

      running(application) {
        val connector = application.injector.instanceOf[FinancialDataConnector]

        server.stubFor(
          get(urlEqualTo(s"$url"))
          .willReturn(
            aResponse().withStatus(OK).withBody(responseJson.toString())
          ))

        connector.getCharge(period).futureValue mustBe Right(charge)
      }
    }

    "must return invalid response when invalid charge json returned" in {

      val responseJson = Json.toJson(Period(2021, Q3))

      running(application) {
        val connector = application.injector.instanceOf[FinancialDataConnector]

        server.stubFor(
          get(urlEqualTo(s"$url"))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson.toString())
            ))

        connector.getCharge(period).futureValue mustBe Left(InvalidJson)
      }
    }
  }
}
