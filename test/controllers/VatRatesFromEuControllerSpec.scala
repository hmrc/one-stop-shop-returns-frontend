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

package controllers

import base.SpecBase
import forms.VatRatesFromEuFormProvider
import models.{Country, NormalMode, VatRate}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import pages.{CountryOfConsumptionFromEuPage, CountryOfSaleFromEuPage, VatRatesFromEuPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.VatRateService
import views.html.VatRatesFromEuView

import scala.concurrent.Future

class VatRatesFromEuControllerSpec extends SpecBase with MockitoSugar with VatRateBaseController {

  private lazy val vatRatesFromEuRoute = routes.VatRatesFromEuController.onPageLoad(NormalMode, period, index, index).url

  private val vatRate      = arbitrary[VatRate].sample.value
  private val vatRate2     = arbitrary[VatRate].sample.value
  private val vatRates     = List(vatRate, vatRate2)
  private val countryFrom  = arbitrary[Country].sample.value
  private val countryTo    = arbitrary[Country].sample.value

  private val baseAnswers =
    emptyUserAnswers
      .set(CountryOfSaleFromEuPage(index), countryFrom).success.value
      .set(CountryOfConsumptionFromEuPage(index, index), countryTo).success.value

  "VatRatesFromEu Controller" - {

    "must return OK and the correct view for a GET" in {
      val vatRateService = mock[VatRateService]
      when(vatRateService.vatRates(any(), any())) thenReturn(vatRates)

      val application = applicationBuilder(userAnswers = Some(baseAnswers))
        .overrides(
          bind[VatRateService].toInstance(vatRateService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, vatRatesFromEuRoute)
        val view           = application.injector.instanceOf[VatRatesFromEuView]
        val formProvider   = application.injector.instanceOf[VatRatesFromEuFormProvider]
        val controller     = application.injector.instanceOf[VatRatesFromEuController]
        val form           = formProvider(vatRates)
        val checkboxItems  = controller.checkboxItems(vatRates)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          NormalMode,
          period,
          index,
          index,
          countryFrom,
          countryTo,
          checkboxItems
        )(request, messages(application)).toString
      }
    }

    "must skip this page (303 SEE_OTHER) if there is only 1 selection and update user answers" in {

      val mockSessionRepository = mock[SessionRepository]
      val mockVatRateService    = mock[VatRateService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockVatRateService.vatRates(any(), any())) thenReturn List(vatRate)

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[VatRateService].toInstance(mockVatRateService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(GET, vatRatesFromEuRoute)

        val result = route(application, request).value
        val expectedAnswers = baseAnswers.set(VatRatesFromEuPage(index, index), List(vatRate)).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual VatRatesFromEuPage(index, index).navigate(NormalMode, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val vatRateService = mock[VatRateService]
      when(vatRateService.vatRates(any(), any())) thenReturn(vatRates)
      val userAnswers = baseAnswers.set(VatRatesFromEuPage(index, index), vatRates).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[VatRateService].toInstance(vatRateService)
        ).build()

      running(application) {
        val request = FakeRequest(GET, vatRatesFromEuRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]
      val mockVatRateService    = mock[VatRateService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockVatRateService.vatRates(any(), any())) thenReturn vatRates

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[VatRateService].toInstance(mockVatRateService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, vatRatesFromEuRoute)
            .withFormUrlEncodedBody(("value[0]", vatRate.rate.toString), ("value[1]", vatRate2.rate.toString))

        val result = route(application, request).value
        val expectedAnswers = baseAnswers.set(VatRatesFromEuPage(index, index), vatRates).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual VatRatesFromEuPage(index, index).navigate(NormalMode, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, vatRatesFromEuRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, vatRatesFromEuRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, vatRatesFromEuRoute)
            .withFormUrlEncodedBody(("value[0]", vatRates.head.rate.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
