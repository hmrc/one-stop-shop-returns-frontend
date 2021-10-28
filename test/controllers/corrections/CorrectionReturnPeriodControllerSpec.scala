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

package controllers.corrections

import base.SpecBase
import connectors.ReturnStatusConnector
import forms.corrections.CorrectionReturnPeriodFormProvider
import models.Quarter._
import models.SubmissionStatus.Complete
import models.{NormalMode, Period, PeriodWithStatus}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.CorrectionReturnPeriodPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import views.html.corrections.CorrectionReturnPeriodView

import scala.concurrent.Future

class CorrectionReturnPeriodControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private lazy val correctionReturnPeriodRoute = controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(NormalMode, period).url

  private val formProvider = new CorrectionReturnPeriodFormProvider()
  private val form = formProvider()

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

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
            PeriodWithStatus(Period(2021, Q3), Complete),
            PeriodWithStatus(Period(2021, Q4), Complete)
        ))))

      running(application) {
        val request = FakeRequest(GET, correctionReturnPeriodRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CorrectionReturnPeriodView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form, NormalMode, period, Seq(Period(2021, Q3), Period(2021, Q4)))(request, messages(application)
        ).toString
      }
    }

    "must redirect to CorrectionReturnSinglePeriodController when less than 2 periods" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
        .build()

      when(mockReturnStatusConnector.listStatuses(any())(any()))
        .thenReturn(Future.successful(Right(Seq(
            PeriodWithStatus(Period(2021, Q3), Complete)
        ))))

      running(application) {
        val request = FakeRequest(GET, correctionReturnPeriodRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value.mustEqual(
          controllers.corrections.routes.CorrectionReturnSinglePeriodController.onPageLoad(NormalMode, period).url
        )
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers.set(CorrectionReturnPeriodPage, Period(2021, Q3)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector)
        ).build()

      when(mockReturnStatusConnector.listStatuses(any())(any()))
        .thenReturn(Future.successful(Right(Seq(
          PeriodWithStatus(Period(2021, Q3), Complete),
          PeriodWithStatus(Period(2021, Q4), Complete)
        ))))

      running(application) {
        val request = FakeRequest(GET, correctionReturnPeriodRoute)

        val view = application.injector.instanceOf[CorrectionReturnPeriodView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(Period(2021, Q3)), NormalMode, period, Seq(Period(2021, Q3), Period(2021, Q4)))(request, messages(application)
        ).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector),
          bind[SessionRepository].toInstance(mockSessionRepository)
        ).build()

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnPeriodRoute)
            .withFormUrlEncodedBody(("value", Period(2021, Q3).toString))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(CorrectionReturnPeriodPage, Period(2021, Q3)).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CorrectionReturnPeriodPage.navigate(NormalMode, expectedAnswers).url
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
          PeriodWithStatus(Period(2021, Q3), Complete),
          PeriodWithStatus(Period(2021, Q4), Complete)
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
          boundForm, NormalMode, period, Seq(Period(2021, Q3), Period(2021, Q4)))(request, messages(application)
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
          PeriodWithStatus(Period(2021, Q3), Complete)
        ))))

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnPeriodRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value.mustEqual(
          controllers.corrections.routes.CorrectionReturnSinglePeriodController.onPageLoad(NormalMode, period).url
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
            .withFormUrlEncodedBody(("value", Period(2021, Q3).toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}