/*
 * Copyright 2023 HM Revenue & Customs
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

package models.exclusions

import base.SpecBase
import connectors.VatReturnConnector
import models.Period
import models.Quarter.{Q2, Q3}
import models.registration.Registration
import models.requests.RegistrationRequest
import models.responses.NotFound
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.{Mockito, MockitoSugar}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.AnyContent
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ExcludedTraderSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockRegistrationRequest = mock[RegistrationRequest[AnyContent]]
  private val exclusionPeriod = Period(2022, Q3)

  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationRequest)
    super.beforeEach()
  }


  ".derriveExclusionSource" - {
    "must return 'HMRC' for exclusion reason 2 and 4" in {
      val exclusionReasons = Seq(2, 4)

      exclusionReasons.foreach { reason =>
        val excludedTrader = ExcludedTrader(Vrn("123456789"), reason, exclusionPeriod)
        excludedTrader.exclusionSource mustBe "HMRC"
      }
    }

    "must return 'Trader' for other exclusion reasons" in {
      val exclusionReasons = Seq(1, 3, 5, 6)

      exclusionReasons.foreach { reason =>
        val excludedTrader = ExcludedTrader(Vrn("123456789"), reason, exclusionPeriod)
        excludedTrader.exclusionSource mustBe "TRADER"
      }
    }

  }

}
