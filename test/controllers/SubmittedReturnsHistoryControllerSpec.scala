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

package controllers

import base.SpecBase
import connectors.VatReturnConnector
import connectors.corrections.CorrectionConnector
import connectors.financialdata.FinancialDataConnector
import models.Quarter.{Q1, Q2, Q3}
import models.corrections.{CorrectionPayload, CorrectionToCountry, PeriodWithCorrections}
import models.financialdata.{CurrentPayments, Payment, PaymentStatus}
import models.responses.UnexpectedResponseStatus
import models.StandardPeriod
import models.external.ExternalEntryUrl
import models.responses.NotFound
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.mockito.MockitoSugar.mock
import org.scalacheck.Arbitrary
import org.scalatest.BeforeAndAfterEach
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Vrn
import views.html.SubmittedReturnsHistoryView

import java.time.{Instant, LocalDate}
import scala.concurrent.Future

class SubmittedReturnsHistoryControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val vatReturnConnector = mock[VatReturnConnector]
  private val correctionConnector = mock[CorrectionConnector]
  private val financialDataConnector = mock[FinancialDataConnector]

  private val period1 = StandardPeriod(2021, Q1)
  private val period2 = StandardPeriod(2021, Q2)
  private val period3 = StandardPeriod(2021, Q3)

  private val payment1 = Payment(period1, BigDecimal(1000), LocalDate.now(), PaymentStatus.Unpaid)
  private val payment2 = Payment(period2, BigDecimal(500), LocalDate.now(), PaymentStatus.Partial)
  private val payment3 = Payment(period3, BigDecimal(500), LocalDate.now(), PaymentStatus.Unpaid)
  private val currentPayments = CurrentPayments(
    duePayments = Seq(payment2, payment3),
    overduePayments = Seq(payment1),
    excludedPayments = Seq.empty,
    totalAmountOwed = BigDecimal(1000),
    totalAmountOverdue = BigDecimal(500)
  )
  private val unknownPaymentAmount = Payment(period3, BigDecimal(100), LocalDate.now(), PaymentStatus.Unknown)
  private val unknownPayment = CurrentPayments(
    duePayments = Seq(unknownPaymentAmount),
    overduePayments = Seq.empty,
    excludedPayments = Seq.empty,
    totalAmountOwed = BigDecimal(0),
    totalAmountOverdue = BigDecimal(0)
  )
  private val emptyPayments = CurrentPayments(
    duePayments = Seq.empty,
    overduePayments = Seq.empty,
    excludedPayments = Seq.empty,
    totalAmountOwed = BigDecimal(0),
    totalAmountOverdue = BigDecimal(0)
  )

  override def beforeEach(): Unit = {
    Mockito.reset(vatReturnConnector)
    Mockito.reset(financialDataConnector)
    Mockito.reset(correctionConnector)
    super.beforeEach()
  }

  "SubmittedReturnsHistory Controller" - {

    "must return OK and correct view with the current period when a return for this period exists" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector),
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[CorrectionConnector].toInstance(correctionConnector)
        ).build()

      when(financialDataConnector.getFinancialData(any())(any())) thenReturn Future.successful(Right(currentPayments))
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))
      when(vatReturnConnector.getSubmittedVatReturns()(any())) thenReturn Future.successful(Seq(completeVatReturn))
      when(correctionConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          Map(period3 -> payment3),
          displayBanner = false
        )(request, messages(application)).toString
      }
    }

    "must return OK and correct view with no-returns message when no returns exist" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector),
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[CorrectionConnector].toInstance(correctionConnector)
        ).build()

      when(financialDataConnector.getFinancialData(any())(any())) thenReturn Future.successful(Right(emptyPayments))
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))
      when(vatReturnConnector.getSubmittedVatReturns()(any())) thenReturn Future.successful(Seq.empty)

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          Map.empty,
          displayBanner = false
        )(request, messages(application)).toString
      }
    }

    "must throw an exception when an unexpected result is returned" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector),
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[CorrectionConnector].toInstance(correctionConnector)
        ).build()

      when(financialDataConnector.getFinancialData(any())(any())) thenReturn Future.failed(new Exception("Some Exception"))
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value

        whenReady(result.failed) { exp => exp mustBe a[Exception] }
      }
    }

    "must throw an exception when the connector returns Left(ErrorResponse)" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector),
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[CorrectionConnector].toInstance(correctionConnector)
        ).build()

      when(financialDataConnector.getFinancialData(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(123, "error")))
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))
      when(correctionConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value

        whenReady(result.failed) { exp => exp mustBe a[Exception] }
      }
    }

    "must return OK and correct view when a vat return exists but no charge is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector),
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[CorrectionConnector].toInstance(correctionConnector)
        ).build()

      when(financialDataConnector.getFinancialData(any())(any()))
        .thenReturn(Future.successful(Right(unknownPayment)))
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))
      when(vatReturnConnector.getSubmittedVatReturns()(any())) thenReturn Future.successful(Seq(completeVatReturn))
      when(correctionConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          Map(period3 -> unknownPaymentAmount),
          displayBanner = true
        )(request, messages(application)).toString
      }
    }

    "must return OK and correct view when a nil vat return exists and no charge is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector),
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[CorrectionConnector].toInstance(correctionConnector)
        ).build()

      when(financialDataConnector.getFinancialData(any())(any()))
        .thenReturn(Future.successful(Right(emptyPayments)))
      when(vatReturnConnector.getSubmittedVatReturns()(any())) thenReturn Future.successful(Seq(emptyVatReturn))
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))
      when(correctionConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          Map(period3 -> Payment(
            period = period3,
            amountOwed = 0,
            dateDue = period.paymentDeadline,
            paymentStatus = PaymentStatus.NilReturn
          )),
          displayBanner = false
        )(request, messages(application)).toString
      }
    }

    "must return OK and correct view when a correction exists" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector),
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[CorrectionConnector].toInstance(correctionConnector)
        ).build()

      val completedCorrectionPayload: CorrectionPayload =
        CorrectionPayload(
          Vrn("063407423"),
          StandardPeriod("2086", "Q3").get,
          List(PeriodWithCorrections(
            period,
            Some(List(Arbitrary.arbitrary[CorrectionToCountry].sample.value))
          )),
          Instant.ofEpochSecond(1630670836),
          Instant.ofEpochSecond(1630670836)
        )

      when(financialDataConnector.getFinancialData(any())(any())) thenReturn Future.successful(Right(currentPayments))
      when(vatReturnConnector.getSubmittedVatReturns()(any())) thenReturn Future.successful(Seq(emptyVatReturn))
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))
      when(correctionConnector.get(any())(any())) thenReturn Future.successful(Right(completedCorrectionPayload))

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          Map(period3 -> payment3),
          displayBanner = false
        )(request, messages(application)).toString
      }
    }

    "must return OK and correct view and add the external backToYourAccount url that has been saved" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector),
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[CorrectionConnector].toInstance(correctionConnector)
        ).build()

      val completedCorrectionPayload: CorrectionPayload =
        CorrectionPayload(
          Vrn("063407423"),
          StandardPeriod("2086", "Q3").get,
          List(PeriodWithCorrections(
            period,
            Some(List(Arbitrary.arbitrary[CorrectionToCountry].sample.value))
          )),
          Instant.ofEpochSecond(1630670836),
          Instant.ofEpochSecond(1630670836)
        )

      when(financialDataConnector.getFinancialData(any())(any())) thenReturn Future.successful(Right(currentPayments))
      when(vatReturnConnector.getSubmittedVatReturns()(any())) thenReturn Future.successful(Seq(emptyVatReturn))
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(Some("example"))))
      when(correctionConnector.get(any())(any())) thenReturn Future.successful(Right(completedCorrectionPayload))

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          Map(period3 -> payment3),
          displayBanner = false,
          Some("example")
        )(request, messages(application)).toString
      }
    }

  }
}
