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
import models.{Country, NormalMode}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.UserAnswersRepository
import views.html.NiToNiInterceptView
import play.api.inject.bind
import queries.SalesFromNiQuery

import scala.concurrent.Future

class NiToNiInterceptControllerSpec extends SpecBase with MockitoSugar {

  private lazy val NiToNiInterceptRoute = routes.NiToNiInterceptController.onPageLoad(period).url

  val countryFrom: Country = Country("LV", "Latvia")
  val countryTo: Country = Country("LV", "Latvia")

  private val salesFromNiEuAnswers = completeUserAnswers
    .set(CountryOfSaleFromEuPage(index), countryFrom).success.value
    .set(CountryOfConsumptionFromNiPage(index), Country("XI", "Northern Ireland")).success.value

  private val baseAnswers =
    completeSalesFromNIUserAnswers
      .set(CountryOfConsumptionFromNiPage(index), Country("XI", "Northern Ireland")).success.value

  "NiToNiIntercept Controller" - {

    "#onpageload" - {

      "must return OK and nd the correct the intercept page when isOnlineMarketplace false" in {
        val reg = registration.copy(isOnlineMarketplace = false)
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = reg).build()

        running(application) {
          val request = FakeRequest(GET, NiToNiInterceptRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[NiToNiInterceptView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(period)(request, messages(application)).toString
        }
      }

    }

    "#onSubmit" - {

      "where there are no Ni Sales then navigate to Ni Question page" in {

        val mockSessionRepository = mock[UserAnswersRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(baseAnswers))
            .overrides(bind[UserAnswersRepository].toInstance(mockSessionRepository))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, NiToNiInterceptRoute)

          val result = route(application, request).value
          val updatedReturn = baseAnswers.remove(SalesFromNiQuery(index)).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.SoldGoodsFromNiController.onPageLoad(NormalMode, period).url
          verify(mockSessionRepository, times(1)).set(eqTo(updatedReturn))

        }
      }

      "Sold goods to Eu and Sold goods to Ni are true navigate to Eu Intercept" in {
        val mockSessionRepository = mock[UserAnswersRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(salesFromNiEuAnswers))
            .overrides(bind[UserAnswersRepository].toInstance(mockSessionRepository))
            .build()

        running(application) {
          val request =
            FakeRequest(POST, NiToNiInterceptRoute)

          val result = route(application, request).value
          val userAnswers = salesFromNiEuAnswers
            .remove(SalesFromNiQuery(index)).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.CountryToSameCountryController.onPageLoad(period).url
          verify(mockSessionRepository, times(1)).set(eqTo(userAnswers))
        }
      }
    }
  }
}
