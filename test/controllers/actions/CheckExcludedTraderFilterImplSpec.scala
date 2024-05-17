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
import config.FrontendAppConfig
import controllers.exclusions.routes
import models.{Period, StandardPeriod}
import models.Quarter.{Q2, Q3}
import models.exclusions.ExcludedTrader
import models.requests.OptionalDataRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import services.PeriodService

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckExcludedTraderFilterImplSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  class Harness(periodService: PeriodService, startReturnPeriod: Period, frontendAppConfig: FrontendAppConfig)
    extends CheckExcludedTraderFilterImpl(periodService, startReturnPeriod, frontendAppConfig) {
    def callFilter(request: OptionalDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  val periodService: PeriodService = mock[PeriodService]
  val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]

  override def beforeEach(): Unit = {
    Mockito.reset(periodService)
  }

  private val periodYear = 2022
  private val exclusionReason = Gen.oneOf("01", "02", "03", "04", "05", "06", "-01").sample.value.toInt

  ".filter" - {

    "must return None when trader is not excluded" in {

      val startReturnPeriod = StandardPeriod(periodYear, Q3)

      val app = applicationBuilder(None)
        .overrides(bind[PeriodService].toInstance(periodService))
        .build()

      when(mockConfig.exclusionsEnabled) thenReturn true
      when(periodService.getNextPeriod(any())) thenReturn StandardPeriod(periodYear, Q3)

      running(app) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, None)
        val controller = new Harness(periodService, startReturnPeriod, mockConfig)

        val result = controller.callFilter(request).futureValue

        result mustBe None
      }

    }

    "must return None when trader is excluded but can complete returns up to and including exclusion effective period" in {

      val excludedPeriod = StandardPeriod(periodYear, Q2)
      val startReturnPeriod = StandardPeriod(periodYear, Q2)

      val excludedTrader: ExcludedTrader = ExcludedTrader(vrn, exclusionReason, excludedPeriod, LocalDate.now())

      val excludedRegistration = registration.copy(excludedTrader = Some(excludedTrader))

      val app = applicationBuilder(None)
        .overrides(bind[PeriodService].toInstance(periodService))
        .build()

      when(mockConfig.exclusionsEnabled) thenReturn true
      when(periodService.getNextPeriod(any())) thenReturn StandardPeriod(periodYear, Q3)

      running(app) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, excludedRegistration, None)
        val controller = new Harness(periodService, startReturnPeriod, mockConfig)

        val result = controller.callFilter(request).futureValue

        result mustBe None
      }

    }

    "must Redirect when trader is excluded" in {

      val excludedPeriod = StandardPeriod(periodYear, Q2)
      val startReturnPeriod = StandardPeriod(periodYear, Q3)

      val excludedTrader: ExcludedTrader =
        ExcludedTrader(vrn, exclusionReason, excludedPeriod, LocalDate.now())

      val excludedRegistration = registration.copy(excludedTrader = Some(excludedTrader))

      val app = applicationBuilder(None)
        .overrides(bind[PeriodService].toInstance(periodService))
        .build()

      when(mockConfig.exclusionsEnabled) thenReturn true
      when(periodService.getNextPeriod(any())) thenReturn StandardPeriod(periodYear, Q3)

      running(app) {

        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, excludedRegistration, None)
        val controller = new Harness(periodService, startReturnPeriod, mockConfig)

        val result = controller.callFilter(request).futureValue

        result.value mustBe Redirect(routes.ExcludedNotPermittedController.onPageLoad())
      }

    }

    "must return None when trader is excluded but exclusions are disabled" in {

      val excludedPeriod = StandardPeriod(periodYear, Q2)
      val startReturnPeriod = StandardPeriod(periodYear, Q3)

      val excludedTrader: ExcludedTrader =
        ExcludedTrader(vrn, exclusionReason, excludedPeriod, LocalDate.now())

      val excludedRegistration = registration.copy(excludedTrader = Some(excludedTrader))

      val app = applicationBuilder(None)
        .overrides(bind[PeriodService].toInstance(periodService))
        .build()

      when(mockConfig.exclusionsEnabled) thenReturn false
      when(periodService.getNextPeriod(any())) thenReturn StandardPeriod(periodYear, Q3)

      running(app) {

        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, excludedRegistration, None)
        val controller = new Harness(periodService, startReturnPeriod, mockConfig)

        val result = controller.callFilter(request).futureValue

        result mustBe None
      }

    }
  }

}

