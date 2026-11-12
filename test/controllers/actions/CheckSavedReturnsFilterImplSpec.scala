/*
 * Copyright 2026 HM Revenue & Customs
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
import controllers.routes
import models.requests.OptionalDataRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckSavedReturnsFilterImplSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockSaveForLaterConnector: SaveForLaterConnector = mock[SaveForLaterConnector]

  private val savedUserAnswers: SavedUserAnswers = arbitrarySavedUserAnswers.arbitrary.sample.value

  class Harness extends CheckSavedReturnsFilterImpl(mockSaveForLaterConnector) {
    def callFilter(request: OptionalDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  override def beforeEach(): Unit = {
    Mockito.reset(mockSaveForLaterConnector)
  }

  "CheckSavedReturnsFilterImpl" - {

    "must return None when a saved return is present" in {

      when(mockSaveForLaterConnector.get()(any())) thenReturn Right(Some(savedUserAnswers)).toFuture

      val application = applicationBuilder()
        .overrides(bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector))
        .build()

      running(application) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, None)
        val controller = new Harness()

        val result = controller.callFilter(request).futureValue

        result must not be defined
        verify(mockSaveForLaterConnector, times(1)).get()(any())
      }
    }

    "must redirect to No Saved Returns when a saved return is not present" in {

      when(mockSaveForLaterConnector.get()(any())) thenReturn Right(None).toFuture

      val application = applicationBuilder()
        .overrides(bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector))
        .build()

      running(application) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, None)
        val controller = new Harness()

        val result = controller.callFilter(request).futureValue

        result `mustBe` Some(Redirect(routes.NoSavedReturnsController.onPageLoad))
        verify(mockSaveForLaterConnector, times(1)).get()(any())
      }
    }
  }
}
