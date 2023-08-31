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
import models.{CheckMode, Country, NormalMode}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages._
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.RemoveSameEuToEuService
import views.html.CountryToSameCountryView

import scala.util.Try

class CountryToSameCountryControllerSpec extends SpecBase with MockitoSugar {

  private lazy val CountryToSameCountryRoute = routes.CountryToSameCountryController.onPageLoad(period).url

  val countryFrom: Country = Country("LV", "Latvia")
  val countryTo: Country = Country("LV", "Latvia")

  private val baseAnswers = emptyUserAnswers.set(CountryOfSaleFromEuPage(index), countryFrom).success.value

  "CountryToSameCountry Controller" - {

    "#onpageload" - {

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


    }

    "#onSubmit" - {

      "when there are sales still remaining then navigate to check your answers" in {
        val mockRemoveSameEuToEuService = mock[RemoveSameEuToEuService]

        when(mockRemoveSameEuToEuService.deleteEuToSameEuCountry(any())) thenReturn Try(completeUserAnswers)

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[RemoveSameEuToEuService].toInstance(mockRemoveSameEuToEuService))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, CountryToSameCountryRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad(period).url
          verify(mockRemoveSameEuToEuService, times(1)).deleteEuToSameEuCountry(eqTo(baseAnswers))
        }
      }

      "when there are no NI or EU sales remaining then navigate to sales to NI question in normal mode" in {
        val mockRemoveSameEuToEuService = mock[RemoveSameEuToEuService]

        val updatedUserAnswers = completeUserAnswers.remove(CountryOfConsumptionFromNiPage(index)).success.value
          .remove(VatRatesFromNiPage(index)).success.value
          .remove(NetValueOfSalesFromNiPage(index, index)).success.value
          .remove(VatOnSalesFromNiPage(index, index)).success.value
          .remove(CountryOfConsumptionFromEuPage(index, index)).success.value
          .remove(VatRatesFromEuPage(index, index)).success.value
          .remove(NetValueOfSalesFromEuPage(index, index, index)).success.value
          .remove(VatOnSalesFromEuPage(index, index, index)).success.value

        when(mockRemoveSameEuToEuService.deleteEuToSameEuCountry(any())) thenReturn Try(updatedUserAnswers)

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[RemoveSameEuToEuService].toInstance(mockRemoveSameEuToEuService))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, CountryToSameCountryRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.SoldGoodsFromNiController.onPageLoad(NormalMode, period).url
          verify(mockRemoveSameEuToEuService, times(1)).deleteEuToSameEuCountry(eqTo(baseAnswers))
        }
      }

      "when there are no EU but there are NI sales remaining then navigate to sales to EU question" in {
        val mockRemoveSameEuToEuService = mock[RemoveSameEuToEuService]

        val updatedUserAnswers = completeUserAnswers.remove(CountryOfSaleFromEuPage(index)).success.value
          .remove(CountryOfConsumptionFromEuPage(index, index)).success.value
          .remove(VatRatesFromEuPage(index, index)).success.value
          .remove(NetValueOfSalesFromEuPage(index, index, index)).success.value
          .remove(VatOnSalesFromEuPage(index, index, index)).success.value

        when(mockRemoveSameEuToEuService.deleteEuToSameEuCountry(any())) thenReturn Try(updatedUserAnswers)

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[RemoveSameEuToEuService].toInstance(mockRemoveSameEuToEuService))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, CountryToSameCountryRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.SoldGoodsFromEuController.onPageLoad(CheckMode, period).url
          verify(mockRemoveSameEuToEuService, times(1)).deleteEuToSameEuCountry(eqTo(baseAnswers))
        }
      }

      "when there are no NI but there are EU sales remaining then navigate to sales to NI question in check mode" in {
        val mockRemoveSameEuToEuService = mock[RemoveSameEuToEuService]

        val updatedUserAnswers = completeUserAnswers.remove(CountryOfConsumptionFromNiPage(index)).success.value
          .remove(VatRatesFromNiPage(index)).success.value
          .remove(NetValueOfSalesFromNiPage(index, index)).success.value
          .remove(VatOnSalesFromNiPage(index, index)).success.value

        when(mockRemoveSameEuToEuService.deleteEuToSameEuCountry(any())) thenReturn Try(updatedUserAnswers)

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[RemoveSameEuToEuService].toInstance(mockRemoveSameEuToEuService))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, CountryToSameCountryRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.SoldGoodsFromNiController.onPageLoad(CheckMode, period).url
          verify(mockRemoveSameEuToEuService, times(1)).deleteEuToSameEuCountry(eqTo(baseAnswers))
        }
      }

    }

  }
}
