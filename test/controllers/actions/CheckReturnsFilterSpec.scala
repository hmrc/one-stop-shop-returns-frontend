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
import controllers.routes
import models.etmp.EtmpObligationDetails
import models.etmp.EtmpObligationsFulfilmentStatus.{Fulfilled, Open}
import models.requests.OptionalDataRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import services.ObligationsService
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckReturnsFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  class Harness(obligationsService: ObligationsService) extends CheckReturnsFilterImpl(period, obligationsService) {
    def callFilter(request: OptionalDataRequest[_]): Future[Option[Result]] = filter(request)
  }
  
  private val mockObligationService = mock[ObligationsService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockObligationService)
  }

  ".filter" - {
    
    val periodKey: String = s"${period.year.toString.substring(2, 4)}${period.quarter}"

    val obligationDetails: Seq[EtmpObligationDetails] =
      Seq(
        EtmpObligationDetails(
          status = Fulfilled,
          periodKey = periodKey
        ),
        EtmpObligationDetails(
          status = Fulfilled,
          periodKey = "21C4"
        ),
        EtmpObligationDetails(
          status = Open,
          periodKey = "22C1"
        )
      )

    "must redirect to Previous Return page when an existing return for the period is found" in {
      
      when(mockObligationService.getFulfilledObligations(any())(any())) thenReturn obligationDetails.toFuture

      val app = applicationBuilder(None)
        .build()

      running(app) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
        val controller = new Harness(mockObligationService)

        val result = controller.callFilter(request).futureValue

        result.value mustBe Redirect(routes.PreviousReturnController.onPageLoad(period))

        verify(mockObligationService, times(1)).getFulfilledObligations(any())(any())
      }
    }

    "must return None when an existing return for the period is not found" in {
      
      when(mockObligationService.getFulfilledObligations(any())(any())) thenReturn Seq.empty.toFuture

      val app = applicationBuilder(None)
        .build()

      running(app) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
        val controller = new Harness(mockObligationService)

        val result = controller.callFilter(request).futureValue

        result must not be defined

        verify(mockObligationService, times(1)).getFulfilledObligations(any())(any())
      }
    }
  }
}
