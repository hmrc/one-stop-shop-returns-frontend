/*
 * Copyright 2023 HM Revenue & Customs
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
import forms.ContinueReturnFormProvider
import models.{ContinueReturn, Country}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import pages.{ContinueReturnPage, CountryOfConsumptionFromEuPage, CountryOfConsumptionFromNiPage, CountryOfSaleFromEuPage, SavedProgressPage}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.ContinueReturnView

class ContinueReturnControllerSpec extends SpecBase with MockitoSugar {

  private lazy val continueReturnRoute = routes.ContinueReturnController.onPageLoad(period).url

  private val formProvider = new ContinueReturnFormProvider()
  private val form = formProvider()

  private val baseAnswers =
    emptyUserAnswers
      .set(CountryOfConsumptionFromNiPage(index), Country("XI", "Northern Ireland")).success.value

  private val sameEuCountryAnswers =
    emptyUserAnswers
      .set(CountryOfSaleFromEuPage(index), Country("LV", "Latvia")).success.value
      .set(CountryOfConsumptionFromEuPage(index, index), Country("LV", "Latvia")).success.value

  val country: Country = arbitrary[Country].sample.value

  "ContinueReturn Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers.set(SavedProgressPage, "test").success.value)).build()

      running(application) {
        val request = FakeRequest(GET, continueReturnRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ContinueReturnView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, period)(request, messages(application)).toString
      }
    }

    "must redirect for a GET if there is no continue url saved in user answers" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, continueReturnRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.StartReturnController.onPageLoad(period).url
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, continueReturnRoute)
            .withFormUrlEncodedBody(("value", ContinueReturn.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual ContinueReturnPage.navigate(emptyUserAnswers, ContinueReturn.values.head).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, continueReturnRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[ContinueReturnView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, period)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, continueReturnRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, continueReturnRoute)
            .withFormUrlEncodedBody(("value", ContinueReturn.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return the Intercept page if user has changed OMP to NO and has a saved return containing NI to NI sales" in {

      val reg = registration.copy(isOnlineMarketplace = false)

      val application =
        applicationBuilder(
          userAnswers = Some(baseAnswers),
          registration = reg
        ).build()

      running(application) {
        val request = FakeRequest(POST, continueReturnRoute)
          .withFormUrlEncodedBody(("value", ContinueReturn.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.NiToNiInterceptController.onPageLoad(period).url
      }
    }

    "must return the Intercept page if user has changed OMP to NO and has a saved return with same Eu to EU country sales" in {

      val reg = registration.copy(isOnlineMarketplace = false)
      val hasSameEuToEu = true
      val application =
        applicationBuilder(
          userAnswers = Some(sameEuCountryAnswers),
          registration = reg
        ).build()

      running(application) {
        val request = FakeRequest(POST, continueReturnRoute)
          .withFormUrlEncodedBody(("value", ContinueReturn.values.head.toString))

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.CountryToSameCountryController.onPageLoad(period).url
      }
    }
  }
}
