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
import controllers.actions.{FakeGetRegistrationAction, GetRegistrationAction}
import models.registration.Registration
import models.responses.{ConflictFound, UnexpectedResponseStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.MockitoSugar.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{SoldGoodsFromEuPage, SoldGoodsFromNiPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.RegistrationRepository
import viewmodels.govuk.SummaryListFluency

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with MockitoSugar with SummaryListFluency with BeforeAndAfterEach {

  private val vatReturnConnector     = mock[VatReturnConnector]
  private val registrationRepository = mock[RegistrationRepository]

  override def beforeEach(): Unit = {
    Mockito.reset(vatReturnConnector, registrationRepository)
    super.beforeEach()
  }

  "Check Your Answers Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(period).url)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result).contains("Business name") mustBe true
        contentAsString(result).contains(registration.registeredCompanyName) mustBe true
        contentAsString(result).contains(registration.vrn.vrn) mustBe true
        contentAsString(result).contains("Sales from Northern Ireland to EU countries") mustBe true
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
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[RegistrationRepository].toInstance(registrationRepository)
            )
            .build()

        when(registrationRepository.get(any())) thenReturn Future.successful(Some(registration))
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
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[RegistrationRepository].toInstance(registrationRepository)
            )
            .build()

        when(registrationRepository.get(any())) thenReturn Future.successful(Some(registration))
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
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[RegistrationRepository].toInstance(registrationRepository)
            )
            .build()

        when(registrationRepository.get(any())) thenReturn Future.successful(Some(registration))
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
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[RegistrationRepository].toInstance(registrationRepository)
            )
            .build()

        when(registrationRepository.get(any())) thenReturn Future.successful(Some(registration))
        when(vatReturnConnector.submit(any())(any())) thenReturn Future.successful(Right(()))

        running(app) {

          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(period).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "when the user's registration cannot be found" - {

      "must redirect to Journey Recovery" in {

        val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value
            .set(SoldGoodsFromEuPage, false).success.value

        val app =
          applicationBuilder(Some(answers))
            .overrides(
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[RegistrationRepository].toInstance(registrationRepository)
            )
            .build()

        when(registrationRepository.get(any())) thenReturn Future.successful(None)
        when(vatReturnConnector.submit(any())(any())) thenReturn Future.successful(Right(()))

        running(app) {

          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(period).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
