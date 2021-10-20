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

package controllers

import base.SpecBase
import connectors.VatReturnConnector
import connectors.financialdata.FinancialDataConnector
import models.Period
import models.Quarter.{Q1, Q2}
import models.financialdata.{Charge, VatReturnWithFinancialData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.mockito.MockitoSugar.mock
import org.scalatest.BeforeAndAfterEach
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.SubmittedReturnsHistoryView
import models.responses.{ConflictFound, UnexpectedResponseStatus, NotFound => NotFoundResponse}
import services.{PeriodService, VatReturnSalesService}
import uk.gov.hmrc.domain.Vrn

import scala.concurrent.Future

class SubmittedReturnsHistoryControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val vatReturnConnector = mock[VatReturnConnector]
  private val financialDataConnector = mock[FinancialDataConnector]
  private val vatReturnSalesService =  mock[VatReturnSalesService]
  private val periodService = mock[PeriodService]

  private val period1 = Period(2021, Q1)
  private val period2 = Period(2021, Q2)

  private val charge = Charge(period1, BigDecimal(1000), BigDecimal(1000), BigDecimal(1000))
  private val charge2 = Charge(period2, BigDecimal(2000), BigDecimal(500), BigDecimal(1500))
  private val vatOwed = (charge.outstandingAmount * 100).toLong
  private val vatOwed2 = (charge2.outstandingAmount * 100).toLong

  private val periods = Seq(period1, period2)

  override def beforeEach(): Unit = {
    Mockito.reset(vatReturnConnector)
    super.beforeEach()
  }

  "SubmittedReturnsHistory Controller" - {

    "must return OK and correct view with the current period when a return for this period exists" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[FinancialDataConnector].toInstance(financialDataConnector),
          bind[PeriodService].toInstance(periodService)
        ).build()

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(completeVatReturn))
      when(financialDataConnector.getCharge(any())(any())) thenReturn Future.successful(Right(charge))
      when(periodService.getReturnPeriods(any())) thenReturn Seq(period1)

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          List(VatReturnWithFinancialData(Some(completeVatReturn), Some(charge), Some(vatOwed))),
          displayBanner = false
        )(request, messages(application)).toString
      }
    }

    "must return OK and correct view with no-returns message when no returns exist" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[PeriodService].toInstance(periodService)
        ).build()

      when(periodService.getReturnPeriods(any())) thenReturn Seq.empty

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          List.empty,
          displayBanner = false
        )(request, messages(application)).toString
      }
    }

    "must throw an exception when an unexpected result is returned" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector)
        ).build()

      when(vatReturnConnector.get(any())(any())) thenReturn Future.failed(new Exception("Some Exception"))

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value

        whenReady(result.failed) { exp => exp mustBe a[Exception]}
      }
    }

    "must return OK and correct view when a vat return exists but no charge is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[FinancialDataConnector].toInstance(financialDataConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService),
          bind[PeriodService].toInstance(periodService)
        ).build()

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(completeVatReturn))
      when(financialDataConnector.getCharge(any())(any()))
        .thenReturn(Future.successful(Left(UnexpectedResponseStatus(400, ""))))
      when(vatReturnSalesService.getTotalVatOnSales(any())).thenReturn(BigDecimal(666.66))
      when(periodService.getReturnPeriods(any())) thenReturn Seq(period1)

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          List(VatReturnWithFinancialData(Some(completeVatReturn), None, Some(66666))),
          displayBanner = true
        )(request, messages(application)).toString
      }
    }

    "must return OK and correct view with multiple periods" in {

      val completeVatReturn2 = completeVatReturn.copy(vrn = Vrn("063407445"))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[FinancialDataConnector].toInstance(financialDataConnector),
          bind[PeriodService].toInstance(periodService)
        ).build()

      when(vatReturnConnector.get(any())(any()))
        .thenReturn(Future.successful(Right(completeVatReturn)))
        .thenReturn(Future.successful(Right(completeVatReturn2)))
      when(financialDataConnector.getCharge(any())(any()))
        .thenReturn(Future.successful(Right(charge)))
        .thenReturn(Future.successful(Right(charge2)))
      when(periodService.getReturnPeriods(any())) thenReturn periods

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        val vatReturnsWithFinancialData = List(
          VatReturnWithFinancialData(Some(completeVatReturn), Some(charge), Some(vatOwed)),
          VatReturnWithFinancialData(Some(completeVatReturn2), Some(charge2), Some(vatOwed2))
        )

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          vatReturnsWithFinancialData,
          displayBanner = false
        )(request, messages(application)).toString
      }
    }

  }
}
