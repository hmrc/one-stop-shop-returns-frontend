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
import connectors.financialdata.FinancialDataConnector
import models.Quarter.{Q1, Q2, Q3}
import models.StandardPeriod
import models.etmp.{EtmpObligationDetails, EtmpObligationsFulfilmentStatus, EtmpVatReturn}
import models.external.ExternalEntryUrl
import models.financialdata.{CurrentPayments, Payment, PaymentStatus}
import models.responses.UnexpectedResponseStatus
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.ObligationsService
import utils.FutureSyntax.FutureOps
import views.html.SubmittedReturnsHistoryView

import java.time.LocalDate
import scala.concurrent.Future

class SubmittedReturnsHistoryControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val mockVatReturnConnector = mock[VatReturnConnector]
  private val mockFinancialDataConnector = mock[FinancialDataConnector]
  private val mockObligationsService: ObligationsService = mock[ObligationsService]

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

  private val etmpObligationDetails: EtmpObligationDetails = EtmpObligationDetails(EtmpObligationsFulfilmentStatus.Fulfilled, "21Q3")

  private val etmpVatReturn: EtmpVatReturn = arbitraryEtmpVatReturn.arbitrary.sample.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockVatReturnConnector)
    Mockito.reset(mockFinancialDataConnector)
    Mockito.reset(mockObligationsService)
    super.beforeEach()
  }

  "SubmittedReturnsHistory Controller" - {

    "when strategicReturnApiEnabled is true" - {

      "must return OK and correct view with the current period when a return for this period exists" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> true)
          .overrides(
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[VatReturnConnector].toInstance(mockVatReturnConnector),
            bind[ObligationsService].toInstance(mockObligationsService)
          ).build()

        when(mockFinancialDataConnector.getFinancialData(any())(any())) thenReturn Right(currentPayments).toFuture
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture
        when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Seq(etmpVatReturn).toFuture
        when(mockObligationsService.getFulfilledObligations(any())(any())) thenReturn Seq(etmpObligationDetails).toFuture

        running(application) {
          val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

          val result = route(application, request).value
          val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            Map(period3 -> payment3),
            displayBanner = false
          )(request, messages(application)).toString
        }
      }

      "must return OK and correct view with no-returns message when no returns exist" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> true)
          .overrides(
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[VatReturnConnector].toInstance(mockVatReturnConnector),
            bind[ObligationsService].toInstance(mockObligationsService)
          ).build()

        when(mockFinancialDataConnector.getFinancialData(any())(any())) thenReturn Right(emptyPayments).toFuture
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture
        when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Seq.empty.toFuture
        when(mockObligationsService.getFulfilledObligations(any())(any())) thenReturn Seq.empty.toFuture

        running(application) {
          val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            Map.empty,
            displayBanner = false
          )(request, messages(application)).toString
        }
      }

      "must throw an exception when an unexpected result is returned" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> true)
          .overrides(
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[VatReturnConnector].toInstance(mockVatReturnConnector),
            bind[ObligationsService].toInstance(mockObligationsService)
          ).build()

        when(mockFinancialDataConnector.getFinancialData(any())(any())) thenReturn Future.failed(new Exception("Some Exception"))
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture
        when(mockObligationsService.getFulfilledObligations(any())(any())) thenReturn Seq(etmpObligationDetails).toFuture

        running(application) {
          val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

          val result = route(application, request).value

          whenReady(result.failed) { exp => exp mustBe a[Exception] }
        }
      }

      "must throw an exception when the connector returns Left(ErrorResponse)" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> true)
          .overrides(
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[VatReturnConnector].toInstance(mockVatReturnConnector)
          ).build()

        when(mockFinancialDataConnector.getFinancialData(any())(any())) thenReturn Left(UnexpectedResponseStatus(123, "error")).toFuture
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture

        running(application) {
          val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

          val result = route(application, request).value

          whenReady(result.failed) { exp => exp mustBe a[Exception] }
        }
      }

      "must return OK and correct view when a vat return exists but no charge is found" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> true)
          .overrides(
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[VatReturnConnector].toInstance(mockVatReturnConnector),
            bind[ObligationsService].toInstance(mockObligationsService)
          ).build()

        when(mockFinancialDataConnector.getFinancialData(any())(any())) thenReturn Right(unknownPayment).toFuture
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture
        when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Seq(emptyVatReturn).toFuture
        when(mockObligationsService.getFulfilledObligations(any())(any())) thenReturn Seq(etmpObligationDetails).toFuture

        running(application) {
          val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

          val result = route(application, request).value
          val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            Map(period3 -> unknownPaymentAmount),
            displayBanner = true
          )(request, messages(application)).toString
        }
      }

      "must throw an Illegal State Exception when a nil vat return exists and no charge is found" in {

        val nilEtmpVatReturn: EtmpVatReturn = etmpVatReturn.copy(
          periodKey = "21Q3",
          goodsSupplied = Seq.empty,
          totalVATGoodsSuppliedGBP = BigDecimal(0),
          totalVATAmountPayable = BigDecimal(0),
          totalVATAmountPayableAllSpplied = BigDecimal(0),
          correctionPreviousVATReturn = Seq.empty,
          totalVATAmountFromCorrectionGBP = BigDecimal(0),
          balanceOfVATDueForMS = Seq.empty,
          totalVATAmountDueForAllMSGBP = BigDecimal(0)
        )

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> true)
          .overrides(
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[VatReturnConnector].toInstance(mockVatReturnConnector),
            bind[ObligationsService].toInstance(mockObligationsService)
          ).build()

        when(mockObligationsService.getFulfilledObligations(any())(any())) thenReturn Seq(etmpObligationDetails).toFuture
        when(mockFinancialDataConnector.getFinancialData(any())(any())) thenReturn Right(emptyPayments).toFuture
        when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Seq(nilEtmpVatReturn).toFuture
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture

        running(application) {
          val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

          val result = route(application, request).value

          whenReady(result.failed) { exp =>
            exp mustBe a[IllegalStateException]
            exp.getMessage mustBe "Unable to find period in financial data for expected period 2021-Q3"
          }
        }
      }

      "must return OK and correct view when a correction exists" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> true)
          .overrides(
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[VatReturnConnector].toInstance(mockVatReturnConnector),
            bind[ObligationsService].toInstance(mockObligationsService)
          ).build()

        when(mockFinancialDataConnector.getFinancialData(any())(any())) thenReturn Right(currentPayments).toFuture
        when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Seq(etmpVatReturn).toFuture
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture
        when(mockObligationsService.getFulfilledObligations(any())(any())) thenReturn Seq(etmpObligationDetails).toFuture

        running(application) {
          val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

          val result = route(application, request).value
          val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            Map(period3 -> payment3),
            displayBanner = false
          )(request, messages(application)).toString
        }
      }

      "must return OK and correct view and add the external backToYourAccount url that has been saved" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> true)
          .overrides(
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[VatReturnConnector].toInstance(mockVatReturnConnector),
            bind[ObligationsService].toInstance(mockObligationsService)
          ).build()

        when(mockFinancialDataConnector.getFinancialData(any())(any())) thenReturn Right(currentPayments).toFuture
        when(mockVatReturnConnector.getEtmpVatReturn(any())(any())) thenReturn Seq(etmpVatReturn).toFuture
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(Some("example"))).toFuture
        when(mockObligationsService.getFulfilledObligations(any())(any())) thenReturn Seq(etmpObligationDetails).toFuture

        running(application) {
          val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

          val result = route(application, request).value
          val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            Map(period3 -> payment3),
            displayBanner = false,
            Some("example")
          )(request, messages(application)).toString
        }
      }
    }

    "when strategicReturnApiEnabled is false" - {

      "must return OK and correct view with the current period when a return for this period exists" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> false)
          .overrides(
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[VatReturnConnector].toInstance(mockVatReturnConnector)
          ).build()

        when(mockFinancialDataConnector.getFinancialData(any())(any())) thenReturn Future.successful(Right(currentPayments))
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))
        when(mockVatReturnConnector.getSubmittedVatReturns()(any())) thenReturn Future.successful(Seq(completeVatReturn))

        running(application) {
          val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

          val result = route(application, request).value
          val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            Map(period3 -> payment3),
            displayBanner = false
          )(request, messages(application)).toString
        }
      }

      "must return OK and correct view with no-returns message when no returns exist" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> false)
          .overrides(
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[VatReturnConnector].toInstance(mockVatReturnConnector)
          ).build()

        when(mockFinancialDataConnector.getFinancialData(any())(any())) thenReturn Future.successful(Right(emptyPayments))
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))
        when(mockVatReturnConnector.getSubmittedVatReturns()(any())) thenReturn Future.successful(Seq.empty)

        running(application) {
          val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            Map.empty,
            displayBanner = false
          )(request, messages(application)).toString
        }
      }

      "must throw an exception when an unexpected result is returned" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[VatReturnConnector].toInstance(mockVatReturnConnector)
          ).build()

        when(mockFinancialDataConnector.getFinancialData(any())(any())) thenReturn Future.failed(new Exception("Some Exception"))
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

        running(application) {
          val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

          val result = route(application, request).value

          whenReady(result.failed) { exp => exp mustBe a[Exception] }
        }
      }

      "must throw an exception when the connector returns Left(ErrorResponse)" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> false)
          .overrides(
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[VatReturnConnector].toInstance(mockVatReturnConnector)
          ).build()

        when(mockFinancialDataConnector.getFinancialData(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(123, "error")))
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

        running(application) {
          val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

          val result = route(application, request).value

          whenReady(result.failed) { exp => exp mustBe a[Exception] }
        }
      }

      "must return OK and correct view when a vat return exists but no charge is found" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> false)
          .overrides(
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[VatReturnConnector].toInstance(mockVatReturnConnector)
          ).build()

        when(mockFinancialDataConnector.getFinancialData(any())(any()))
          .thenReturn(Future.successful(Right(unknownPayment)))
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))
        when(mockVatReturnConnector.getSubmittedVatReturns()(any())) thenReturn Future.successful(Seq(completeVatReturn))

        running(application) {
          val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

          val result = route(application, request).value
          val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            Map(period3 -> unknownPaymentAmount),
            displayBanner = true
          )(request, messages(application)).toString
        }
      }

      "must throw an IllegalStateException when a nil vat return exists and no charge is found" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> false)
          .overrides(
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[VatReturnConnector].toInstance(mockVatReturnConnector)
          ).build()

        when(mockFinancialDataConnector.getFinancialData(any())(any()))
          .thenReturn(Future.successful(Right(emptyPayments)))
        when(mockVatReturnConnector.getSubmittedVatReturns()(any())) thenReturn Future.successful(Seq(emptyVatReturn))
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))
        when(mockVatReturnConnector.get(eqTo(period3))(any())) thenReturn Future.successful(Right(emptyVatReturn))

        running(application) {
          val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

          val result = route(application, request).value

          whenReady(result.failed) { exp =>
            exp mustBe a[IllegalStateException]
            exp.getMessage mustBe "Unable to find period in financial data for expected period 2021-Q3"
          }
        }
      }

      "must return OK and correct view when a correction exists" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> false)
          .overrides(
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[VatReturnConnector].toInstance(mockVatReturnConnector)
          ).build()

        when(mockFinancialDataConnector.getFinancialData(any())(any())) thenReturn Future.successful(Right(currentPayments))
        when(mockVatReturnConnector.getSubmittedVatReturns()(any())) thenReturn Future.successful(Seq(emptyVatReturn))
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

        running(application) {
          val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

          val result = route(application, request).value
          val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            Map(period3 -> payment3),
            displayBanner = false
          )(request, messages(application)).toString
        }
      }

      "must return OK and correct view and add the external backToYourAccount url that has been saved" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> false)
          .overrides(
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[VatReturnConnector].toInstance(mockVatReturnConnector)
          ).build()

        when(mockFinancialDataConnector.getFinancialData(any())(any())) thenReturn Future.successful(Right(currentPayments))
        when(mockVatReturnConnector.getSubmittedVatReturns()(any())) thenReturn Future.successful(Seq(emptyVatReturn))
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(Some("example"))))

        running(application) {
          val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

          val result = route(application, request).value
          val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            Map(period3 -> payment3),
            displayBanner = false,
            Some("example")
          )(request, messages(application)).toString
        }
      }
    }
  }
}
