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
import forms.CountryOfConsumptionFromEuFormProvider
import models.{Country, NormalMode}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{CountryOfConsumptionFromEuPage, CountryOfSaleFromEuPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem
import views.html.CountryOfConsumptionFromEuView

import scala.concurrent.Future

class CountryOfConsumptionFromEuControllerSpec extends SpecBase with MockitoSugar {

  val countryFrom: Country = Country("LV", "Latvia")
  val countryTo: Country   = Country("NL", "Netherlands")
  val selectItems: Seq[SelectItem] = Country.selectItems(Country.euCountriesWithNI.filterNot(_ == countryFrom))

  private val formProvider = new CountryOfConsumptionFromEuFormProvider()
  private val form         = formProvider(index, Seq.empty, countryFrom, false)
  private val baseAnswers  = emptyUserAnswers.set(CountryOfSaleFromEuPage(index), countryFrom).success.value

  private lazy val countryOfConsumptionFromEuRoute = routes.CountryOfConsumptionFromEuController.onPageLoad(NormalMode, period, index, index).url

  "CountryOfConsumptionFromEu Controller" - {

    "must return OK and the correct view for a GET and isOnlineMarketplace false" in {

      val form         = formProvider(index, Seq.empty, countryFrom, false)
      val selectItems  = Country.selectItems(Country.euCountriesWithNI.filterNot(_ == countryFrom))
      val reg = registration.copy(isOnlineMarketplace = false)

      val application =
        applicationBuilder(
          userAnswers = Some(baseAnswers),
          registration = reg
        ).build()

      running(application) {
        val request = FakeRequest(GET, countryOfConsumptionFromEuRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CountryOfConsumptionFromEuView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, period, index, index, countryFrom, selectItems)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET and isOnlineMarketplace true" in {

      val form        = formProvider(index, Seq.empty, countryFrom, false)
      val selectItems = Country.selectItems(Country.euCountriesWithNI)
      val reg = registration.copy(isOnlineMarketplace = true)

      val application =
        applicationBuilder(
          userAnswers = Some(baseAnswers),
          registration = reg
        ).build()

      running(application) {
        val request = FakeRequest(GET, countryOfConsumptionFromEuRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CountryOfConsumptionFromEuView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, period, index, index, countryFrom, selectItems)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = baseAnswers.set(CountryOfConsumptionFromEuPage(index, index), countryTo).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, countryOfConsumptionFromEuRoute)

        val view = application.injector.instanceOf[CountryOfConsumptionFromEuView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(countryTo), NormalMode, period, index, index, countryFrom, selectItems)(request, messages(application)).toString
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
          FakeRequest(POST, countryOfConsumptionFromEuRoute)
            .withFormUrlEncodedBody(("value", countryTo.code))

        val result = route(application, request).value
        val expectedAnswers = baseAnswers.set(CountryOfConsumptionFromEuPage(index, index), countryTo).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CountryOfConsumptionFromEuPage(index, index).navigate(NormalMode, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, countryOfConsumptionFromEuRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[CountryOfConsumptionFromEuView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, period, index, index, countryFrom, selectItems)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, countryOfConsumptionFromEuRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, countryOfConsumptionFromEuRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
