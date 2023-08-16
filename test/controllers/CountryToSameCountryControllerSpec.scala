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
import models.{Country, NormalMode, SalesFromCountryWithOptionalVat, SalesFromEuWithOptionalVat, VatRate, VatRateAndSalesWithOptionalVat}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import pages.CountryOfSaleFromEuPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{AllSalesFromEuQueryWithOptionalVatQuery, SalesFromEuQuery}
import repositories.UserAnswersRepository
import views.html.CountryToSameCountryView

import scala.concurrent.Future

class CountryToSameCountryControllerSpec extends SpecBase with MockitoSugar {

  private lazy val CountryToSameCountryRoute = routes.CountryToSameCountryController.onPageLoad(period).url

  val countryFrom: Country = Country("LV", "Latvia")
  val countryTo: Country = Country("LV", "Latvia")

  private val baseAnswers = emptyUserAnswers.set(CountryOfSaleFromEuPage(index), countryFrom).success.value

  "CountryToSameCountry Controller" - {

    "must return OK and the correct the intercept page when isOnlineMarketplace false" in {
      val reg = registration.copy(isOnlineMarketplace = false)
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = reg).build()

      running(application) {
        val request = FakeRequest(GET, CountryToSameCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CountryToSameCountryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(period)(request, messages(application)).toString
      }
    }

    "must redirect to the correct page when the user clicks confirm" in {

      val mockSessionRepository = mock[UserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[UserAnswersRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, CountryToSameCountryRoute)
            .withFormUrlEncodedBody(("value", countryTo.code))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.CheckYourAnswersController.onPageLoad(period).url
        verify(mockSessionRepository, times(1)).set(any())

      }

    }

  }
}
