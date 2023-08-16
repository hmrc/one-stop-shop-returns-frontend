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
import models.Country
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.CountryOfConsumptionFromNiPage
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.UserAnswersRepository
import views.html.NiToNiInterceptView
import play.api.inject.bind
import queries.SalesFromNiQuery

import scala.concurrent.Future

class NiToNiInterceptControllerSpec extends SpecBase with MockitoSugar {

  private lazy val NiToNiInterceptRoute = routes.NiToNiInterceptController.onPageLoad(period).url

  private val baseAnswers =
    emptyUserAnswers
      .set(CountryOfConsumptionFromNiPage(index), Country("XI", "Northern Ireland")).success.value

  "NiToNiIntercept Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, NiToNiInterceptRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NiToNiInterceptView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(period)(request, messages(application)).toString
      }
    }

    "must delete NI to NI sales and redirect to the correct page when the user clicks confirm" in {

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
        redirectLocation(result).value mustEqual controllers.routes.CheckYourAnswersController.onPageLoad(period).url
        verify(mockSessionRepository, times(1)).set(eqTo(updatedReturn))

      }
    }

  }
}
