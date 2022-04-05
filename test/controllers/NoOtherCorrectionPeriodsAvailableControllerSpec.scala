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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.UserAnswersRepository
import views.html.corrections.NoOtherCorrectionPeriodsAvailableView

import scala.concurrent.Future

class NoOtherCorrectionPeriodsAvailableControllerSpec extends SpecBase with BeforeAndAfterEach{

  private lazy val NoOtherCorrectionPeriodsAvailableRoute = controllers.corrections.routes.NoOtherCorrectionPeriodsAvailableController.onPageLoad(period).url

  val mockSessionRepository = mock[UserAnswersRepository]

  override def beforeEach(): Unit = {
    Mockito.reset(mockSessionRepository)
    super.beforeEach()
  }

  "NoOtherCorrectionPeriodsAvailable Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, NoOtherCorrectionPeriodsAvailableRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NoOtherCorrectionPeriodsAvailableView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(period)(request, messages(application)).toString
      }
    }

    "must redirect to CheckYourAnswersController when completed correction periods are empty for a POST" in {

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[UserAnswersRepository].toInstance(mockSessionRepository)
        ).build()

      running(application) {
        val request = FakeRequest(POST, NoOtherCorrectionPeriodsAvailableRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad(period).url
      }
    }

    "must redirect to CheckYourAnswersController when completed correction periods are not empty for a POST" in {

      val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithCorrections)).build()

      running(application) {
        val request = FakeRequest(POST, NoOtherCorrectionPeriodsAvailableRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad(period).url
      }
    }

    "must throw an Exception when Session Repository returns an Exception" in {

      when(mockSessionRepository.set(any())) thenReturn Future.failed(new Exception("Some exception"))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[UserAnswersRepository].toInstance(mockSessionRepository)
        ).build()

      running(application) {
        val request = FakeRequest(POST, NoOtherCorrectionPeriodsAvailableRoute)

        val result = route(application, request).value

        whenReady(result.failed) { exp => exp mustBe a[Exception] }
      }
    }
  }
}
