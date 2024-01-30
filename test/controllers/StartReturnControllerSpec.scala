/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.StartReturnFormProvider
import models.{Country, PartialReturnPeriod}
import models.Quarter.Q4
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import pages.{CountryOfConsumptionFromNiPage, StartReturnPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.UserAnswersRepository
import services.PartialReturnPeriodService
import views.html.StartReturnView

import java.time.LocalDate
import scala.concurrent.Future

class StartReturnControllerSpec extends SpecBase with MockitoSugar {

  private lazy val startReturnRoute = routes.StartReturnController.onPageLoad(period).url

  private val formProvider = new StartReturnFormProvider()

  private val mockPartialReturnPeriodService = mock[PartialReturnPeriodService]

  "StartReturn Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn Future.successful(None)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
        .build()

      running(application) {
        val form = formProvider(period)(messages(application))
        val request = FakeRequest(GET, startReturnRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[StartReturnView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, period, None)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when partial return" in {
      val partialReturn = Some(PartialReturnPeriod(LocalDate.now, LocalDate.now, 2023, Q4))

      when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn Future.successful(partialReturn)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
        .build()


      running(application) {
        val form = formProvider(period)(messages(application))
        val request = FakeRequest(GET, startReturnRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[StartReturnView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, period, partialReturn)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn Future.successful(None)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
        .build()

      running(application) {

        val request =
          FakeRequest(POST, startReturnRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual StartReturnPage.navigate(period, startReturn = true).url
      }
    }

    "must redirect to the No Other Periods Available page when answer is no" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
        .build()

      running(application) {

        val request =
          FakeRequest(POST, startReturnRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual StartReturnPage.navigate(period, startReturn = false).url
      }
    }

    "must clear useranswers when answer is no" in {
      val mockSessionRepository = mock[UserAnswersRepository]

      when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)

      val country: Country = arbitrary[Country].sample.value

      val answers = emptyUserAnswers.set(CountryOfConsumptionFromNiPage(index), country).success.value

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
        .overrides(bind[UserAnswersRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {

        val request =
          FakeRequest(POST, startReturnRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual StartReturnPage.navigate(period, startReturn = false).url
        verify(mockSessionRepository, times(1)).clear(eqTo(answers.userId))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn Future.successful(None)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
        .build()

      running(application) {
        val form = formProvider(period)(messages(application))

        val request =
          FakeRequest(POST, startReturnRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[StartReturnView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, period, None)(request, messages(application)).toString
      }
    }
  }
}
