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
import controllers.exclusions.routes
import models.Period
import models.Quarter.{Q2, Q3, Q4}
import models.exclusions.ExcludedTrader
import models.requests.OptionalDataRequest
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
import services.exclusions.ExclusionService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckExcludedTraderFilterImplSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  class Harness(exclusionService: ExclusionService, startReturnPeriod: Period)
    extends CheckExcludedTraderFilterImpl(exclusionService, startReturnPeriod) {
    def callFilter(request: OptionalDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  val exclusionService: ExclusionService = mock[ExclusionService]

  override def beforeEach(): Unit = {
    Mockito.reset(exclusionService)
  }

  ".filter" - {

    "must return None when trader is not excluded" in {

      val startReturnPeriod = Period(2022, Q3)

      val app = applicationBuilder(None)
        .overrides(bind[ExclusionService].toInstance(exclusionService))
        .build()

      when(exclusionService.findExcludedTrader(any())) thenReturn Future.successful(None)

      running(app) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, None)
        val controller = new Harness(exclusionService, startReturnPeriod)

        val result = controller.callFilter(request).futureValue

        result mustBe None
      }

    }

    "must return None when trader is excluded but can complete returns up to exclusion effective period" in {

      val startReturnPeriod = Period(2022, Q3)

      val excludedTrader: ExcludedTrader = ExcludedTrader(vrn, "HMRC", 4, Period(2022, Q4))

      val app = applicationBuilder(None)
        .overrides(bind[ExclusionService].toInstance(exclusionService))
        .build()

      when(exclusionService.findExcludedTrader(any())) thenReturn Future.successful(Some(excludedTrader))

      running(app) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, None)
        val controller = new Harness(exclusionService, startReturnPeriod)

        val result = controller.callFilter(request).futureValue

        result mustBe None
      }

    }

    "must return Some(ExcludedTrader) when trader is excluded" in {

      val excludedPeriod = Period(2022, Q2)
      val startReturnPeriod = Period(2022, Q3)

      val excludedTrader: ExcludedTrader =
        ExcludedTrader(vrn, "HMRC", 4, excludedPeriod)

      val app = applicationBuilder(None)
        .overrides(bind[ExclusionService].toInstance(exclusionService))
        .build()

      when(exclusionService.findExcludedTrader(any())) thenReturn Future.successful(Some(excludedTrader))

      running(app) {

        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, None)
        val controller = new Harness(exclusionService, startReturnPeriod)

        val result = controller.callFilter(request).futureValue

        result.value mustBe Redirect(routes.ExcludedNotPermittedController.onPageLoad())
      }

    }
  }

}

