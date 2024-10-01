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
import generators.Generators
import models.{Country, EuVatRate}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate

class EuVatRateConnectorSpec extends SpecBase
  with WireMockHelper
  with ScalaCheckPropertyChecks
  with Generators {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val euVatRate1: EuVatRate = arbitrary[EuVatRate].sample.value
  private val euVatRate2: EuVatRate = arbitrary[EuVatRate].sample.value
  private val euVatRates: Seq[EuVatRate] = Seq(euVatRate1, euVatRate2)

  private def application: Application = {
    applicationBuilder()
      .configure("microservice.services.eu-vat-rates.port" -> server.port)
      .build()
  }

  "EuVatRateConnector" - {

    ".getEuVatRates" - {

      val country = arbitrary[Country].sample.value
      val startDate = LocalDate.of(2021, 1, 1)
      val endDate = LocalDate.of(2025, 1, 1)

      val getEuVatRatesUrl: String = s"/eu-vat-rates/vat-rate/${country.code}?startDate=$startDate&endDate=$endDate"

      "must return OK with a payload of EU VAT Rates" in {

        val app = application

        running(app) {
          val connector = app.injector.instanceOf[EuVatRateConnector]

          val responseBody = Json.toJson(euVatRates).toString()

          server.stubFor(
            get(urlEqualTo(getEuVatRatesUrl))
              .willReturn(ok()
                .withBody(responseBody)
              )
          )

          val result = connector.getEuVatRates(country, startDate, endDate).futureValue

          result mustBe euVatRates
        }
      }
    }
  }
}
