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
import connectors.ReturnStatusConnector
import controllers.routes
import models.Quarter.Q3
import models.requests.DataRequest
import models.responses.UnexpectedResponseStatus
import models.{Period, PeriodWithStatus, SubmissionStatus}
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

class CheckSubmittedReturnsFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  class Harness(connector: ReturnStatusConnector) extends CheckSubmittedReturnsFilterImpl(connector) {
    def callFilter(request: DataRequest[_]): Future[Option[Result]] = filter(request)
  }

  private val mockConnector = mock[ReturnStatusConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector)
  }

  ".filter" - {

    "must return None when submitted returns are found" in {

      when(mockConnector.listStatuses(any())(any())) thenReturn Future.successful(
        Right(Seq(PeriodWithStatus(Period(2021, Q3), SubmissionStatus.Complete))))

      val app = applicationBuilder(None)
        .overrides(bind[ReturnStatusConnector].toInstance(mockConnector))
        .build()

      running(app) {
        val request = DataRequest(FakeRequest(), testCredentials, vrn, registration, completeUserAnswers)
        val controller = new Harness(mockConnector)

        val result = controller.callFilter(request).futureValue

        result must not be defined
      }
    }

    "must redirect to CheckYourAnswers page" - {

      "when no returns are found" in {

        when(mockConnector.listStatuses(any())(any())) thenReturn Future.successful(Right(Seq.empty))

        val app = applicationBuilder(None)
          .overrides(bind[ReturnStatusConnector].toInstance(mockConnector))
          .build()

        running(app) {
          val request = DataRequest(FakeRequest(), testCredentials, vrn, registration, completeUserAnswers)
          val controller = new Harness(mockConnector)

          val result = controller.callFilter(request).futureValue

          result.value mustEqual Redirect(routes.CheckYourAnswersController.onPageLoad(period))
        }
      }

      "when returns are found but there is no complete return" in {

        when(mockConnector.listStatuses(any())(any())) thenReturn Future.successful(Right(Seq(
          PeriodWithStatus(Period(2021, Q3), SubmissionStatus.Due))))

        val app = applicationBuilder(None)
          .overrides(bind[ReturnStatusConnector].toInstance(mockConnector))
          .build()

        running(app) {
          val request = DataRequest(FakeRequest(), testCredentials, vrn, registration, completeUserAnswers)
          val controller = new Harness(mockConnector)

          val result = controller.callFilter(request).futureValue

          result.value mustEqual Redirect(routes.CheckYourAnswersController.onPageLoad(period))
        }
      }

      "when there is an error retrieving the periods" in {

        when(mockConnector.listStatuses(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(123, "something went wrong")))

        val app = applicationBuilder(None)
          .overrides(bind[ReturnStatusConnector].toInstance(mockConnector))
          .build()

        running(app) {
          val request = DataRequest(FakeRequest(), testCredentials, vrn, registration, completeUserAnswers)
          val controller = new Harness(mockConnector)

          val result = controller.callFilter(request).futureValue

          result.value mustEqual Redirect(routes.CheckYourAnswersController.onPageLoad(period))
        }
      }
    }

  }

}
