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

package controllers

import base.SpecBase
import forms.VatOnSalesFromNiFormProvider
import models.{Country, NormalMode, VatOnSales, VatOnSalesChoice, VatRate}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import pages.{CountryOfConsumptionFromNiPage, NetValueOfSalesFromNiPage, VatOnSalesFromNiPage, VatRatesFromNiPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.VatRateService
import views.html.VatOnSalesFromNiView

import scala.concurrent.Future

class VatOnSalesFromNiControllerSpec extends SpecBase with MockitoSugar {

  private val country = arbitrary[Country].sample.value
  private val vatRate = arbitrary[VatRate].sample.value
  private val netSales = Gen.choose[BigDecimal](0, 100000).sample.value

  private val standardVatOnSales = BigDecimal(1)

  private val mockVatRateService = mock[VatRateService]
  when(mockVatRateService.standardVatOnSales(any(), any())) thenReturn standardVatOnSales

  private val baseAnswers =
    emptyUserAnswers
      .set(CountryOfConsumptionFromNiPage(index), country).success.value
      .set(VatRatesFromNiPage(index), List(vatRate)).success.value
      .set(NetValueOfSalesFromNiPage(index, index), netSales).success.value

  private val formProvider = new VatOnSalesFromNiFormProvider(mockVatRateService)
  private val form = formProvider(vatRate, netSales)

  private val validAnswer = VatOnSales(VatOnSalesChoice.Standard, 1)

  private lazy val vatOnSalesFromNiRoute = routes.VatOnSalesFromNiController.onPageLoad(NormalMode, period, index, index).url

  "VatOnSalesFromNi Controller" - {

    "must return OK and the correct view for a GET" in {

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[VatRateService].toInstance(mockVatRateService))
          .build()

      running(application) {
        val request = FakeRequest(GET, vatOnSalesFromNiRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[VatOnSalesFromNiView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          NormalMode,
          period,
          index,
          index,
          country,
          vatRate,
          netSales,
          standardVatOnSales
        )(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = baseAnswers.set(VatOnSalesFromNiPage(index, index), validAnswer).success.value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[VatRateService].toInstance(mockVatRateService))
          .build()

      running(application) {
        val request = FakeRequest(GET, vatOnSalesFromNiRoute)

        val view = application.injector.instanceOf[VatOnSalesFromNiView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(validAnswer),
          NormalMode,
          period,
          index,
          index,
          country,
          vatRate,
          netSales,
          standardVatOnSales
        )(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[VatRateService].toInstance(mockVatRateService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, vatOnSalesFromNiRoute)
            .withFormUrlEncodedBody(("choice", VatOnSalesChoice.Standard.toString))

        val result = route(application, request).value
        val expectedAnswers = baseAnswers.set(VatOnSalesFromNiPage(index, index), validAnswer).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual VatOnSalesFromNiPage(index, index).navigate(NormalMode, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[VatRateService].toInstance(mockVatRateService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, vatOnSalesFromNiRoute)
            .withFormUrlEncodedBody(("choice", "invalid value"))

        val boundForm = form.bind(Map("choice" -> "invalid value"))

        val view = application.injector.instanceOf[VatOnSalesFromNiView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          NormalMode,
          period,
          index,
          index,
          country,
          vatRate,
          netSales,
          standardVatOnSales
        )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, vatOnSalesFromNiRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, vatOnSalesFromNiRoute)
            .withFormUrlEncodedBody(("choice", VatOnSalesChoice.Standard.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
