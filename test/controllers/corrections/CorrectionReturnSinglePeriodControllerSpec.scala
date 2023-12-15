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
import forms.corrections.CorrectionReturnSinglePeriodFormProvider
import models.{Index, NormalMode, PeriodWithStatus, StandardPeriod}
import models.Quarter.Q4
import models.SubmissionStatus.Complete
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.CorrectionReturnSinglePeriodPage
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.corrections.CorrectionReturnSinglePeriodView

import scala.concurrent.Future

class CorrectionReturnSinglePeriodControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val returnStatusConnector = mock[ReturnStatusConnector]

  private val formProvider = new CorrectionReturnSinglePeriodFormProvider()
  private val form = formProvider()

  private lazy val correctionReturnSinglePeriodRoute = controllers.corrections.routes.CorrectionReturnSinglePeriodController.onPageLoad(NormalMode, period, Index(0)).url

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(returnStatusConnector)
  }

  "CorrectionReturnSinglePeriod Controller" - {

    "must return OK and the correct view for a GET with 1 returns period" in {

      when(returnStatusConnector.listStatuses(any())(any()))
        .thenReturn(Future.successful(Right(Seq(
          PeriodWithStatus(period, Complete)
        ))))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .configure("bootstrap.filters.csrf.enabled" -> false)
        .overrides(bind[ReturnStatusConnector].toInstance(returnStatusConnector))
        .build()

      running(application) {
        implicit val msgs: Messages = messages(application)
        val request = FakeRequest(GET, correctionReturnSinglePeriodRoute)
        val result = route(application, request).value
        val view = application.injector.instanceOf[CorrectionReturnSinglePeriodView]

        status(result) mustEqual OK
        contentAsString(result).mustEqual(
          view(form, NormalMode, period, period.displayText, Index(0))(request, messages(application)).toString
        )
      }
    }

    "must redirect to CorrectionReturnPeriodController for a GET with more than 1 returns period" in {

      when(returnStatusConnector.listStatuses(any())(any()))
        .thenReturn(Future.successful(Right(Seq(
          PeriodWithStatus(period, Complete),
          PeriodWithStatus(StandardPeriod(2021, Q4), Complete)
        ))))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ReturnStatusConnector].toInstance(returnStatusConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, correctionReturnSinglePeriodRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value.mustEqual(
          controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(NormalMode, period, Index(0)).url
        )
      }
    }

    "must redirect to JourneyRecoveryController for a GET with 0 previous returns periods" in {

      when(returnStatusConnector.listStatuses(any())(any())).thenReturn(Future.successful(Right(Seq.empty)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ReturnStatusConnector].toInstance(returnStatusConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, correctionReturnSinglePeriodRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value.mustEqual(
          controllers.routes.CheckYourAnswersController.onPageLoad(period).url
        )
      }
    }

    "must redirect to the next page when valid data is submitted" in {


      when(returnStatusConnector.listStatuses(any())(any()))
        .thenReturn(Future.successful(Right(Seq(
          PeriodWithStatus(period, Complete)
        ))))

      val application =
        applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(bind[ReturnStatusConnector].toInstance(returnStatusConnector))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnSinglePeriodRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value


        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value.mustEqual(
          CorrectionReturnSinglePeriodPage(Index(0)).navigate(NormalMode, completeUserAnswers, true).url
        )
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(returnStatusConnector.listStatuses(any())(any()))
        .thenReturn(Future.successful(Right(Seq(
          PeriodWithStatus(period, Complete)
        ))))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ReturnStatusConnector].toInstance(returnStatusConnector))
        .build()

      running(application) {
        implicit val msgs: Messages = messages(application)
        val request =
          FakeRequest(POST, correctionReturnSinglePeriodRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))
        val view = application.injector.instanceOf[CorrectionReturnSinglePeriodView]
        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result).mustEqual(
          view(boundForm, NormalMode, period, period.displayText, Index(0))(request, messages(application)).toString
        )
      }
    }

    "must redirect to CorrectionReturnPeriodController when invalid data is submitted and connector returns more than 1 returns period" in {

      when(returnStatusConnector.listStatuses(any())(any()))
        .thenReturn(Future.successful(Right(Seq(
          PeriodWithStatus(period, Complete),
          PeriodWithStatus(StandardPeriod(2021, Q4), Complete)
        ))))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ReturnStatusConnector].toInstance(returnStatusConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnSinglePeriodRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value.mustEqual(
          controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(NormalMode, period, Index(0)).url
        )
      }
    }

    "must redirect to JourneyRecoveryController when invalid data is submitted and connector returns more than 0 returns periods" in {

      when(returnStatusConnector.listStatuses(any())(any())).thenReturn(Future.successful(Right(Seq.empty)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ReturnStatusConnector].toInstance(returnStatusConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnSinglePeriodRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value.mustEqual(
          controllers.routes.JourneyRecoveryController.onPageLoad().url
        )
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, correctionReturnSinglePeriodRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnSinglePeriodRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
