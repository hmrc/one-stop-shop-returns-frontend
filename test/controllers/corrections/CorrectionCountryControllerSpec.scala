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
import config.FrontendAppConfig
import connectors.VatReturnConnector
import forms.corrections.CorrectionCountryFormProvider
import models.responses.UnexpectedResponseStatus
import models.{Country, Index, NormalMode}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, CountryVatCorrectionPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.corrections.{PreviouslyDeclaredCorrectionAmount, PreviouslyDeclaredCorrectionAmountQuery}
import repositories.UserAnswersRepository
import services.VatReturnService
import services.corrections.CorrectionService
import views.html.corrections.CorrectionCountryView

import scala.concurrent.Future

class CorrectionCountryControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new CorrectionCountryFormProvider()
  private val form = formProvider(index, Seq.empty)
  private val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  val country: Country = arbitrary[Country].sample.value
  val country2: Country = arbitrary[Country].retryUntil(_ != country).sample.value
  val strategicReturnsEnabled: Boolean = mockFrontendAppConfig.strategicReturnApiEnabled

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

    "must return OK and the correct view for a GET when the questions have previously been answered" in {

      val userAnswers = emptyUserAnswers
        .set(CorrectionReturnPeriodPage(index), period).success.value
        .set(CorrectionCountryPage(index, index), country).success.value
        .set(CountryVatCorrectionPage(index, index), BigDecimal(10)).success.value

      val application = applicationBuilder(Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.corrections.routes.CorrectionCountryController.onPageLoad(NormalMode, period, index, Index(1)).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CorrectionCountryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, period, index, period, Index(1))(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery Controller when user calls onPageLoad and has not answered correction period question" in {

      val application = applicationBuilder(Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, correctionCountryRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers
        .set(CorrectionReturnPeriodPage(index), period).success.value
        .set(CorrectionCountryPage(index, index), country).success.value

      val mockService = mock[CorrectionService]
      when(mockService.getCorrectionsForPeriod(any())(any(), any())).thenReturn(Future.successful(List.empty))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[CorrectionService].toInstance(mockService))
        .build()

      running(application) {
        val request = FakeRequest(GET, correctionCountryRoute)

        val view = application.injector.instanceOf[CorrectionCountryView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(country), NormalMode, period, index, period, index)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[UserAnswersRepository]
      val mockService = mock[CorrectionService]
      val mockVatReturnService = mock[VatReturnService]

      when(mockService.getCorrectionsForPeriod(any())(any(), any())).thenReturn(Future.successful(List.empty))
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockVatReturnService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any())) thenReturn Future.successful(PreviouslyDeclaredCorrectionAmount(false, BigDecimal(1000)))

      val expectedAnswers = emptyUserAnswers.set(CorrectionCountryPage(index, index), country).success.value
      val expectedAnswers2 = expectedAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
      val previouslyDeclaredCorrectionAmount = PreviouslyDeclaredCorrectionAmount(previouslyDeclared = false, amount = 1000)
      val expectedAnswers3 = expectedAnswers2.set(
        PreviouslyDeclaredCorrectionAmountQuery(index, index),
        previouslyDeclaredCorrectionAmount
      ).success.value

      val application =
        applicationBuilder(userAnswers = Some(expectedAnswers3))
          .overrides(bind[UserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[CorrectionService].toInstance(mockService))
          .overrides(bind[VatReturnService].toInstance(mockVatReturnService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, correctionCountryRoute)
            .withFormUrlEncodedBody(("value", country.code))

        val result = route(application, request).value


        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CorrectionCountryPage(index, index).navigate(NormalMode, expectedAnswers3).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers3))
      }
    }
    
    "must save the answer and redirect to the next page when valid data is submitted and corrected vat return is not empty" in {

      val country = completeVatReturn.salesFromNi.map(_.countryOfConsumption).head
      val mockSessionRepository = mock[UserAnswersRepository]
      val mockService = mock[CorrectionService]
      val mockVatReturnService = mock[VatReturnService]

      when(mockService.getCorrectionsForPeriod(any())(any(), any())).thenReturn(Future.successful(List.empty))
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockVatReturnService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any())) thenReturn
        Future.successful(PreviouslyDeclaredCorrectionAmount(false, BigDecimal(1000)))

      val expectedAnswers = emptyUserAnswers.set(CorrectionCountryPage(index, index), country).success.value
      val expectedAnswers2 = expectedAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
      val previouslyDeclaredCorrectionAmount = PreviouslyDeclaredCorrectionAmount(previouslyDeclared = false, amount = 1000)
      val expectedAnswers3 = expectedAnswers2.set(
        PreviouslyDeclaredCorrectionAmountQuery(index, index),
        previouslyDeclaredCorrectionAmount
      ).success.value
      val application =
        applicationBuilder(userAnswers = Some(expectedAnswers3))
          .overrides(bind[UserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[CorrectionService].toInstance(mockService))
          .overrides(bind[VatReturnService].toInstance(mockVatReturnService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, correctionCountryRoute)
            .withFormUrlEncodedBody(("value", country.code))

        val result = route(application, request).value


        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CorrectionCountryPage(index, index).navigate(NormalMode, expectedAnswers3).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers3))
      }
    }
    

    "must save the answer and redirect to the next page when valid data is submitted when the questions have been answered" in {

      val mockSessionRepository = mock[UserAnswersRepository]
      val mockService = mock[CorrectionService]
      val mockVatReturnService = mock[VatReturnService]

      when(mockService.getCorrectionsForPeriod(any())(any(), any())).thenReturn(Future.successful(List.empty))
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockVatReturnService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any())) thenReturn
        Future.successful(PreviouslyDeclaredCorrectionAmount(false, BigDecimal(1000)))
      val previouslyDeclaredCorrectionAmount = PreviouslyDeclaredCorrectionAmount(previouslyDeclared = false, amount = 1000)
      val expectedAnswers2 = emptyUserAnswers
        .set(CorrectionCountryPage(index, index), country).success.value
        .set(CorrectionReturnPeriodPage(index), period).success.value
        .set(CountryVatCorrectionPage(index, index), BigDecimal(10)).success.value
        .set(CorrectionCountryPage(index, Index(1)), country2).success.value
        .set(PreviouslyDeclaredCorrectionAmountQuery(index, Index(1)), previouslyDeclaredCorrectionAmount).success.value

      val application =
        applicationBuilder(userAnswers = Some(expectedAnswers2))
          .overrides(bind[UserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[CorrectionService].toInstance(mockService))
          .overrides(bind[VatReturnService].toInstance(mockVatReturnService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, controllers.corrections.routes.CorrectionCountryController.onPageLoad(NormalMode, period, index, Index(1)).url)
            .withFormUrlEncodedBody(("value", country2.code))

        val result = route(application, request).value


        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CorrectionCountryPage(index, Index(1)).navigate(NormalMode, expectedAnswers2).url
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

    "must redirect to Journey Recovery when invalid data is submitted and no correction return period is found" in {
      val application = applicationBuilder(Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, correctionCountryRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
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

    "must throw exception when VatReturnConnector returns an Error Response" in {

      val mockSessionRepository = mock[UserAnswersRepository]
      val mockVatReturnConnector = mock[VatReturnConnector]
      val mockService = mock[CorrectionService]

      when(mockService.getCorrectionsForPeriod(any())(any(), any())).thenReturn(Future.successful(List.empty))
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(123, "Error 123")))
      val expectedAnswers = emptyUserAnswers.set(CorrectionCountryPage(index, index), country).success.value
      val expectedAnswers2 = expectedAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
      val application =
        applicationBuilder(userAnswers = Some(expectedAnswers2))
          .overrides(bind[UserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .overrides(bind[CorrectionService].toInstance(mockService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, correctionCountryRoute)
            .withFormUrlEncodedBody(("value", country.code))

        val result = route(application, request).value
        whenReady(result.failed) { exp => exp mustBe a[Exception] }
      }
    }

    "must redirect to Journey Recovery when valid data is submitted and Correction Period is not provided" in {

      val mockSessionRepository = mock[UserAnswersRepository]
      val mockVatReturnConnector = mock[VatReturnConnector]
      val mockService = mock[CorrectionService]

      when(mockService.getCorrectionsForPeriod(any())(any(), any())).thenReturn(Future.successful(List.empty))
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(emptyVatReturn))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[UserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .overrides(bind[CorrectionService].toInstance(mockService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, correctionCountryRoute)
            .withFormUrlEncodedBody(("value", country.code))

        val result = route(application, request).value


        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
