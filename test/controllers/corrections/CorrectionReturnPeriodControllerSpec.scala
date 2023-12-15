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
import forms.corrections.CorrectionReturnPeriodFormProvider
import models.{Index, NormalMode, PeriodWithStatus, StandardPeriod, SubmissionStatus}
import models.Quarter._
import models.SubmissionStatus.Complete
import models.responses.UnexpectedResponseStatus
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.CorrectionReturnPeriodPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.UserAnswersRepository
import views.html.corrections.CorrectionReturnPeriodView

import scala.concurrent.Future

class CorrectionReturnPeriodControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private lazy val correctionReturnPeriodRoute = controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(NormalMode, period, Index(0)).url

  private val formProvider = new CorrectionReturnPeriodFormProvider()
  private val form = formProvider(index, testPeriodsList, Seq.empty)

  private val mockReturnStatusConnector = mock[ReturnStatusConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockReturnStatusConnector)
  }

  "CorrectionReturnPeriod Controller" - {

    "must return OK and the correct view for a GET with multiple completed returns" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
        .build()

      when(mockReturnStatusConnector.listStatuses(any())(any()))
        .thenReturn(Future.successful(Right(Seq(
          PeriodWithStatus(StandardPeriod(2021, Q3), Complete),
          PeriodWithStatus(StandardPeriod(2021, Q4), Complete)
        ))))

      running(application) {
        val request = FakeRequest(GET, correctionReturnPeriodRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CorrectionReturnPeriodView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form, NormalMode, period, Seq(StandardPeriod(2021, Q3), StandardPeriod(2021, Q4)), index)(request, messages(application)
        ).toString
      }
    }

    "must redirect to CorrectionReturnSinglePeriodController when less than 2 periods" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
        .build()

      when(mockReturnStatusConnector.listStatuses(any())(any()))
        .thenReturn(Future.successful(Right(Seq(
          PeriodWithStatus(StandardPeriod(2021, Q3), Complete)
        ))))

      running(application) {
        val request = FakeRequest(GET, correctionReturnPeriodRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value.mustEqual(
          controllers.corrections.routes.CorrectionReturnSinglePeriodController.onPageLoad(NormalMode, period, Index(0)).url
        )
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers.set(CorrectionReturnPeriodPage(Index(0)), StandardPeriod(2021, Q3)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector)
        ).build()

      when(mockReturnStatusConnector.listStatuses(any())(any()))
        .thenReturn(Future.successful(Right(Seq(
          PeriodWithStatus(StandardPeriod(2021, Q3), Complete),
          PeriodWithStatus(StandardPeriod(2021, Q4), Complete)
        ))))

      running(application) {
        val request = FakeRequest(GET, correctionReturnPeriodRoute)

        val view = application.injector.instanceOf[CorrectionReturnPeriodView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(StandardPeriod(2021, Q3)), NormalMode, period, Seq(StandardPeriod(2021, Q3), StandardPeriod(2021, Q4)), index)(request, messages(application)
        ).toString
      }
    }

    "must throw and Exception for a GET when Return Status Connector returns an error" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
        .build()

      when(mockReturnStatusConnector.listStatuses(any())(any()))
        .thenReturn(Future.successful(Left(UnexpectedResponseStatus(1, "Some error"))))

      running(application) {
        val request = FakeRequest(GET, correctionReturnPeriodRoute)

        val result = route(application, request).value

        whenReady(result.failed) { exp => exp mustBe a[Exception] }
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[UserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockReturnStatusConnector.listStatuses(any())(any())) thenReturn (Future.successful(Right(Seq(PeriodWithStatus(StandardPeriod(2021, Q3), SubmissionStatus.Complete)))))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector),
          bind[UserAnswersRepository].toInstance(mockSessionRepository)
        ).build()

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnPeriodRoute)
            .withFormUrlEncodedBody(("value", StandardPeriod(2021, Q3).toString))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(CorrectionReturnPeriodPage(Index(0)), StandardPeriod(2021, Q3)).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CorrectionReturnPeriodPage(Index(0)).navigate(NormalMode, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted with multiple previous returns" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector)
        ).build()

      when(mockReturnStatusConnector.listStatuses(any())(any()))
        .thenReturn(Future.successful(Right(Seq(
          PeriodWithStatus(StandardPeriod(2021, Q3), Complete),
          PeriodWithStatus(StandardPeriod(2021, Q4), Complete)
        ))))

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnPeriodRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[CorrectionReturnPeriodView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm, NormalMode, period, Seq(StandardPeriod(2021, Q3), StandardPeriod(2021, Q4)), index)(request, messages(application)
        ).toString
      }
    }

    "must redirect to CorrectionReturnSinglePeriodController when invalid data is submitted with < 2 previous returns" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector)
        ).build()

      when(mockReturnStatusConnector.listStatuses(any())(any()))
        .thenReturn(Future.successful(Right(Seq(
          PeriodWithStatus(StandardPeriod(2021, Q3), Complete)
        ))))

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnPeriodRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value.mustEqual(
          controllers.corrections.routes.CorrectionReturnSinglePeriodController.onPageLoad(NormalMode, period, Index(0)).url
        )
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, correctionReturnPeriodRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnPeriodRoute)
            .withFormUrlEncodedBody(("value", StandardPeriod(2021, Q3).toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must throw and Exception for a POST when Return Status Connector returns an error" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
        .build()

      when(mockReturnStatusConnector.listStatuses(any())(any()))
        .thenReturn(Future.successful(Left(UnexpectedResponseStatus(1, "Some error"))))

      running(application) {
        val request = FakeRequest(POST, correctionReturnPeriodRoute)

        val result = route(application, request).value

        whenReady(result.failed) { exp => exp mustBe a[Exception] }
      }
    }
  }
}
