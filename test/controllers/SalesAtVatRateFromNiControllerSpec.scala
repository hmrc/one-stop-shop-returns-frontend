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
import forms.SalesAtVatRateFromNiFormProvider
import models.{Country, NormalMode, SalesAtVatRate, VatRate}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import pages.{CountryOfConsumptionFromNiPage, SalesAtVatRateFromNiPage, VatRatesFromNiPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.SalesAtVatRateFromNiView

import scala.concurrent.Future

class SalesAtVatRateFromNiControllerSpec extends SpecBase with MockitoSugar {

  private val validAnswerForNetValueOfSales: BigDecimal = 0
  private val validAnswerVatOnSales: BigDecimal = 0
  private val validAnswer: SalesAtVatRate = SalesAtVatRate(validAnswerForNetValueOfSales, validAnswerVatOnSales)

  private lazy val salesAtVatRateFromNiRoute = routes.SalesAtVatRateFromNiController.onPageLoad(NormalMode, period, index, index).url

  private val country = arbitrary[Country].sample.value
  private val vatRate = arbitrary[VatRate].sample.value
  private val baseAnswers =
    emptyUserAnswers
      .set(CountryOfConsumptionFromNiPage(index), country).success.value
      .set(VatRatesFromNiPage(index), List(vatRate)).success.value

  private val formProvider = new SalesAtVatRateFromNiFormProvider()
  private val form = formProvider(vatRate)

  "SalesAtVatRateFromNi Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, salesAtVatRateFromNiRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SalesAtVatRateFromNiView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, period, index, index, country, vatRate)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = baseAnswers.set(SalesAtVatRateFromNiPage(index, index), validAnswer).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, salesAtVatRateFromNiRoute)

        val view = application.injector.instanceOf[SalesAtVatRateFromNiView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(validAnswer),
          NormalMode,
          period,
          index,
          index,
          country,
          vatRate
        )(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, salesAtVatRateFromNiRoute)
            .withFormUrlEncodedBody(
              ("netValueOfSales", validAnswerForNetValueOfSales.toString()),
              ("vatOnSales", validAnswerVatOnSales.toString()))

        val result = route(application, request).value
        val expectedAnswers = baseAnswers.set(SalesAtVatRateFromNiPage(index, index), validAnswer).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual SalesAtVatRateFromNiPage(index, index).navigate(NormalMode, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, salesAtVatRateFromNiRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[SalesAtVatRateFromNiView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, period, index, index, country, vatRate)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, salesAtVatRateFromNiRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, salesAtVatRateFromNiRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
