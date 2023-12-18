/*
 * Copyright 2023 HM Revenue & Customs
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
import formats.Format
import models.Quarter.{Q1, Q3}
import models.StandardPeriod
import models.financialdata._
import models.responses.{InvalidJson, UnexpectedResponseStatus}
import org.scalatest.EitherValues
import play.api.Application
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate

class FinancialDataConnectorSpec extends SpecBase with WireMockHelper with EitherValues {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  val baseUrl = s"/one-stop-shop-returns/financial-data"

  private def application: Application =
    applicationBuilder()
      .configure("microservice.services.one-stop-shop-returns.port" -> server.port)
      .build()

  ".getCharge" - {

    val url = s"$baseUrl/charge/$period"

    val charge = Some(Charge(period, BigDecimal(1000.50), BigDecimal(1000.50), BigDecimal(1000.50)))
    val responseJson = Json.toJson(charge)

    "must return a Some(charge) when successful" in {

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

    "must return None when no charge" in {

      val responseJson = Json.toJson(None)

      running(application) {
        val connector = application.injector.instanceOf[FinancialDataConnector]

        server.stubFor(
          get(urlEqualTo(s"$url"))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson.toString())
            ))

        connector.getCharge(period).futureValue mustBe Right(None)
      }
    }

    "must return invalid response when invalid charge json returned" in {

      val responseJson = Json.toJson(StandardPeriod(2021, Q3))

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

    "must return Left(UnexpectedResponseStatus) when the server responds with an error code" in {

      running(application) {
        val connector = application.injector.instanceOf[FinancialDataConnector]

        server.stubFor(
          get(urlEqualTo(s"$url"))
            .willReturn(
              aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("")
            ))

        connector.getCharge(period)
          .futureValue mustBe Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, ""))
      }
    }
  }

  ".getPeriodsAndOutstandingAmounts" - {

    val url = s"$baseUrl/outstanding-payments"
    val periodWithOutstandingAmount = PeriodWithOutstandingAmount(period, BigDecimal(1000.50))
    val responseJson = Json.toJson(Seq(periodWithOutstandingAmount))

    "must return a PeriodWithOutstandingAmount when successful" in {

      running(application) {
        val connector = application.injector.instanceOf[FinancialDataConnector]

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson.toString())
            ))

        connector.getPeriodsAndOutstandingAmounts().futureValue mustBe Right(Seq(periodWithOutstandingAmount))
      }
    }

    "must return invalid response when invalid charge json returned" in {

      val responseJson = Json.toJson(StandardPeriod(2021, Q3))

      running(application) {
        val connector = application.injector.instanceOf[FinancialDataConnector]

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson.toString())
            ))

        connector.getPeriodsAndOutstandingAmounts().futureValue mustBe Left(InvalidJson)
      }
    }

    "must return unexpected response when response is not OK" in {

      running(application) {
        val connector = application.injector.instanceOf[FinancialDataConnector]

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(
              aResponse().withStatus(BAD_REQUEST).withBody("")
            ))

        connector.getPeriodsAndOutstandingAmounts().futureValue mustBe Left(UnexpectedResponseStatus(BAD_REQUEST, ""))
      }
    }
  }

  ".getVatReturnWithFinancialData" - {

    val commencementDate = LocalDate.now()
    val url = s"$baseUrl/charge-history/${Format.dateTimeFormatter.format(commencementDate)}"

    "with no correctionPayload" - {

      val vatReturnWithFinancialData =
        VatReturnWithFinancialData(
          completeVatReturn,
          Some(Charge(period, BigDecimal(100), BigDecimal(100), BigDecimal(100))),
          100,
          None
        )
      val responseJson = Json.toJson(Seq(vatReturnWithFinancialData))

      "must return a Some(vatReturnWithFinancialData) when successful" in {

        running(application) {
          val connector = application.injector.instanceOf[FinancialDataConnector]

          server.stubFor(
            get(urlEqualTo(s"$url"))
              .willReturn(
                aResponse().withStatus(OK).withBody(responseJson.toString())
              ))

          connector.getVatReturnWithFinancialData(commencementDate).futureValue mustBe Right(Seq(vatReturnWithFinancialData))
        }
      }
    }

    "with a single correctionPayload" - {

      val vatReturnWithFinancialData =
        VatReturnWithFinancialData(
          completeVatReturn,
          Some(Charge(period, BigDecimal(100), BigDecimal(100), BigDecimal(100))),
          100,
          Some(emptyCorrectionPayload)
        )
      val responseJson = Json.toJson(Seq(vatReturnWithFinancialData))

      "must return a Some(vatReturnWithFinancialData) when successful" in {

        running(application) {
          val connector = application.injector.instanceOf[FinancialDataConnector]

          server.stubFor(
            get(urlEqualTo(s"$url"))
              .willReturn(
                aResponse().withStatus(OK).withBody(responseJson.toString())
              ))

          connector.getVatReturnWithFinancialData(commencementDate).futureValue mustBe Right(Seq(vatReturnWithFinancialData))
        }
      }
    }

    "must return invalid response when invalid json returned" in {

      val responseJson = Json.toJson(StandardPeriod(2021, Q3))

      running(application) {
        val connector = application.injector.instanceOf[FinancialDataConnector]

        server.stubFor(
          get(urlEqualTo(s"$url"))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson.toString())
            ))

        connector.getVatReturnWithFinancialData(commencementDate).futureValue mustBe Left(InvalidJson)
      }
    }

    "must return unexpected response if response code is not OK" in {

      running(application) {
        val connector = application.injector.instanceOf[FinancialDataConnector]

        server.stubFor(
          get(urlEqualTo(s"$url"))
            .willReturn(
              aResponse().withStatus(CREATED)
            ))

        connector.getVatReturnWithFinancialData(commencementDate).futureValue
          .mustBe(Left(UnexpectedResponseStatus(CREATED, "")))
      }
    }
  }

  ".getCurrentPayments" - {

    val period = StandardPeriod(2022, Q1)

    val url = s"$baseUrl/prepare/${vrn.vrn}"

    val payment = Payment(period, 1000L, period.paymentDeadline, PaymentStatus.Unpaid)

    val currentPayments = CurrentPayments(Seq(payment), Seq(payment), payment.amountOwed, payment.amountOwed + payment.amountOwed)
    val responseJson = Json.toJson(currentPayments)

    "must return Current Payments when successful" in {

      running(application) {
        val connector = application.injector.instanceOf[FinancialDataConnector]

        server.stubFor(
          get(urlEqualTo(s"$url"))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson.toString())
            ))

        connector.getCurrentPayments(vrn).futureValue mustBe Right(currentPayments)
      }
    }

    "must return invalid response when invalid current payments json returned" in {

      val responseJson = Json.toJson(StandardPeriod(2021, Q3))

      running(application) {
        val connector = application.injector.instanceOf[FinancialDataConnector]

        server.stubFor(
          get(urlEqualTo(s"$url"))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson.toString())
            ))

        connector.getCurrentPayments(vrn).futureValue mustBe Left(InvalidJson)
      }
    }

    "must return Left(UnexpectedResponseStatus) when the server responds with an error code" in {

      running(application) {
        val connector = application.injector.instanceOf[FinancialDataConnector]

        server.stubFor(
          get(urlEqualTo(s"$url"))
            .willReturn(
              aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("")
            ))

        connector.getCurrentPayments(vrn)
          .futureValue mustBe Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, ""))
      }
    }
  }
}
