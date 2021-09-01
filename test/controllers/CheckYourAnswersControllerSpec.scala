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
import org.mockito.Mockito.when
import connectors.VatReturnConnector
import models.responses.{ConflictFound, UnexpectedResponseStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{SoldGoodsFromEuPage, SoldGoodsFromNiPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import viewmodels.govuk.SummaryListFluency
import services.SalesAtVatRateService

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with MockitoSugar with SummaryListFluency with BeforeAndAfterEach {

  private val vatReturnConnector = mock[VatReturnConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(vatReturnConnector)
    super.beforeEach()
  }

  "Check Your Answers Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(period).url)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result).contains("Business name") mustBe true
        contentAsString(result).contains(registration.registeredCompanyName) mustBe true
        contentAsString(result).contains(registration.vrn.vrn) mustBe true
        contentAsString(result).contains("Sales from Northern Ireland to EU countries") mustBe true
        contentAsString(result).contains("Sales from EU countries to other EU countries") mustBe true
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(period).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  "on submit" - {

    "when the user answered all necessary data and submission of the registration succeeds" - {

      "must redirect to the next page" in {

        val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value
            .set(SoldGoodsFromEuPage, false).success.value

        val app =
          applicationBuilder(Some(answers))
            .overrides(
              bind[VatReturnConnector].toInstance(vatReturnConnector)
            )
            .build()

        when(vatReturnConnector.submit(any())(any())) thenReturn Future.successful(Right(()))

        running(app) {

          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(period).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.ReturnSubmittedController.onPageLoad(period).url
        }
      }
    }

    "when the user has already submitted a return for this period" - {

      "must redirect to Index" in {

        val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value
            .set(SoldGoodsFromEuPage, false).success.value

        val app =
          applicationBuilder(Some(answers))
            .overrides(
              bind[VatReturnConnector].toInstance(vatReturnConnector)
            )
            .build()

        when(vatReturnConnector.submit(any())(any())) thenReturn Future.successful(Left(ConflictFound))

        running(app) {

          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(period).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.IndexController.onPageLoad().url
        }
      }
    }

    "when the submission to the backend fails" - {

      "must redirect to Journey Recovery" in {

        val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value
            .set(SoldGoodsFromEuPage, false).success.value

        val app =
          applicationBuilder(Some(answers))
            .overrides(
              bind[VatReturnConnector].toInstance(vatReturnConnector)
            )
            .build()

        when(vatReturnConnector.submit(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "foo")))

        running(app) {

          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(period).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "when the user has not answered all necessary questions" - {

      "must redirect to Journey Recovery" in {

        val app =
          applicationBuilder(Some(emptyUserAnswers))
            .overrides(
              bind[VatReturnConnector].toInstance(vatReturnConnector)
            )
            .build()

        when(vatReturnConnector.submit(any())(any())) thenReturn Future.successful(Right(()))

        running(app) {

          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(period).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "must display total sales sections from eu and ni" in {
      val salesAtVatRateService = mock[SalesAtVatRateService]

      when(salesAtVatRateService.getEuTotalVatOnSales(any())).thenReturn(Some(BigDecimal(3333)))
      when(salesAtVatRateService.getEuTotalNetSales(any())).thenReturn(Some(BigDecimal(4444)))
      when(salesAtVatRateService.getNiTotalVatOnSales(any())).thenReturn(Some(BigDecimal(5555)))
      when(salesAtVatRateService.getNiTotalNetSales(any())).thenReturn(Some(BigDecimal(6666)))

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(bind[SalesAtVatRateService].toInstance(salesAtVatRateService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(period).url)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result).contains("&pound;6,666") mustBe true
        contentAsString(result).contains("&pound;5,555") mustBe true
        contentAsString(result).contains("&pound;4,444") mustBe true
        contentAsString(result).contains("&pound;3,333") mustBe true
      }
    }
  }
}
