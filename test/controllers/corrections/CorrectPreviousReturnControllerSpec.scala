/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.corrections

import base.SpecBase
import connectors.ReturnStatusConnector
import forms.corrections.CorrectPreviousReturnFormProvider
import models.{NormalMode, PeriodWithStatus, StandardPeriod}
import models.Quarter.{Q3, Q4}
import models.SubmissionStatus.Complete
import models.responses.UnexpectedResponseStatus
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.CorrectPreviousReturnPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.UserAnswersRepository
import views.html.corrections.CorrectPreviousReturnView

import scala.concurrent.Future

class CorrectPreviousReturnControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val formProvider = new CorrectPreviousReturnFormProvider()
  private val form = formProvider()

  private val mockReturnStatusConnector = mock[ReturnStatusConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockReturnStatusConnector)
  }

  private lazy val correctPreviousReturnRoute = controllers.corrections.routes.CorrectPreviousReturnController.onPageLoad(NormalMode, period).url

  "CorrectPreviousReturn Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, correctPreviousReturnRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CorrectPreviousReturnView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, period, None)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers.set(CorrectPreviousReturnPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, correctPreviousReturnRoute)

        val view = application.injector.instanceOf[CorrectPreviousReturnView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, period, None)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[UserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[UserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
          .build()

      val periods = Seq(
                              PeriodWithStatus(StandardPeriod(2021, Q3), Complete),
                              PeriodWithStatus(StandardPeriod(2021, Q4), Complete)
                            )

      when(mockReturnStatusConnector.listStatuses(any())(any()))
        .thenReturn(Future.successful(Right(periods)))

      running(application) {
        val request =
          FakeRequest(POST, correctPreviousReturnRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(CorrectPreviousReturnPage, true).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CorrectPreviousReturnPage.navigate(NormalMode, expectedAnswers, periods.size).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, correctPreviousReturnRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[CorrectPreviousReturnView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, period, None)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, correctPreviousReturnRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, correctPreviousReturnRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST when the Return Status Connector returns an Unexpected Response Status" in {

      val mockSessionRepository = mock[UserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val expectedAnswers = emptyUserAnswers.set(CorrectPreviousReturnPage, true).success.value

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[UserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
          .build()

      when(mockReturnStatusConnector.listStatuses(any())(any()))
        .thenReturn(Future.successful(Left(UnexpectedResponseStatus(1, "Some Status"))))

      running(application) {
        val request =
          FakeRequest(POST, correctPreviousReturnRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }
  }
}
