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
import models.corrections.CorrectionToCountry
import models.domain.{SalesDetails, SalesToCountry, VatRate, VatRateType, VatReturn}
import models.{Country, NormalMode, PaymentReference, ReturnReference, VatOnSales, VatOnSalesChoice}
import org.jsoup.Jsoup
import org.scalacheck.Arbitrary.arbitrary
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, CountryVatCorrectionPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.VatReturnService
import services.corrections.CorrectionService
import views.html.corrections.CountryVatCorrectionView

import java.time.Instant
import scala.concurrent.Future

class CountryVatCorrectionControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockVatReturnConnector = mock[VatReturnConnector]
  private val mockService = mock[VatReturnService]
  private val mockCorrectionService = mock[CorrectionService]
  private val mockSessionRepository = mock[SessionRepository]

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
    Mockito.reset(mockVatReturnConnector)
    Mockito.reset(mockService)
    Mockito.reset(mockCorrectionService)
    Mockito.reset(mockSessionRepository)
  }

  "CountryVatCorrection Controller" - {

    "must return OK and the correct view for a GET" in {
      when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(emptyVatReturn))
      when(mockService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any()))
        .thenReturn(Future.successful(validAnswer))

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
        contentAsString(result) mustEqual view(
          form, NormalMode, period, selectedCountry, period, index, index, validAnswer, undeclaredCountry = false
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET for undeclared country" in {
      when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(emptyVatReturn))
      when(mockService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any()))
        .thenReturn(Future.successful(validAnswer))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
        .overrides(
          bind[VatReturnConnector].toInstance(mockVatReturnConnector),
          bind[VatReturnService].toInstance(mockService))
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

      when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(emptyVatReturn))
      when(mockService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any()))
        .thenReturn(Future.successful(validAnswer))

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
        contentAsString(result) mustEqual view(
          form.fill(validAnswer), NormalMode, period, selectedCountry, period, index, index, validAnswer, undeclaredCountry = false
        )(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(emptyVatReturn))
      when(mockCorrectionService.getCorrectionsForPeriod(any())(any(), any())) thenReturn Future.successful(List.empty)

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .overrides(bind[CorrectionService].toInstance(mockCorrectionService))
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
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(emptyVatReturn))
      when(mockService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any()))
        .thenReturn(Future.successful(validAnswer))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
        .overrides(
          bind[VatReturnConnector].toInstance(mockVatReturnConnector),
          bind[VatReturnService].toInstance(mockService)
        ).build()

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
      val previousVatReturn = VatReturn(
        vrn = vrn,
        period = period,
        reference = ReturnReference(vrn, period),
        paymentReference = PaymentReference(vrn, period),
        startDate = Some(period.firstDay),
        endDate = Some(period.lastDay),
        salesFromNi = List(SalesToCountry(
          selectedCountry,
          List(SalesDetails(
            vatRate = VatRate(
              rate = BigDecimal(20.0),
              rateType = VatRateType.Standard),
            BigDecimal(1000.0),
            VatOnSales(
              choice = VatOnSalesChoice.Standard,
              amount = BigDecimal(200.0)
            )))
        )),
        salesFromEu = List.empty,
        submissionReceived = Instant.now(),
        lastUpdated = Instant.now()
      )

      val previousCorrection = CorrectionToCountry(
        selectedCountry,
        BigDecimal(100.0)
      )

      when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(previousVatReturn))
      when(mockCorrectionService.getCorrectionsForPeriod(any())(any(), any()))
        .thenReturn(Future.successful(List(previousCorrection)))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
        .overrides(
          bind[VatReturnConnector].toInstance(mockVatReturnConnector),
          bind[CorrectionService].toInstance(mockCorrectionService)
        )
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
