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

package services.external

import base.SpecBase
import models.SessionData
import models.external._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.JsObject
import repositories.SessionRepository

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class ExternalServiceSpec extends SpecBase {
  implicit private lazy val ec: ExecutionContext = ExecutionContext.global
  val userId = "1234"
  val sessionData = SessionData(userId, JsObject.empty, Instant.now)
  val externalRequest = ExternalRequest("BTA", "/bta")
  val currentPeriod = arbitraryPeriod.arbitrary.sample.value

  ".getExternalResponse" - {
    Seq(YourAccount, ReturnsHistory).map {
      entryPage =>
        s"when entry page in the request is ${entryPage}" - {

          "and no period is provided" - {
            "and language specified is Welsh" - {
              "must return correct response" in {
                val sessionRepository = mock[SessionRepository]
                val service = new ExternalService(sessionRepository)

                when(sessionRepository.get(any())) thenReturn Future.successful(Seq(sessionData))
                when(sessionRepository.set(any())) thenReturn Future.successful(true)
                val result = service.getExternalResponse(externalRequest, userId, entryPage.name, None, Some("cy"))

                result mustBe Right(
                  ExternalResponse(
                    NoMoreWelsh.url(entryPage.url)
                  )
                )
              }
            }

            "and language is not Welsh" - {
              "must return correct response" in {
                val sessionRepository = mock[SessionRepository]
                val service = new ExternalService(sessionRepository)

                when(sessionRepository.get(any())) thenReturn Future.successful(Seq(sessionData))
                when(sessionRepository.set(any())) thenReturn Future.successful(true)
                val result = service.getExternalResponse(externalRequest, userId, entryPage.name, None, None)

                result mustBe Right(
                  ExternalResponse(
                    entryPage.url
                  )
                )
              }
            }
          }
        }
    }

    Seq(StartReturn, ContinueReturn).map {
      entryPage =>
        s"when entry page in the request is ${entryPage}" - {

          "and period is provided" - {
            "and language specified is Welsh" - {
              "must return correct response" in {
                val sessionRepository = mock[SessionRepository]
                val service = new ExternalService(sessionRepository)

                when(sessionRepository.get(any())) thenReturn Future.successful(Seq(sessionData))
                when(sessionRepository.set(any())) thenReturn Future.successful(true)
                val result = service.getExternalResponse(externalRequest, userId, entryPage.name, Some(currentPeriod), Some("cy"))

                result mustBe Right(
                  ExternalResponse(
                    NoMoreWelsh.url(entryPage.url(currentPeriod))
                  )
                )
              }
            }

            "and language is not Welsh" - {
              "must return correct response" in {
                val sessionRepository = mock[SessionRepository]
                val service = new ExternalService(sessionRepository)

                when(sessionRepository.get(any())) thenReturn Future.successful(Seq(sessionData))
                when(sessionRepository.set(any())) thenReturn Future.successful(true)
                val result = service.getExternalResponse(externalRequest, userId, entryPage.name, Some(currentPeriod), None)

                result mustBe Right(
                  ExternalResponse(
                    entryPage.url(currentPeriod)
                  )
                )
              }
            }
          }
        }
    }


  }
}
