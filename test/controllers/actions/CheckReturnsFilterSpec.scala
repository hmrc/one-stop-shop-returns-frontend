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

package controllers.actions

import base.SpecBase
import connectors.VatReturnConnector
import controllers.routes
import models.requests.OptionalDataRequest
import models.responses.NotFound
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckReturnsFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  class Harness(connector: VatReturnConnector) extends CheckReturnsFilterImpl(connector) {
    def callFilter(request: OptionalDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  private val mockConnector = mock[VatReturnConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector)
  }

  ".filter" - {

    "must return None when an existing return is not found" in {

      when(mockConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))

      val app = applicationBuilder(None)
        .overrides(bind[VatReturnConnector].toInstance(mockConnector))
        .build()

      running(app) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
        val controller = new Harness(mockConnector)

        val result = controller.callFilter(request).futureValue

        result must not be defined
      }
    }

    "must redirect to Vat Return Details page when an existing return is found" in {

      when(mockConnector.get(any())(any())) thenReturn Future.successful(Right(completeVatReturn))

      val app = applicationBuilder(None)
        .overrides(bind[VatReturnConnector].toInstance(mockConnector))
        .build()

      running(app) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
        val controller = new Harness(mockConnector)

        val result = controller.callFilter(request).futureValue

        result.value mustEqual Redirect(routes.PreviousReturnController.onPageLoad(period))
      }
    }
  }

}
