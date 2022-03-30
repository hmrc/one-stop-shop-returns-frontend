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

package controllers.external

import base.SpecBase
import controllers.routes
import models.SessionData
import models.external.{ExternalRequest, ExternalResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.OK
import play.api.inject
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.{SessionRepository, UserAnswersRepository}

import scala.concurrent.Future

class ExternalControllerSpec extends SpecBase {
  private val yourAccount = "your-account"
  private val returnsHistory = "returns-history"
  private val startReturn = "start-your-return"
  private val continueReturn = "continue-your-return"
  private val externalRequest = ExternalRequest("BTA", "exampleurl")


  ".onExternal" - {

    "when correct ExternalRequest is posted" - {
      "must save external return url in session" - {
        "and respond with OK(YourAccountUrl) when your-account and no period provided" in {
          val mockSessionRepository = mock[SessionRepository]
          val mockUserAnswersRepository = mock[UserAnswersRepository]

          when(mockSessionRepository.get(any())) thenReturn Future.successful(Seq.empty)
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
          when(mockUserAnswersRepository.get(any())) thenReturn Future.successful(Seq(completeUserAnswers))

          val application = applicationBuilder()
            .overrides(inject.bind[SessionRepository].toInstance(mockSessionRepository))
            .build()

          running(application) {
            val request = FakeRequest(POST, routes.ExternalController.onExternal(yourAccount).url).withJsonBody(
              Json.toJson(externalRequest)
            )

            val result = route(application, request).value
            status(result) mustBe OK
            contentAsJson(result).as[ExternalResponse] mustBe ExternalResponse(controllers.routes.YourAccountController.onPageLoad().url)
            verify(mockSessionRepository, times(1)).set(any())
          }
        }

        "and respond with OK(ReturnsHistoryUrl) when returns-history and no period provided" in {
          val mockSessionRepository = mock[SessionRepository]

          when(mockSessionRepository.get(any())) thenReturn Future.successful(Seq.empty)
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val application = applicationBuilder()
            .overrides(inject.bind[SessionRepository].toInstance(mockSessionRepository))
            .build()

          running(application) {
            val request = FakeRequest(POST, routes.ExternalController.onExternal(returnsHistory).url).withJsonBody(
              Json.toJson(externalRequest)
            )

            val result = route(application, request).value
            status(result) mustBe OK
            contentAsJson(result).as[ExternalResponse] mustBe ExternalResponse(controllers.routes.SubmittedReturnsHistoryController.onPageLoad().url)
            verify(mockSessionRepository, times(1)).set(any())
          }
        }

        "and respond with OK(StartYourReturnUrl) when start-your-return and a period provided" in {
          val mockSessionRepository = mock[SessionRepository]

          when(mockSessionRepository.get(any())) thenReturn Future.successful(Seq.empty)
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val application = applicationBuilder()
            .overrides(inject.bind[SessionRepository].toInstance(mockSessionRepository))
            .build()

          running(application) {
            val request = FakeRequest(POST, routes.ExternalController.onExternal(startReturn, Some(period)).url).withJsonBody(
              Json.toJson(externalRequest)
            )

            val result = route(application, request).value
            status(result) mustBe OK
            contentAsJson(result).as[ExternalResponse] mustBe ExternalResponse(controllers.routes.StartReturnController.onPageLoad(period).url)
            verify(mockSessionRepository, times(1)).set(any())
          }
        }

        "and respond with OK(ContinueYourReturnUrl) when continue-your-return and a period provided" in {
          val mockSessionRepository = mock[SessionRepository]

          when(mockSessionRepository.get(any())) thenReturn Future.successful(Seq.empty)
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val application = applicationBuilder()
            .overrides(inject.bind[SessionRepository].toInstance(mockSessionRepository))
            .build()

          running(application) {
            val request = FakeRequest(POST, routes.ExternalController.onExternal(continueReturn, Some(period)).url).withJsonBody(
              Json.toJson(externalRequest)
            )

            val result = route(application, request).value
            status(result) mustBe OK
            contentAsJson(result).as[ExternalResponse] mustBe ExternalResponse(controllers.routes.ContinueReturnController.onPageLoad(period).url)
            verify(mockSessionRepository, times(1)).set(any())
          }
        }
      }
    }
  }

}
