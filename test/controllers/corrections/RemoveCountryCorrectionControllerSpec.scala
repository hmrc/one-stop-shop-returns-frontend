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
import forms.corrections.RemoveCountryCorrectionFormProvider
import models.{Country, Index, NormalMode, Period}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, CountryVatCorrectionPage, RemoveCountryCorrectionPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.corrections.RemoveCountryCorrectionView

import scala.concurrent.Future

class RemoveCountryCorrectionControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new RemoveCountryCorrectionFormProvider()
  private val form = formProvider()

  private lazy val removeCountryCorrectionRoute = controllers.corrections.routes.RemoveCountryCorrectionController.onPageLoad(NormalMode, period, index, index).url

  "RemoveCountryCorrection Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removeCountryCorrectionRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemoveCountryCorrectionView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, period, index, index)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers.set(RemoveCountryCorrectionPage(index), true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removeCountryCorrectionRoute)

        val view = application.injector.instanceOf[RemoveCountryCorrectionView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, period, index, index)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when true is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val answers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
        .set(CorrectionCountryPage(index, index), Country("DE", "Germany")).success.value
        .set(CountryVatCorrectionPage(index, index), BigDecimal(10)).success.value
        .set(CorrectionCountryPage(index, Index(1)), Country("BE", "Belgium")).success.value
        .set(CountryVatCorrectionPage(index, Index(1)), BigDecimal(10)).success.value

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeCountryCorrectionRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
          .set(CorrectionCountryPage(index, index), Country("BE", "Belgium")).success.value
          .set(CountryVatCorrectionPage(index, index), BigDecimal(10)).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual RemoveCountryCorrectionPage(index).navigate(NormalMode, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must save the answer and redirect to VatCorrectionsList page when true is submitted and there are already corrections" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val answers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
        .set(CorrectionCountryPage(index, index), Country("DE", "Germany")).success.value
        .set(CountryVatCorrectionPage(index, index), BigDecimal(10)).success.value
        .set(CorrectionCountryPage(index, Index(1)), Country("BE", "Belgium")).success.value
        .set(CountryVatCorrectionPage(index, Index(1)), BigDecimal(10)).success.value
        .set(CorrectionReturnPeriodPage(Index(1)), period).success.value
        .set(CorrectionCountryPage(Index(1), index), Country("ES", "Spain")).success.value
        .set(CountryVatCorrectionPage(Index(1), index), BigDecimal(10)).success.value

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeCountryCorrectionRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
          .set(CorrectionCountryPage(index, index), Country("BE", "Belgium")).success.value
          .set(CountryVatCorrectionPage(index, index), BigDecimal(10)).success.value
          .set(CorrectionReturnPeriodPage(Index(1)), period).success.value
          .set(CorrectionCountryPage(Index(1), index), Country("ES", "Spain")).success.value
          .set(CountryVatCorrectionPage(Index(1), index), BigDecimal(10)).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual RemoveCountryCorrectionPage(index).navigate(NormalMode, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must save the answer and redirect to VatPeriodCorrectionsList page when true is submitted and there are multiple periods" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val answers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
        .set(CorrectionCountryPage(index, index), Country("DE", "Germany")).success.value
        .set(CountryVatCorrectionPage(index, index), BigDecimal(10)).success.value
        .set(CorrectionReturnPeriodPage(Index(1)), period).success.value
        .set(CorrectionCountryPage(Index(1), index), Country("ES", "Spain")).success.value
        .set(CountryVatCorrectionPage(Index(1), index), BigDecimal(10)).success.value

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeCountryCorrectionRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
          .set(CorrectionCountryPage(index, index), Country("ES", "Spain")).success.value
          .set(CountryVatCorrectionPage(index, index), BigDecimal(10)).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual RemoveCountryCorrectionPage(index).navigate(NormalMode, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must save the answer and redirect to CorrectPreviousReturn page when true is submitted with a single correction in a single period" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val answers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
        .set(CorrectionCountryPage(index, index), Country("DE", "Germany")).success.value
        .set(CountryVatCorrectionPage(index, index), BigDecimal(10)).success.value

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeCountryCorrectionRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual RemoveCountryCorrectionPage(index).navigate(NormalMode, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must not change the answer and redirect to the next page when false is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val answers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
        .set(CorrectionCountryPage(index, index), Country("DE", "Germany")).success.value
        .set(CountryVatCorrectionPage(index, index), BigDecimal(10)).success.value

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeCountryCorrectionRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value
        val expectedAnswers = answers

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual RemoveCountryCorrectionPage(index).navigate(NormalMode, expectedAnswers).url
        verify(mockSessionRepository, never()).set(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, removeCountryCorrectionRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[RemoveCountryCorrectionView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, period, index, index)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, removeCountryCorrectionRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, removeCountryCorrectionRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
