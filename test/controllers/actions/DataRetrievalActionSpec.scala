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
import models.Period
import models.requests.{OptionalDataRequest, RegistrationRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import repositories.UserAnswersRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataRetrievalActionSpec extends SpecBase with MockitoSugar {

  class Harness(period: Period, repository: UserAnswersRepository) extends DataRetrievalAction(period, repository) {

    def callTransform(request: RegistrationRequest[_]): Future[OptionalDataRequest[_]] =
      transform(request)
  }

  "Data Retrieval Action" - {

    "when there is no data in the cache" - {

      "must set userAnswers to `None` in the request" in {

        val period     = arbitrary[Period].sample.value
        val repository = mock[UserAnswersRepository]
        val request    = RegistrationRequest(FakeRequest(), testCredentials, vrn, registration)

        when(repository.get(any(), any())) thenReturn Future.successful(None)

        val action = new Harness(period, repository)

        val result = action.callTransform(request).futureValue

        result.userAnswers must not be defined
      }
    }

    "when there is data in the cache" - {

      "must add the userAnswers to the request" in {

        val period     = arbitrary[Period].sample.value
        val repository = mock[UserAnswersRepository]
        val request    = RegistrationRequest(FakeRequest(), testCredentials, vrn, registration)

        when(repository.get(any(), any())) thenReturn Future.successful(Some(emptyUserAnswers))

        val action = new Harness(period, repository)

        val result = action.callTransform(request).futureValue

        result.userAnswers.value mustEqual emptyUserAnswers
      }
    }
  }
}
