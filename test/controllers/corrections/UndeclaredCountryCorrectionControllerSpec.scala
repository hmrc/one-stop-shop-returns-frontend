/*
 * Copyright 2022 HM Revenue & Customs
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
import forms.corrections.UndeclaredCountryCorrectionFormProvider
import models.{Country, NormalMode}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, UndeclaredCountryCorrectionPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.corrections.UndeclaredCountryCorrectionView

import scala.concurrent.Future

class UndeclaredCountryCorrectionControllerSpec extends SpecBase with MockitoSugar {
  private val selectedCountry = arbitrary[Country].sample.value
  private val formProvider = new UndeclaredCountryCorrectionFormProvider()
  private val form = formProvider()
  private val userAnswersWithCountryAndPeriod = emptyUserAnswers.set(CorrectionCountryPage(index, index), selectedCountry).success.value
    .set(CorrectionReturnPeriodPage(index), period).success.value
  private lazy val undeclaredCountryCorrectionRoute = controllers.corrections.routes.UndeclaredCountryCorrectionController.onPageLoad(NormalMode, period, index, index).url

  "UndeclaredCountryCorrection Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockVatReturnConnector = mock[VatReturnConnector]
      when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(emptyVatReturn))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
        .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, undeclaredCountryCorrectionRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[UndeclaredCountryCorrectionView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, period, selectedCountry, period, index, index)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithCountryAndPeriod.set(UndeclaredCountryCorrectionPage(index, index), true).success.value

      val mockVatReturnConnector = mock[VatReturnConnector]
      when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(emptyVatReturn))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, undeclaredCountryCorrectionRoute)

        val view = application.injector.instanceOf[UndeclaredCountryCorrectionView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, period, selectedCountry, period, index, index)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val mockVatReturnConnector = mock[VatReturnConnector]
      when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(emptyVatReturn))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
        .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, undeclaredCountryCorrectionRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = userAnswersWithCountryAndPeriod.set(UndeclaredCountryCorrectionPage(index, index), true).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual UndeclaredCountryCorrectionPage(index, index).navigate(NormalMode, expectedAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val mockVatReturnConnector = mock[VatReturnConnector]
      when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(emptyVatReturn))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
        .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, undeclaredCountryCorrectionRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[UndeclaredCountryCorrectionView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, period, selectedCountry, period, index, index)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, undeclaredCountryCorrectionRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, undeclaredCountryCorrectionRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
