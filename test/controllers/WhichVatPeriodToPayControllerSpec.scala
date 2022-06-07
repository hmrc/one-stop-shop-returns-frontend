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

package controllers

import base.SpecBase
import connectors.financialdata.CurrentPaymentsHttpParser.CurrentPaymentsResponse
import connectors.financialdata.FinancialDataConnector
import models.financialdata.{CurrentPayments, Payment, PaymentStatus}
import models.responses.InvalidJson
import models.{Period, Quarter}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{GatewayTimeoutException, HeaderCarrier}

import java.time.LocalDate
import scala.concurrent.Future

class WhichVatPeriodToPayControllerSpec extends SpecBase with MockitoSugar {

  private lazy val whichVatPeriodToPayRoute = routes.WhichVatPeriodToPayController.onPageLoad().url

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  "WhichVatPeriodToPay GET" - {

    "when there is 1 period must redirect to payment controller " in {

      val financialDataConnector = mock[FinancialDataConnector]

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector)
        ).build()

      val duePayments = Seq(
        Payment(
          Period(2022, Quarter.Q1),
          BigDecimal(200.0),
          LocalDate.now(),
          PaymentStatus.Unpaid
        )
      )

      when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
        Future.successful(Right(CurrentPayments(duePayments, Seq.empty)))

      running(application) {
        val request = FakeRequest(GET, whichVatPeriodToPayRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.PaymentController.makePayment(
          duePayments.head.period, duePayments.head.amountOwed.longValue() * 100).url

      }
    }

