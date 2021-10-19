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
import models.financialdata.Charge
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
import services.VatReturnSalesService

import scala.concurrent.Future

class SubmittedReturnsHistoryControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val vatReturnConnector = mock[VatReturnConnector]
  private val financialDataConnector = mock[FinancialDataConnector]
  private val vatReturnSalesService =  mock[VatReturnSalesService]

  private val charge = Charge(period, BigDecimal(1000), BigDecimal(1000), BigDecimal(1000))
  private val vatOwed = (charge.outstandingAmount * 100).toLong

  override def beforeEach(): Unit = {
    Mockito.reset(vatReturnConnector)
    super.beforeEach()
  }

  "SubmittedReturnsHistory Controller" - {

    "must return OK and correct view with the current period when a return for this period exists" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[FinancialDataConnector].toInstance(financialDataConnector)
        ).build()

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(completeVatReturn))
      when(financialDataConnector.getCharge(any())(any())) thenReturn Future.successful(Right(charge))

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(Some(completeVatReturn), Some(charge), Some(vatOwed))(request, messages(application)).toString
      }
    }

    "must return OK and correct view with no-returns message when no returns exist" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector)
        ).build()

      when(financialDataConnector.getCharge(any())(any())) thenReturn Future.successful(Right(charge))
      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFoundResponse))

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(None, None, None)(request, messages(application)).toString
      }
    }

    "must return redirect to Journey Controller when an unexpected result is returned" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector)
        ).build()

      when(financialDataConnector.getCharge(any())(any())) thenReturn Future.successful(Right(charge))
      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(ConflictFound))

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return OK and correct view when a vat return exists but no charge is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[FinancialDataConnector].toInstance(financialDataConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService)
        ).build()

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(completeVatReturn))
      when(financialDataConnector.getCharge(any())(any()))
        .thenReturn(Future.successful(Left(UnexpectedResponseStatus(400, ""))))
      when(vatReturnSalesService.getTotalVatOnSales(any())).thenReturn(BigDecimal(666.66))

      running(application) {
        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(Some(completeVatReturn), None, Some(66666))(request, messages(application)).toString
      }
    }
  }
}
