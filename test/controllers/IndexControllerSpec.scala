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
import connectors.RegistrationConnector
import generators.Generators
import models.registration.Registration
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.IndexView

import scala.concurrent.Future

class IndexControllerSpec extends SpecBase with MockitoSugar with Generators {

  "Index Controller" - {

    "GET" - {

      "when we already have UserAnswers set up for this user" - {

        "must return OK and the correct view" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[IndexView]

            status(result) mustEqual OK

            contentAsString(result) mustEqual view()(request, messages(application)).toString
          }
        }
      }

      "when we do not already have User Answers for this user" - {

        "and the user is registered" - {

          "must create a UserAnswers, return OK and show the correct view" in {

            val registration = arbitrary[Registration].sample.value

            val mockConnector  = mock[RegistrationConnector]
            val mockRepository = mock[SessionRepository]
            when(mockConnector.get()(any())) thenReturn Future.successful(Some(registration))
            when(mockRepository.set(any())) thenReturn Future.successful(true)

            val application =
              applicationBuilder(userAnswers = None)
                .overrides(
                  bind[RegistrationConnector].toInstance(mockConnector),
                  bind[SessionRepository].toInstance(mockRepository)
                )
                .build()

            running(application) {
              val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

              val result = route(application, request).value

              val view = application.injector.instanceOf[IndexView]

              status(result) mustEqual OK

              contentAsString(result) mustEqual view()(request, messages(application)).toString
              verify(mockRepository, times(1)).set(any())
            }
          }
        }

        "and the user is not registered" - {

          "must redirect the user to Journey Recovery" in {

            val mockConnector  = mock[RegistrationConnector]
            val mockRepository = mock[SessionRepository]
            when(mockConnector.get()(any())) thenReturn Future.successful(None)

            val application =
              applicationBuilder(userAnswers = None)
                .overrides(
                  bind[RegistrationConnector].toInstance(mockConnector),
                  bind[SessionRepository].toInstance(mockRepository)
                )
                .build()

            running(application) {
              val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.NotRegisteredController.onPageLoad().url
              verify(mockRepository, never).set(any())
            }
          }
        }
      }
    }
  }
}