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
import controllers.routes
import models.requests.OptionalDataRequest
import models.Period
import models.Quarter.Q3
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
import services.PeriodService

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckCommencementDateFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  class Harness(periodService: PeriodService) extends CheckCommencementDateFilterImpl(periodService) {
    def callFilter(request: OptionalDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  val periodService: PeriodService = mock[PeriodService]

  override def beforeEach(): Unit = {
    Mockito.reset(periodService)
  }

  ".filter" - {

    "must return None when there are periods to be submitted" in {

      val app = applicationBuilder(None)
        .overrides(bind[PeriodService].toInstance(periodService))
        .build()

      val registrationModel = registration.copy(commencementDate = LocalDate.of(2021, 9, 30))

      when(periodService.getReturnPeriods(any())) thenReturn Seq(Period(2021, Q3))

      running(app) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registrationModel, Some(emptyUserAnswers))
        val controller = new Harness(periodService)

        val result = controller.callFilter(request).futureValue

        result must not be defined
      }
    }

    "must redirect when there are no periods to be submitted" in {

      val app = applicationBuilder(None)
        .overrides(bind[PeriodService].toInstance(periodService))
        .build()

      val registrationModel = registration.copy(commencementDate = LocalDate.of(2021, 9, 30))

      when(periodService.getReturnPeriods(any())) thenReturn Seq()

      running(app) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registrationModel, Some(emptyUserAnswers))
        val controller = new Harness(periodService)

        val result = controller.callFilter(request).futureValue

        result.value mustEqual Redirect(routes.NoOtherPeriodsAvailableController.onPageLoad())
      }
    }
  }

}
