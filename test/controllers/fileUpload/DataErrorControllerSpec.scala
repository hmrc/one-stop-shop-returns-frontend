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
import controllers.routes
import forms.DataErrorFormProvider
import models.upscan.*
import models.NormalMode
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.fileUpload.{CsvValidationErrorsPage, DataErrorPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.UserAnswersRepository
import views.html.fileUpload.DataErrorView

import scala.concurrent.Future

class DataErrorControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new DataErrorFormProvider()
  private val form = formProvider()
  private val csvErrors: Seq[CsvError] = Seq(CsvError.BlankCell(row = 4, column = CsvColumn.C))
  private val userAnswersWithErrors = emptyUserAnswers.set(CsvValidationErrorsPage, csvErrors).success.value

  private lazy val dataErrorRoute = controllers.fileUpload.routes.DataErrorController.onPageLoad(NormalMode, period).url

  "DataError Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithErrors)).build()

      running(application) {
        val request = FakeRequest(GET, dataErrorRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DataErrorView]

        val msgs = messages(application)

        val expectedParagraphs: Seq[String] = Seq(msgs("dataError.errorMessage.blankCell.p1", "C4"))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, period, csvErrors, expectedParagraphs, false, Nil)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithErrors.set(DataErrorPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, dataErrorRoute)

        val view = application.injector.instanceOf[DataErrorView]

        val result = route(application, request).value

        val msgs = messages(application)

        val expectedParagraphs: Seq[String] = Seq(msgs("dataError.errorMessage.blankCell.p1", "C4"))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, period, csvErrors, expectedParagraphs, false, Nil)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[UserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[UserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, dataErrorRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(DataErrorPage, true).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual DataErrorPage.navigate(NormalMode, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithErrors)).build()

      running(application) {
        val request =
          FakeRequest(POST, dataErrorRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[DataErrorView]

        val result = route(application, request).value

        val msgs = messages(application)

        val expectedParagraphs: Seq[String] = Seq(msgs("dataError.errorMessage.blankCell.p1", "C4"))

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, period, csvErrors, expectedParagraphs, false, Nil)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, dataErrorRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, dataErrorRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
