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
import connectors.VatReturnConnector
import forms.corrections.CorrectionCountryFormProvider
import models.Quarter.Q3
import models.{Country, NormalMode, Period, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.corrections.CorrectionCountryView

import scala.concurrent.Future

class CorrectionCountryControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new CorrectionCountryFormProvider()
  private val form = formProvider(index, Seq.empty)
  val country: Country = arbitrary[Country].sample.value

  private lazy val correctionCountryRoute = controllers.corrections.routes.CorrectionCountryController.onPageLoad(NormalMode, period, index, index).url

  "CorrectionCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val userAnswers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value

      val application = applicationBuilder(Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, correctionCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CorrectionCountryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, period, index, period, index)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery Controller when user calls onPageLoad and has not answered correction period question" in {

      val application = applicationBuilder(Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, correctionCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CorrectionCountryView]

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers
        .set(CorrectionReturnPeriodPage(index), period).success.value
        .set(CorrectionCountryPage(index, index), country).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, correctionCountryRoute)

        val view = application.injector.instanceOf[CorrectionCountryView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(country), NormalMode, period, index, period, index)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]
      val mockVatReturnConnector = mock[VatReturnConnector]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(emptyVatReturn))
      val expectedAnswers = emptyUserAnswers.set(CorrectionCountryPage(index, index), country).success.value
      val expectedAnswers2 = expectedAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
      val application =
        applicationBuilder(userAnswers = Some(expectedAnswers2))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, correctionCountryRoute)
            .withFormUrlEncodedBody(("value", country.code))

        val result = route(application, request).value


        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CorrectionCountryPage(index, index).navigate(NormalMode, expectedAnswers2, Seq(), Seq()).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers2))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val userAnswers = emptyUserAnswers
        .set(CorrectionReturnPeriodPage(index), period).success.value

      val application = applicationBuilder(Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, correctionCountryRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[CorrectionCountryView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, period, index, period, index)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, correctionCountryRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, correctionCountryRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
