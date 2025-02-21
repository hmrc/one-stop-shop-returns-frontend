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
import models.Quarter.*
import models.StandardPeriod
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.domain.Vrn

import java.time.LocalDate

class ExcludedTraderSpec extends SpecBase {

  private val exclusionPeriod = StandardPeriod(2022, Q3)

  ".derriveExclusionSource" - {
    "must return 'HMRC' for exclusion reason 2 and 4" in {
      val exclusionReasons = Seq(ExclusionReason.CeasedTrade, ExclusionReason.FailsToComply)

      exclusionReasons.foreach { reason =>
        val excludedTrader = ExcludedTrader(Vrn("123456789"), reason, exclusionPeriod.firstDay, quarantined = false)
        excludedTrader.exclusionReason.exclusionSource mustBe HMRC
      }
    }

    "must return 'Trader' for other exclusion reasons" in {
      val exclusionReasons = Seq(
        ExclusionReason.NoLongerSupplies,
        ExclusionReason.NoLongerMeetsConditions,
        ExclusionReason.VoluntarilyLeaves,
        ExclusionReason.TransferringMSID
      )

      exclusionReasons.foreach { reason =>
        val excludedTrader = ExcludedTrader(Vrn("123456789"), reason, exclusionPeriod.firstDay, quarantined = false)
        excludedTrader.exclusionReason.exclusionSource mustBe TRADER
      }
    }

  }

  "ExcludedTrader" - {
    "must serialise and deserialise correctly" in {

      val vrn: Vrn = Vrn("123456789")
      val exclusionReason: ExclusionReason = ExclusionReason.Reversal
      val effectiveDate: LocalDate = LocalDate.of(2021,2,2)
      val quarantined: Boolean = true


      val json = Json.obj(
        "vrn" -> Vrn("123456789"),
        "exclusionReason" -> ExclusionReason.Reversal.toString,
        "effectiveDate" -> LocalDate.of(2021,2,2),
        "quarantined" -> true
      )

      val expectedResult = ExcludedTrader(vrn, exclusionReason, effectiveDate, quarantined)

      Json.toJson(expectedResult) mustBe json
      json.validate[ExcludedTrader] mustBe JsSuccess(expectedResult)
    }
  }

}