    "if all statuses are UNPAID or PARTIAL" - {

      val financialDataConnector = mock[FinancialDataConnector]

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector)
        ).build()

      val duePayments = Seq(
        Payment(
          Period(2021, Quarter.Q4),
          BigDecimal(100.0),
          LocalDate.now(),
          PaymentStatus.Partial
        ),
        Payment(
          Period(2022, Quarter.Q1),
          BigDecimal(200.0),
          LocalDate.now(),
          PaymentStatus.Unpaid
        )
      )

      when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
        Future.successful(Right(CurrentPayments(duePayments, Seq.empty)))

      running(application) {
        val request = FakeRequest(GET, whichVatPeriodToPayRoute)

        val result = route(application, request).value

        status(result) mustEqual OK

        val responseString = contentAsString(result)

        val doc = Jsoup.parse(responseString)

        "must show due amounts" in {
          val radioLabels = doc.getElementsByClass("govuk-label govuk-radios__label")
          radioLabels.size() mustEqual 2
          radioLabels.get(0).text() mustEqual "You owe £100 for 1 October to 31 December 2021"
          radioLabels.get(1).text() mustEqual "You owe £200 for 1 January to 31 March 2022"
        }

        "must not show banner" in {
          val notificationBanner = doc.getElementsByClass("govuk-notification-banner")
          notificationBanner.isEmpty mustBe true
        }
      }
    }

    "if any statuses are UNKNOWN" - {

      val financialDataConnector = mock[FinancialDataConnector]

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector)
        ).build()

      val duePayments = Seq(
        Payment(
          Period(2021, Quarter.Q3),
          BigDecimal(100.0),
          LocalDate.now(),
          PaymentStatus.Unpaid
        ),
        Payment(
          Period(2021, Quarter.Q4),
          BigDecimal(200.0),
          LocalDate.now(),
          PaymentStatus.Partial
        ),
        Payment(
          Period(2022, Quarter.Q1),
          BigDecimal(300.0),
          LocalDate.now(),
          PaymentStatus.Unknown
        )
      )

      when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
        Future.successful(Right(CurrentPayments(duePayments, Seq.empty)))

      running(application) {
        val request = FakeRequest(GET, whichVatPeriodToPayRoute)

        val result = route(application, request).value

        status(result) mustEqual OK

        val responseString = contentAsString(result)

        val doc = Jsoup.parse(responseString)

        "must not show due amounts" in {
          val radioLabels = doc.getElementsByClass("govuk-label govuk-radios__label")
          radioLabels.size() mustEqual 3
          radioLabels.get(0).text() mustEqual "1 July to 30 September 2021"
          radioLabels.get(1).text() mustEqual "1 October to 31 December 2021"
          radioLabels.get(2).text() mustEqual "1 January to 31 March 2022"
        }

        "must show banner" in {
          val notificationBanner = doc.getElementsByClass("govuk-notification-banner")
          notificationBanner.size() mustEqual 1
        }
      }
    }

    "must redirect to Journey Recovery if call to Financial Controller fails" in {

      val financialDataConnector = mock[FinancialDataConnector]

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector)
        ).build()

      val msg = "GET failed. Caused by: TimeoutException"

      when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn Future.failed(new GatewayTimeoutException(msg))

      running(application) {
        val request = FakeRequest(GET, whichVatPeriodToPayRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery if Financial Data Controller receives error from backend" in {

      val financialDataConnector = mock[FinancialDataConnector]

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, whichVatPeriodToPayRoute)

        when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn Future.successful[CurrentPaymentsResponse](Left(InvalidJson))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  "WhichVatPeriodToPay POST" - {

    "when there is 1 period must redirect to payment controller " in {

      val financialDataConnector = mock[FinancialDataConnector]

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector)
        ).build()

      val duePayments = Seq(
        Payment(
          Period(2022, Quarter.Q1),
          BigDecimal(200.0),
          LocalDate.now(),
          PaymentStatus.Unpaid
        )
      )

      when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
        Future.successful(Right(CurrentPayments(duePayments, Seq.empty)))

      running(application) {
        val request = FakeRequest(POST, whichVatPeriodToPayRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.PaymentController.makePayment(
          duePayments.head.period, duePayments.head.amountOwed.longValue() * 100).url

      }
    }

    "when there is more than one period must redirect to payment controller and selected period" in {

      val financialDataConnector = mock[FinancialDataConnector]

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector)
        ).build()

      val duePayments = Seq(
        Payment(
          Period(2021, Quarter.Q3),
          BigDecimal(100.0),
          LocalDate.now(),
          PaymentStatus.Unpaid
        ),
        Payment(
          Period(2021, Quarter.Q4),
          BigDecimal(200.0),
          LocalDate.now(),
          PaymentStatus.Unpaid
        ),
        Payment(
          Period(2022, Quarter.Q1),
          BigDecimal(300.0),
          LocalDate.now(),
          PaymentStatus.Unpaid
        )
      )

      when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
        Future.successful(Right(CurrentPayments(duePayments, Seq.empty)))

      running(application) {
        val request = FakeRequest(POST, whichVatPeriodToPayRoute)
          .withFormUrlEncodedBody("value" -> duePayments(2).period.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.PaymentController.makePayment(duePayments(2).period, duePayments(2).amountOwed.longValue() * 100).url
      }
    }

    "must redirect to Journey Recovery if call to Financial Controller fails" in {

      val financialDataConnector = mock[FinancialDataConnector]

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector)
        ).build()

      val duePayments = Seq(
        Payment(
          Period(2021, Quarter.Q3),
          BigDecimal(100.0),
          LocalDate.now(),
          PaymentStatus.Unpaid
        ),
        Payment(
          Period(2021, Quarter.Q4),
          BigDecimal(200.0),
          LocalDate.now(),
          PaymentStatus.Unpaid
        ),
        Payment(
          Period(2022, Quarter.Q1),
          BigDecimal(300.0),
          LocalDate.now(),
          PaymentStatus.Unpaid
        )
      )

      val msg = "GET failed. Caused by: TimeoutException"

      when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn Future.failed(new GatewayTimeoutException(msg))

      running(application) {
        val request = FakeRequest(POST, whichVatPeriodToPayRoute)
          .withFormUrlEncodedBody("value" -> duePayments(2).period.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery if Financial Data Controller receives error from backend" in {

      val financialDataConnector = mock[FinancialDataConnector]

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector)
        )
        .build()

      running(application) {

        val duePayments = Seq(
          Payment(
            Period(2021, Quarter.Q3),
            BigDecimal(100.0),
            LocalDate.now(),
            PaymentStatus.Unpaid
          ),
          Payment(
            Period(2021, Quarter.Q4),
            BigDecimal(200.0),
            LocalDate.now(),
            PaymentStatus.Unpaid
          ),
          Payment(
            Period(2022, Quarter.Q1),
            BigDecimal(300.0),
            LocalDate.now(),
            PaymentStatus.Unpaid
          )
        )

        val request = FakeRequest(POST, whichVatPeriodToPayRoute)
          .withFormUrlEncodedBody("value" -> duePayments(2).period.toString)

        when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn Future.successful[CurrentPaymentsResponse](Left(InvalidJson))

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
