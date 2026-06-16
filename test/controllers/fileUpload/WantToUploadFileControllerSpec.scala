/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.fileUpload

import base.SpecBase
import forms.WantToUploadFileFormProvider
import models.NormalMode
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.fileUpload.WantToUploadFilePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.UserAnswersRepository
import services.PartialReturnPeriodService
import views.html.fileUpload.WantToUploadFileView

import scala.concurrent.Future

class WantToUploadFileControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new WantToUploadFileFormProvider()
  private val form = formProvider()

  private val mockPartialReturnPeriodService = mock[PartialReturnPeriodService]

  private lazy val wantToUploadFileRoute = controllers.fileUpload.routes.WantToUploadFileController.onPageLoad(NormalMode, period).url

  "WantToUploadFile Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, wantToUploadFileRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WantToUploadFileView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, NormalMode, period)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers.set(WantToUploadFilePage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, wantToUploadFileRoute)

        val view = application.injector.instanceOf[WantToUploadFileView]

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form.fill(true), NormalMode, period)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when Yes is submitted" in {

      val mockSessionRepository = mock[UserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn Future.successful(None)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[UserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, wantToUploadFileRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(WantToUploadFilePage, true).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe WantToUploadFilePage.navigate(NormalMode, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must redirect to the next page when No is submitted" in {

      val mockSessionRepository = mock[UserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn Future.successful(None)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[UserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, wantToUploadFileRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(WantToUploadFilePage, false).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe WantToUploadFilePage.navigate(NormalMode, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, wantToUploadFileRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[WantToUploadFileView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, NormalMode, period)(request, messages(application)).toString
      }
    }
  }
}
