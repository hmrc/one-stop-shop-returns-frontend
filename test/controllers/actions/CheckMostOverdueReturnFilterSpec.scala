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
import connectors.ReturnStatusConnector
import controllers.routes
import models.{PeriodWithStatus, StandardPeriod, SubmissionStatus}
import models.requests.OptionalDataRequest
import models.responses.UnexpectedResponseStatus
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

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckMostOverdueReturnFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val instant = Instant.parse("2022-12-31T00:00:00.00Z")
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  class Harness(connector: ReturnStatusConnector) extends CheckMostOverdueReturnFilterImpl(period, connector, stubClock) {
    def callFilter(request: OptionalDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  private val mockConnector = mock[ReturnStatusConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector)
  }

  ".filter" - {

    "must redirect to Journey Recovery if connector returns error response" in {

      when(mockConnector.listStatuses(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(123, "Error")))

      val app = applicationBuilder(None)
        .overrides(bind[ReturnStatusConnector].toInstance(mockConnector))
        .build()

      running(app) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
        val controller = new Harness(mockConnector)

        val result = controller.callFilter(request).futureValue

        result.value mustEqual Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }

    "must return None if the return period is the most overdue" in {

      when(mockConnector.listStatuses(any())(any())) thenReturn Future.successful(Right(Seq(
        PeriodWithStatus(StandardPeriod("2021", "Q2").success.value, SubmissionStatus.Complete),
        PeriodWithStatus(StandardPeriod("2021", "Q3").success.value, SubmissionStatus.Overdue),
        PeriodWithStatus(StandardPeriod("2021", "Q4").success.value, SubmissionStatus.Overdue),
        PeriodWithStatus(StandardPeriod("2022", "Q1").success.value, SubmissionStatus.Due)
      )))

      val app = applicationBuilder(None)
        .overrides(bind[ReturnStatusConnector].toInstance(mockConnector))
        .build()

      running(app) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
        val controller = new Harness(mockConnector)

        val result = controller.callFilter(request).futureValue

        result must not be defined
      }
    }

    "must redirect to CannotStartReturn if the return period is not the most overdue" in {

      when(mockConnector.listStatuses(any())(any())) thenReturn Future.successful(Right(Seq(
        PeriodWithStatus(StandardPeriod("2021", "Q2").success.value, SubmissionStatus.Overdue),
        PeriodWithStatus(StandardPeriod("2021", "Q3").success.value, SubmissionStatus.Overdue),
        PeriodWithStatus(StandardPeriod("2021", "Q4").success.value, SubmissionStatus.Overdue),
        PeriodWithStatus(StandardPeriod("2022", "Q1").success.value, SubmissionStatus.Due)
      )))

      val app = applicationBuilder(None)
        .overrides(bind[ReturnStatusConnector].toInstance(mockConnector))
        .build()

      running(app) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
        val controller = new Harness(mockConnector)

        val result = controller.callFilter(request).futureValue

        result.value mustEqual Redirect(routes.CannotStartReturnController.onPageLoad())
      }
    }

    "must redirect to NoOtherPeriodsAvailableController if the return period is Excluded and 3 years old" in {

      when(mockConnector.listStatuses(any())(any())) thenReturn Future.successful(Right(Seq(
        PeriodWithStatus(StandardPeriod("2019", "Q3").success.value, SubmissionStatus.Excluded)
      )))

      val app = applicationBuilder(None)
        .overrides(bind[ReturnStatusConnector].toInstance(mockConnector))
        .build()

      running(app) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
        val controller = new Harness(mockConnector)

        val result = controller.callFilter(request).futureValue

        result.value mustEqual Redirect(routes.NoOtherPeriodsAvailableController.onPageLoad())
      }
    }

    "must redirect to CannotStartReturn if the return period is Excluded and not three years old" in {

      when(mockConnector.listStatuses(any())(any())) thenReturn Future.successful(Right(Seq(
        PeriodWithStatus(StandardPeriod("2021", "Q3").success.value, SubmissionStatus.Excluded)
      )))

      val app = applicationBuilder(None)
        .overrides(bind[ReturnStatusConnector].toInstance(mockConnector))
        .build()

      running(app) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
        val controller = new Harness(mockConnector)

        val result = controller.callFilter(request).futureValue

        result must not be defined
      }
    }

    "must redirect to NoOtherPeriodsAvailable if there are no returns due" in {

      when(mockConnector.listStatuses(any())(any())) thenReturn Future.successful(Right(Seq(
        PeriodWithStatus(StandardPeriod("2021", "Q2").success.value, SubmissionStatus.Complete),
        PeriodWithStatus(StandardPeriod("2021", "Q3").success.value, SubmissionStatus.Complete),
        PeriodWithStatus(StandardPeriod("2021", "Q4").success.value, SubmissionStatus.Complete),
        PeriodWithStatus(StandardPeriod("2022", "Q1").success.value, SubmissionStatus.Complete)
      )))

      val app = applicationBuilder(None)
        .overrides(bind[ReturnStatusConnector].toInstance(mockConnector))
        .build()

      running(app) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
        val controller = new Harness(mockConnector)

        val result = controller.callFilter(request).futureValue

        result.value mustEqual Redirect(routes.NoOtherPeriodsAvailableController.onPageLoad())
      }
    }
  }

}
