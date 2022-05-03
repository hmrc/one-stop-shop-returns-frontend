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

package controllers.actions

import base.SpecBase
import connectors.{SaveForLaterConnector, SavedUserAnswers}
import models.UserAnswers
import models.requests.{OptionalDataRequest, RegistrationRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import repositories.UserAnswersRepository

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SavedAnswersRetrievalActionSpec extends SpecBase with MockitoSugar {

  private val expectedSavedUserAnswers =
    SavedUserAnswers(
      vrn, period, JsObject(Seq("test" -> Json.toJson("test"))),
      Instant.now(stubClockAtArbitraryDate)
    )
  class Harness(repository: UserAnswersRepository, saveForLaterConnector: SaveForLaterConnector)
    extends SavedAnswersRetrievalAction(repository, saveForLaterConnector) {

    def callTransform(request: RegistrationRequest[_]): Future[OptionalDataRequest[_]] =
      transform(request)
  }

  "Saved Answers Retrieval Action" - {

    "when there is no data in the cache" - {

      "must set userAnswers to `None` in the request if there are no answers saved in the backend" in {

        val saveForLaterConnector = mock[SaveForLaterConnector]
        val repository = mock[UserAnswersRepository]
        val request    = RegistrationRequest(FakeRequest(), testCredentials, vrn, registration)

        when(repository.get(any())) thenReturn Future.successful(Seq.empty)
        when(saveForLaterConnector.get()(any())) thenReturn Future.successful(Right(None))

        val action = new Harness(repository, saveForLaterConnector)

        val result = action.callTransform(request).futureValue

        result.userAnswers must not be defined
      }

      "must set userAnswers and save them in UserAnswersRepository if there are answers saved in the backend" in {

        val saveForLaterConnector = mock[SaveForLaterConnector]
        val repository = mock[UserAnswersRepository]
        val request    = RegistrationRequest(FakeRequest(), testCredentials, vrn, registration)

        when(repository.get(any())) thenReturn Future.successful(Seq.empty)
        when(saveForLaterConnector.get()(any())) thenReturn Future.successful(Right(Some(expectedSavedUserAnswers)))

        val action = new Harness(repository, saveForLaterConnector)

        val result = action.callTransform(request).futureValue
        val newAnswers = UserAnswers(request.userId, expectedSavedUserAnswers.period, expectedSavedUserAnswers.data, expectedSavedUserAnswers.lastUpdated)

        result.userAnswers mustBe Some(newAnswers)
        verify(repository, times(1)).set(newAnswers)
      }
    }

    "when there is data in the cache" - {

      "must add the userAnswers to the request" in {

        val saveForLaterConnector = mock[SaveForLaterConnector]
        val repository = mock[UserAnswersRepository]
        val request    = RegistrationRequest(FakeRequest(), testCredentials, vrn, registration)

        when(repository.get(any())) thenReturn Future.successful(Seq(emptyUserAnswers))
        when(saveForLaterConnector.get()(any())) thenReturn Future.successful(Right(Some(expectedSavedUserAnswers)))
        val action = new Harness(repository, saveForLaterConnector)

        val result = action.callTransform(request).futureValue

        result.userAnswers.value mustEqual emptyUserAnswers
      }
    }
  }
}
