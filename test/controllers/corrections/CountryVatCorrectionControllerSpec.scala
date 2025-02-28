/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.corrections.CountryVatCorrectionFormProvider
import models.{Country, NormalMode}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, CountryVatCorrectionPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.corrections.PreviouslyDeclaredCorrectionAmount
import services.VatReturnService
import views.html.corrections.CountryVatCorrectionView

import scala.concurrent.Future

class CountryVatCorrectionControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockService = mock[VatReturnService]

  private val selectedCountry = arbitrary[Country].sample.value

  private val formProvider = new CountryVatCorrectionFormProvider()
  private val form = formProvider(selectedCountry.name, BigDecimal(100.0), false)
  private val userAnswersWithCountryAndPeriod =
    emptyUserAnswers.set(CorrectionCountryPage(index, index), selectedCountry).success.value
      .set(CorrectionReturnPeriodPage(index), period).success.value

  private val validAnswer = BigDecimal(10)

  private lazy val countryVatCorrectionRoute =
    controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(
      NormalMode, period, index, index, false
    ).url

  override def beforeEach(): Unit = {
    Mockito.reset(mockService)
  }

  "CountryVatCorrection Controller" - {

    "with strategic toggle off" - {

      "must return OK and the correct view for a GET" in {
        when(mockService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any())) thenReturn
          Future.successful(PreviouslyDeclaredCorrectionAmount(false, validAnswer))

        val application = applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
          .overrides(
            bind[VatReturnService].toInstance(mockService))
          .configure("features.strategic-returns.enabled" -> false)
          .build()

        running(application) {
          val request = FakeRequest(GET, countryVatCorrectionRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CountryVatCorrectionView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form, NormalMode, period, selectedCountry, period, index, index, validAnswer, undeclaredCountry = false
          )(request, messages(application)).toString
        }
      }

      "must return OK and the correct view for a GET for undeclared country" in {
        when(mockService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any())) thenReturn
          Future.successful(PreviouslyDeclaredCorrectionAmount(false, validAnswer))

        val application = applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
          .overrides(
            bind[VatReturnService].toInstance(mockService))
          .configure("features.strategic-returns.enabled" -> false)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(
              NormalMode, period, index, index, true
            ).url
          )

          val result = route(application, request).value

          val view = application.injector.instanceOf[CountryVatCorrectionView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form, NormalMode, period, selectedCountry, period, index, index, validAnswer, undeclaredCountry = true
          )(request, messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers =
          userAnswersWithCountryAndPeriod.set(CountryVatCorrectionPage(index, index), validAnswer).success.value

        when(mockService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any())) thenReturn Future.successful(PreviouslyDeclaredCorrectionAmount(false, validAnswer))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[VatReturnService].toInstance(mockService)
          )
          .configure("features.strategic-returns.enabled" -> false)
          .build()

        running(application) {
          val request = FakeRequest(GET, countryVatCorrectionRoute)

          val view = application.injector.instanceOf[CountryVatCorrectionView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill(validAnswer), NormalMode, period, selectedCountry, period, index, index, validAnswer, undeclaredCountry = false
          )(request, messages(application)).toString
        }
      }

      "must save the answer and redirect to the next page when valid data is submitted" in {

        val previouslyDeclaredCorrectionAmount = PreviouslyDeclaredCorrectionAmount(true, 10)

        when(mockService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any())) thenReturn
          Future.successful(previouslyDeclaredCorrectionAmount)

        val application =
          applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
            .overrides(bind[VatReturnService].toInstance(mockService))
            .configure("features.strategic-returns.enabled" -> false)
            .build()

        running(application) {
          val request =
            FakeRequest(POST, countryVatCorrectionRoute)
              .withFormUrlEncodedBody(("value", validAnswer.toString))

          val result = route(application, request).value
          val expectedAnswers =
            userAnswersWithCountryAndPeriod.set(CountryVatCorrectionPage(index, index), validAnswer).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual CountryVatCorrectionPage(index, index).navigate(NormalMode, expectedAnswers).url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {
        when(mockService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any())) thenReturn
          Future.successful(PreviouslyDeclaredCorrectionAmount(false, validAnswer))

        val application = applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
          .overrides(
            bind[VatReturnService].toInstance(mockService)
          )
          .configure("features.strategic-returns.enabled" -> false)
          .build()

        running(application) {
          val request =
            FakeRequest(POST, countryVatCorrectionRoute)
              .withFormUrlEncodedBody(("value", "invalid value"))

          val boundForm = form.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[CountryVatCorrectionView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(
            boundForm, NormalMode, period, selectedCountry, period, index, index, validAnswer, undeclaredCountry = false
          )(request, messages(application)).toString
        }
      }

      "must return a Bad Request and errors when the total VAT owed is negative" in {

        val previouslyDeclaredCorrectionAmount = PreviouslyDeclaredCorrectionAmount(true, 300)

        when(mockService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any())) thenReturn Future.successful(previouslyDeclaredCorrectionAmount)


        val application = applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
          .overrides(
            bind[VatReturnService].toInstance(mockService)
          )
          .configure("features.strategic-returns.enabled" -> false)
          .build()

        running(application) {
          val request =
            FakeRequest(POST, countryVatCorrectionRoute)
              .withFormUrlEncodedBody(("value", "-400.0"))

          val form = formProvider(selectedCountry.name, BigDecimal(-300.0), false)
          val boundForm = form.bind(Map("value" -> "-400.0"))

          val view = application.injector.instanceOf[CountryVatCorrectionView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST

          val responseString = contentAsString(result)
          responseString mustEqual view(
            boundForm, NormalMode, period, selectedCountry, period, index, index, BigDecimal(300), undeclaredCountry = false
          )(request, messages(application)).toString
          val doc = Jsoup.parse(responseString)

          val error = doc.getElementsByClass("govuk-error-summary__body")
          error.size() mustEqual 1
          error.get(0).text() mustEqual "The correction value cannot be less than £-300"
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .configure("features.strategic-returns.enabled" -> false)
          .build()

        running(application) {
          val request = FakeRequest(GET, countryVatCorrectionRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a GET if no correction period or country found in user answers" in {

        val application = applicationBuilder(Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> false)
          .build()

        running(application) {
          val request = FakeRequest(GET, countryVatCorrectionRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .configure("features.strategic-returns.enabled" -> false)
          .build()

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

        val application = applicationBuilder(Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> false)
          .build()

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

    "with strategic toggle on" - {

      "must return OK and the correct view for a GET" in {
        when(mockService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any())) thenReturn
          Future.successful(PreviouslyDeclaredCorrectionAmount(false, validAnswer))

        val application = applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
          .overrides(
            bind[VatReturnService].toInstance(mockService))
          .configure("features.strategic-returns.enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(GET, countryVatCorrectionRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CountryVatCorrectionView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form, NormalMode, period, selectedCountry, period, index, index, validAnswer, undeclaredCountry = false
          )(request, messages(application)).toString
        }
      }

      "must return OK and the correct view for a GET for undeclared country" in {
        when(mockService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any())) thenReturn
          Future.successful(PreviouslyDeclaredCorrectionAmount(false, validAnswer))

        val application = applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
          .overrides(
            bind[VatReturnService].toInstance(mockService))
          .configure("features.strategic-returns.enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(
              NormalMode, period, index, index, true
            ).url
          )

          val result = route(application, request).value

          val view = application.injector.instanceOf[CountryVatCorrectionView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form, NormalMode, period, selectedCountry, period, index, index, validAnswer, undeclaredCountry = true
          )(request, messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val userAnswers =
          userAnswersWithCountryAndPeriod.set(CountryVatCorrectionPage(index, index), validAnswer).success.value

        when(mockService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any())) thenReturn
          Future.successful(PreviouslyDeclaredCorrectionAmount(false, validAnswer))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[VatReturnService].toInstance(mockService)
          )
          .configure("features.strategic-returns.enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(GET, countryVatCorrectionRoute)

          val view = application.injector.instanceOf[CountryVatCorrectionView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form.fill(validAnswer), NormalMode, period, selectedCountry, period, index, index, validAnswer, undeclaredCountry = false
          )(request, messages(application)).toString
        }
      }

      "must save the answer and redirect to the next page when valid data is submitted" in {

        val previouslyDeclaredCorrectionAmount = PreviouslyDeclaredCorrectionAmount(true, 10)

        when(mockService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any())) thenReturn Future.successful(previouslyDeclaredCorrectionAmount)

        val application =
          applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
            .overrides(bind[VatReturnService].toInstance(mockService))
            .configure("features.strategic-returns.enabled" -> true)
            .build()

        running(application) {
          val request =
            FakeRequest(POST, countryVatCorrectionRoute)
              .withFormUrlEncodedBody(("value", validAnswer.toString))

          val result = route(application, request).value
          val expectedAnswers =
            userAnswersWithCountryAndPeriod.set(CountryVatCorrectionPage(index, index), validAnswer).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual CountryVatCorrectionPage(index, index).navigate(NormalMode, expectedAnswers).url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {
        when(mockService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any())) thenReturn Future.successful(PreviouslyDeclaredCorrectionAmount(false, validAnswer))

        val application = applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
          .overrides(
            bind[VatReturnService].toInstance(mockService)
          )
          .configure("features.strategic-returns.enabled" -> true)
          .build()

        running(application) {
          val request =
            FakeRequest(POST, countryVatCorrectionRoute)
              .withFormUrlEncodedBody(("value", "invalid value"))

          val boundForm = form.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[CountryVatCorrectionView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(
            boundForm, NormalMode, period, selectedCountry, period, index, index, validAnswer, undeclaredCountry = false
          )(request, messages(application)).toString
        }
      }

      "must return a Bad Request and errors when the total VAT owed is negative" in {

        val previouslyDeclaredCorrectionAmount = PreviouslyDeclaredCorrectionAmount(true, 300)

        when(mockService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any())) thenReturn Future.successful(previouslyDeclaredCorrectionAmount)

        val application = applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
          .overrides(
            bind[VatReturnService].toInstance(mockService)
          )
          .configure("features.strategic-returns.enabled" -> true)
          .build()

        running(application) {
          val request =
            FakeRequest(POST, countryVatCorrectionRoute)
              .withFormUrlEncodedBody(("value", "-400.0"))

          val form = formProvider(selectedCountry.name, BigDecimal(-300.0), false)
          val boundForm = form.bind(Map("value" -> "-400.0"))

          val view = application.injector.instanceOf[CountryVatCorrectionView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST

          val responseString = contentAsString(result)
          responseString mustEqual view(
            boundForm, NormalMode, period, selectedCountry, period, index, index, BigDecimal(300), undeclaredCountry = false
          )(request, messages(application)).toString
          val doc = Jsoup.parse(responseString)

          val error = doc.getElementsByClass("govuk-error-summary__body")
          error.size() mustEqual 1
          error.get(0).text() mustEqual "The correction value cannot be less than £-300"
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .configure("features.strategic-returns.enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(GET, countryVatCorrectionRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a GET if no correction period or country found in user answers" in {

        val application = applicationBuilder(Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(GET, countryVatCorrectionRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None)
          .configure("features.strategic-returns.enabled" -> true)
          .build()

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

        val application = applicationBuilder(Some(emptyUserAnswers))
          .configure("features.strategic-returns.enabled" -> true)
          .build()

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

}

