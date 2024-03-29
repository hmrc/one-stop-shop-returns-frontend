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
import forms.SoldGoodsFromNiFormProvider
import models.NormalMode
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.SoldGoodsFromNiPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.UserAnswersRepository
import services.PartialReturnPeriodService
import views.html.SoldGoodsFromNiView

import scala.concurrent.Future

class SoldGoodsFromNiControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new SoldGoodsFromNiFormProvider()
  private val form = formProvider()

  private val mockPartialReturnPeriodService = mock[PartialReturnPeriodService]

  private lazy val soldGoodsFromNiRoute = routes.SoldGoodsFromNiController.onPageLoad(NormalMode, period).url

  "SoldGoodsFromNi Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn Future.successful(None)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
        .build()

      running(application) {
        val request = FakeRequest(GET, soldGoodsFromNiRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SoldGoodsFromNiView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, period)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn Future.successful(None)

      val userAnswers = emptyUserAnswers.set(SoldGoodsFromNiPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
        .build()

      running(application) {
        val request = FakeRequest(GET, soldGoodsFromNiRoute)

        val view = application.injector.instanceOf[SoldGoodsFromNiView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, period)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[UserAnswersRepository]

      when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn Future.successful(None)
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
          .overrides(bind[UserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, soldGoodsFromNiRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(SoldGoodsFromNiPage, true).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual SoldGoodsFromNiPage.navigate(NormalMode, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn Future.successful(None)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, soldGoodsFromNiRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[SoldGoodsFromNiView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, period)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET if no existing data is found" in {

      when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn Future.successful(None)

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
        .build()

      running(application) {
        val request = FakeRequest(GET, soldGoodsFromNiRoute)
        val view    = application.injector.instanceOf[SoldGoodsFromNiView]
        val result  = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, period)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page for a POST if no existing data is found" in {

      when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn Future.successful(None)

      val mockSessionRepository = mock[UserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = None)
          .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
          .overrides(bind[UserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, soldGoodsFromNiRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(SoldGoodsFromNiPage, true).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual SoldGoodsFromNiPage.navigate(NormalMode, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(any())
      }
    }
  }
}
