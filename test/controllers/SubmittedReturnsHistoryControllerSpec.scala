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
import connectors.VatReturnConnector
import connectors.financialdata.FinancialDataConnector
import models.Period
import models.Quarter.{Q1, Q2}
import models.corrections.{CorrectionPayload, CorrectionToCountry, PeriodWithCorrections}
import models.financialdata.{Charge, VatReturnWithFinancialData}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.mockito.MockitoSugar.mock
import org.scalacheck.Arbitrary
import org.scalatest.BeforeAndAfterEach
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.VatReturnSalesService
import uk.gov.hmrc.domain.Vrn
import views.html.SubmittedReturnsHistoryView

import java.time.Instant
import scala.concurrent.Future

class SubmittedReturnsHistoryControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val vatReturnConnector = mock[VatReturnConnector]
  private val financialDataConnector = mock[FinancialDataConnector]
  private val vatReturnsSalesService = mock[VatReturnSalesService]

  private val period1 = Period(2021, Q1)
  private val period2 = Period(2021, Q2)

  private val charge = Charge(period1, BigDecimal(1000), BigDecimal(1000), BigDecimal(1000))
  private val charge2 = Charge(period2, BigDecimal(2000), BigDecimal(500), BigDecimal(1500))
  private val vatOwed = (charge.outstandingAmount * 100).toLong
  private val vatOwed2 = (charge2.outstandingAmount * 100).toLong

  private val vatReturnWithFinancialData = VatReturnWithFinancialData(completeVatReturn, Some(charge), Some(vatOwed), None)

  override def beforeEach(): Unit = {
    Mockito.reset(vatReturnConnector)
    super.beforeEach()
  }

  "SubmittedReturnsHistory Controller" - {

    "must return OK and correct view with the current period when a return for this period exists" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector)
        ).build()

      when(financialDataConnector.getVatReturnWithFinancialData(any())(any())) thenReturn Future.successful(Right(Seq(vatReturnWithFinancialData)))

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          Seq(VatReturnWithFinancialData(completeVatReturn, Some(charge), Some(vatOwed), None)),
          displayBanner = false
        )(request, messages(application)).toString
      }
    }

    "must return OK and correct view with no-returns message when no returns exist" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector)
        ).build()

      when(financialDataConnector.getVatReturnWithFinancialData(any())(any())) thenReturn Future.successful(Right(Seq.empty))

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          Seq.empty,
          displayBanner = false
        )(request, messages(application)).toString
      }
    }

    "must throw an exception when an unexpected result is returned" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector)
        ).build()

      when(financialDataConnector.getVatReturnWithFinancialData(any())(any())) thenReturn Future.failed(new Exception("Some Exception"))

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value

        whenReady(result.failed) { exp => exp mustBe a[Exception] }
      }
    }

    "must return OK and correct view when a vat return exists but no charge is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector)
        ).build()

      when(financialDataConnector.getVatReturnWithFinancialData(any())(any()))
        .thenReturn(Future.successful(Right(Seq(
          vatReturnWithFinancialData.copy(charge = None, vatOwed = Some(66666))
        ))))

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          Seq(VatReturnWithFinancialData(completeVatReturn, None, Some(66666), None)),
          displayBanner = true
        )(request, messages(application)).toString
      }
    }


    "must return OK and correct view when a nil vat return exists and no charge is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector)
        ).build()

      when(financialDataConnector.getVatReturnWithFinancialData(any())(any()))
        .thenReturn(Future.successful(Right(Seq(
          vatReturnWithFinancialData.copy(charge = None, vatOwed = Some(0))
        ))))

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          Seq(VatReturnWithFinancialData(completeVatReturn, None, Some(0), None)),
          displayBanner = false
        )(request, messages(application)).toString
      }
    }

    "must return OK and correct view with multiple periods" in {

      val completeVatReturn2 = completeVatReturn.copy(vrn = Vrn("063407445"))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector)
        ).build()

      when(financialDataConnector.getVatReturnWithFinancialData(any())(any()))
        .thenReturn(Future.successful(Right(Seq(
          vatReturnWithFinancialData,
          VatReturnWithFinancialData(completeVatReturn2, Some(charge2), Some(vatOwed2), None)
        ))))

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        val vatReturnsWithFinancialData = List(
          VatReturnWithFinancialData(completeVatReturn, Some(charge), Some(vatOwed), None),
          VatReturnWithFinancialData(completeVatReturn2, Some(charge2), Some(vatOwed2), None)
        )

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          vatReturnsWithFinancialData,
          displayBanner = false
        )(request, messages(application)).toString
      }
    }

    "must return OK and correct view when a correction exists and vat owed is not in the response" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector),
          bind[VatReturnSalesService].toInstance(vatReturnsSalesService)
        ).build()

      val completedCorrectionPayload: CorrectionPayload =
        CorrectionPayload(
          Vrn("063407423"),
          Period("2086", "Q3").get,
          List(PeriodWithCorrections(
            period,
            List(Arbitrary.arbitrary[CorrectionToCountry].sample.value)
          )),
          Instant.ofEpochSecond(1630670836),
          Instant.ofEpochSecond(1630670836)
        )
      val vatReturnWithFinancialData = VatReturnWithFinancialData(
        vatReturn = completeVatReturn,
        charge = None,
        vatOwed = None,
        corrections = Some(completedCorrectionPayload)
      )

      when(financialDataConnector.getVatReturnWithFinancialData(any())(any())) thenReturn Future.successful(Right(Seq(vatReturnWithFinancialData)))
      when(vatReturnsSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(Some(completedCorrectionPayload)))) thenReturn BigDecimal(1000)

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          Seq(VatReturnWithFinancialData(completeVatReturn, None, Some(vatOwed), Some(completedCorrectionPayload))),
          displayBanner = true
        )(request, messages(application)).toString

        verify(vatReturnsSalesService, times(2)).getTotalVatOnSalesAfterCorrection(any(), eqTo(Some(completedCorrectionPayload)))
      }
    }

    "must return OK and correct view when a correction exists" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[FinancialDataConnector].toInstance(financialDataConnector)
        ).build()

      val completedCorrectionPayload: CorrectionPayload =
        CorrectionPayload(
          Vrn("063407423"),
          Period("2086", "Q3").get,
          List(PeriodWithCorrections(
            period,
            List(Arbitrary.arbitrary[CorrectionToCountry].sample.value)
          )),
          Instant.ofEpochSecond(1630670836),
          Instant.ofEpochSecond(1630670836)
        )
      val vatReturnWithFinancialData = VatReturnWithFinancialData(
        vatReturn = completeVatReturn,
        charge = Some(charge),
        vatOwed = Some(vatOwed),
        corrections = Some(completedCorrectionPayload)
      )

      when(financialDataConnector.getVatReturnWithFinancialData(any())(any())) thenReturn Future.successful(Right(Seq(vatReturnWithFinancialData)))

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          Seq(VatReturnWithFinancialData(completeVatReturn, Some(charge), Some(vatOwed), Some(completedCorrectionPayload))),
          displayBanner = false
        )(request, messages(application)).toString
      }
    }

  }
}
