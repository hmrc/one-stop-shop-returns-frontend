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
import forms.corrections.CountryVatCorrectionFormProvider
import models.{Country, NormalMode}
import org.scalacheck.Arbitrary.arbitrary
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, CountryVatCorrectionPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.VatReturnService
import views.html.corrections.CountryVatCorrectionView

import scala.concurrent.Future

class CountryVatCorrectionControllerSpec extends SpecBase with MockitoSugar {

  private val selectedCountry = arbitrary[Country].sample.value

  private val formProvider = new CountryVatCorrectionFormProvider()
  private val form = formProvider(selectedCountry.name)
  private val userAnswersWithCountryAndPeriod = emptyUserAnswers.set(CorrectionCountryPage(index, index), selectedCountry).success.value
    .set(CorrectionReturnPeriodPage(index), period).success.value

  private val validAnswer = BigDecimal(10)

  private lazy val countryVatCorrectionRoute = controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(NormalMode, period, index, index, false).url

  "CountryVatCorrection Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockVatReturnConnector = mock[VatReturnConnector]
      val mockService = mock[VatReturnService]
      when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(emptyVatReturn))
      when(mockService.getVatOwedToCountryOnReturn(any(), any())(any(), any())) thenReturn Future.successful(validAnswer)

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
        .overrides(
          bind[VatReturnConnector].toInstance(mockVatReturnConnector),
          bind[VatReturnService].toInstance(mockService))
        .build()

      running(application) {
        val request = FakeRequest(GET, countryVatCorrectionRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CountryVatCorrectionView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, period, selectedCountry, period, index, index, validAnswer, false)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithCountryAndPeriod.set(CountryVatCorrectionPage(index, index), validAnswer).success.value

      val mockVatReturnConnector = mock[VatReturnConnector]
      val mockService = mock[VatReturnService]
      when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(emptyVatReturn))
      when(mockService.getVatOwedToCountryOnReturn(any(), any())(any(), any())) thenReturn Future.successful(validAnswer)


      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(mockVatReturnConnector),
          bind[VatReturnService].toInstance(mockService)
        )
        .build()


      running(application) {
        val request = FakeRequest(GET, countryVatCorrectionRoute)

        val view = application.injector.instanceOf[CountryVatCorrectionView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validAnswer), NormalMode, period, selectedCountry, period, index, index, validAnswer, false)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]
      val mockVatReturnConnector = mock[VatReturnConnector]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(emptyVatReturn))

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, countryVatCorrectionRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value
        val expectedAnswers = userAnswersWithCountryAndPeriod.set(CountryVatCorrectionPage(index, index), validAnswer).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CountryVatCorrectionPage(index, index).navigate(NormalMode, expectedAnswers, false).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val mockVatReturnConnector = mock[VatReturnConnector]
      val mockService = mock[VatReturnService]
      when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(emptyVatReturn))
      when(mockService.getVatOwedToCountryOnReturn(any(), any())(any(), any())) thenReturn Future.successful(validAnswer)


      val application = applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
        .overrides(
          bind[VatReturnConnector].toInstance(mockVatReturnConnector),
          bind[VatReturnService].toInstance(mockService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, countryVatCorrectionRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[CountryVatCorrectionView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, period, selectedCountry, period, index, index, validAnswer, false)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, countryVatCorrectionRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no correction period or country found in user answers" in {

      val application = applicationBuilder(Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, countryVatCorrectionRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, countryVatCorrectionRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no correction period or country found in user answers" in {

      val application = applicationBuilder(Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, countryVatCorrectionRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }


  }
}
