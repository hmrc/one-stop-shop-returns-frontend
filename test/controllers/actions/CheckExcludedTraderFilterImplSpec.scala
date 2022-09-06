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

import controllers.exclusions.routes
import base.SpecBase
import models.exclusions.ExcludedTrader
import models.requests.IdentifierRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import play.api.inject.bind
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import services.exclusions.ExclusionService

import java.time.LocalDate
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class CheckExcludedTraderFilterImplSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  class Harness(exclusionService: ExclusionService) extends CheckExcludedTraderFilterImpl(exclusionService) {
    def callFilter(request: IdentifierRequest[_]): Future[Option[Result]] = filter(request)
  }

  val exclusionService: ExclusionService = mock[ExclusionService]

  override def beforeEach(): Unit = {
    Mockito.reset(exclusionService)
  }

  ".filter" - {

    "must return None when trader is not excluded" in {


      val app = applicationBuilder(None)
        .overrides(bind[ExclusionService].toInstance(exclusionService))
        .build()

      when(exclusionService.findExcludedTrader(any())) thenReturn None

      running(app) {
        val request = IdentifierRequest(FakeRequest(), testCredentials, vrn)
        val controller = new Harness(exclusionService)

        val result = controller.callFilter(request).futureValue

        result mustBe None
      }

    }

    "must return Some(ExcludedTrader) when trader is excluded" in {

      val exclusionReason = 4

      val excludedTrader: ExcludedTrader =
        ExcludedTrader(vrn, "HMRC", exclusionReason, LocalDate.now.format(ExcludedTrader.dateFormatter))

      val app = applicationBuilder(None)
        .overrides(bind[ExclusionService].toInstance(exclusionService))
        .build()

      when(exclusionService.findExcludedTrader(any())) thenReturn Some(excludedTrader)

      running(app) {

        val request = IdentifierRequest(FakeRequest(), testCredentials, vrn)
        val controller = new Harness(exclusionService)

        val result = controller.callFilter(request).futureValue

        result.value mustBe Redirect(routes.ExcludedNotPermittedController.onPageLoad())
      }

    }
  }

}
