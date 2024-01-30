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

package controllers.actions

import base.SpecBase
import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.routes
import models.requests.{IdentifierRequest, RegistrationRequest}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import repositories.RegistrationRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetRegistrationActionSpec extends SpecBase with MockitoSugar with EitherValues {

  class Harness(
                 repository: RegistrationRepository,
                 connector: RegistrationConnector,
                 appConfig: FrontendAppConfig
               ) extends GetRegistrationAction(repository, connector, appConfig) {
    def callRefine[A](request: IdentifierRequest[A]): Future[Either[Result, RegistrationRequest[A]]] =
      refine(request)
  }

  "Get Registration Action" - {

    "when registration cache is enabled" - {

      "when there is a registration in the repository" - {

        "must return Right" in {

          val request = FakeRequest()
          val repository = mock[RegistrationRepository]
          val connector = mock[RegistrationConnector]
          val appConfig = mock[FrontendAppConfig]
          when(appConfig.cacheRegistrations) thenReturn true
          when(repository.get(userAnswersId)) thenReturn Future.successful(Some(registration))
          val action = new Harness(repository, connector, appConfig)

          val result = action.callRefine(IdentifierRequest(request, testCredentials, vrn)).futureValue

          result.isRight mustEqual true
        }
      }

      "when there is no registration in the repository" - {

        "and a registration can be retrieved from the backend" - {

          "must save the registration to the repository and return Right" in {

            val request = FakeRequest()
            val repository = mock[RegistrationRepository]
            val connector = mock[RegistrationConnector]
            val appConfig = mock[FrontendAppConfig]
            when(appConfig.cacheRegistrations) thenReturn true
            when(repository.get(userAnswersId)) thenReturn Future.successful(None)
            when(repository.set(any(), any())) thenReturn Future.successful(true)
            when(connector.get()(any())) thenReturn Future.successful(Some(registration))

            val action = new Harness(repository, connector, appConfig)

            val result = action.callRefine(IdentifierRequest(request, testCredentials, vrn)).futureValue

            result.isRight mustEqual true
            verify(repository, times(1)).set(eqTo(userAnswersId), eqTo(registration))
          }
        }

        "and a registration cannot be retrieved from the backend" - {

          "must return Left and redirect to Not Registered" in {

            val request = FakeRequest()
            val repository = mock[RegistrationRepository]
            val connector = mock[RegistrationConnector]
            val appConfig = mock[FrontendAppConfig]
            when(appConfig.cacheRegistrations) thenReturn true
            when(repository.get(userAnswersId)) thenReturn Future.successful(None)
            when(connector.get()(any())) thenReturn Future.successful(None)

            val action = new Harness(repository, connector, appConfig)

            val result = action.callRefine(IdentifierRequest(request, testCredentials, vrn)).futureValue

            result.left.value mustEqual Redirect(routes.NotRegisteredController.onPageLoad())
          }
        }
      }
    }

    "when registration cache is disabled" - {

      "must not save registration in repository" - {
        val request = FakeRequest()
        val repository = mock[RegistrationRepository]
        val connector = mock[RegistrationConnector]
        val appConfig = mock[FrontendAppConfig]
        when(appConfig.cacheRegistrations) thenReturn false
        when(connector.get()(any())) thenReturn Future.successful(Some(registration))

        val action = new Harness(repository, connector, appConfig)

        val result = action.callRefine(IdentifierRequest(request, testCredentials, vrn)).futureValue

        result.isRight mustEqual true
        verifyNoInteractions(repository)
      }

    }
  }
}
