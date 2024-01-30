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

package models.exclusions

import base.SpecBase
import models.Quarter._
import models.StandardPeriod
import uk.gov.hmrc.domain.Vrn


class ExcludedTraderSpec extends SpecBase {

  private val exclusionPeriod = StandardPeriod(2022, Q3)

  ".derriveExclusionSource" - {
    "must return 'HMRC' for exclusion reason 2 and 4" in {
      val exclusionReasons = Seq(2, 4)

      exclusionReasons.foreach { reason =>
        val excludedTrader = ExcludedTrader(Vrn("123456789"), reason, exclusionPeriod, None)
        excludedTrader.exclusionSource mustBe "HMRC"
      }
    }

    "must return 'Trader' for other exclusion reasons" in {
      val exclusionReasons = Seq(1, 3, 5, 6)

      exclusionReasons.foreach { reason =>
        val excludedTrader = ExcludedTrader(Vrn("123456789"), reason, exclusionPeriod, None)
        excludedTrader.exclusionSource mustBe "TRADER"
      }
    }

  }

}
