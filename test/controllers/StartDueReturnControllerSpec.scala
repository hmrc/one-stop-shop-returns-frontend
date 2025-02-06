/*
 * Copyright 2025 HM Revenue & Customs
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
import connectors.ReturnStatusConnector
import models.StandardPeriod
import models.SubmissionStatus.{Complete, Due}
import models.responses.{ErrorResponse, UnexpectedResponseStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.FutureSyntax.FutureOps
import viewmodels.yourAccount.{CurrentReturns, Return}

class StartDueReturnControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val mockReturnStatusConnector: ReturnStatusConnector = mock[ReturnStatusConnector]
  private val currentReturns: CurrentReturns = arbitraryCurrentReturns.arbitrary.sample.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockReturnStatusConnector)
    super.beforeEach()
  }

  "StartDueReturn Controller" - {

    "must redirect to Start Return Controller when a due return exists" in {

      val dueReturn: Return = Return(
        period = period,
        firstDay = period.firstDay,
        lastDay = period.lastDay,
        dueDate = period.paymentDeadline,
        submissionStatus = Due,
        inProgress = false,
        isOldest = true
      )

      val nextPeriod: StandardPeriod = StandardPeriod(period.getNextPeriod.year, period.getNextPeriod.quarter)

      val completeReturn: Return = Return(
        period = nextPeriod,
        firstDay = nextPeriod.firstDay,
        lastDay = nextPeriod.lastDay,
        dueDate = nextPeriod.paymentDeadline,
        submissionStatus = Complete,
        inProgress = false,
        isOldest = false
      )

      val returns = currentReturns
        .copy(excluded = false)
        .copy(returns = Seq(dueReturn, completeReturn))

      when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn Right(returns).toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.StartDueReturnController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.StartReturnController.onPageLoad(period).url
      }
    }

    "must redirect to No Returns Due Controller when no due or overdue returns exist" in {

      val noCurrentReturns: CurrentReturns = CurrentReturns(
        returns = Seq.empty,
        excludedReturns = Seq.empty
      )

      when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn Right(noCurrentReturns).toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.StartDueReturnController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.NoReturnsDueController.onPageLoad().url
      }
    }

    "must throw an Exception when Return Status Connector fails to retrieve Current Returns" in {

      val error: ErrorResponse = UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "ERROR")

      when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn Left(error).toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.StartDueReturnController.onPageLoad().url)

        val result = route(application, request).value

        whenReady(result.failed) { exp =>
          exp mustBe an[Exception]
          exp.getMessage mustBe "Error when getting current returns: ERROR"
        }
      }
    }
  }
}
