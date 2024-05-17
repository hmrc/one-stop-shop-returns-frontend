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

import logging.Logging
import models.{Enumerable, StandardPeriod, WithName}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.domain.Vrn

import java.time.LocalDate

  case class ExcludedTrader(
                           vrn: Vrn,
                           exclusionReason: Int,
                           effectivePeriod: StandardPeriod,
                           effectiveDate: LocalDate
                         ) {
  val exclusionSource: ExclusionSource = deriveExclusionSource(exclusionReason)

  private def deriveExclusionSource(code: Int): ExclusionSource = {
    code match {
      case x if x == 2 || x == 4 => HMRC
      case _ => TRADER
    }
  }
}

sealed trait ExclusionSource
object HMRC extends ExclusionSource
object TRADER extends ExclusionSource

object ExcludedTrader extends Logging {

  implicit val format: OFormat[ExcludedTrader] = Json.format[ExcludedTrader]

  implicit class ExcludedTraderHelper(excludedTrader: ExcludedTrader) {

    def hasRequestedToLeave: Boolean = {
      val exclusionSource = excludedTrader.exclusionSource

      if (exclusionSource == TRADER) {
        LocalDate.now().isBefore(excludedTrader.effectiveDate)
      } else {
        false
      }
    }
  }
}

sealed trait ExclusionReason {
  val exclusionSource: ExclusionSource
  val numberValue: Int
}

object ExclusionReason extends Enumerable.Implicits {

  case object Reversal extends WithName("-1")  with ExclusionReason {
    val exclusionSource: ExclusionSource = HMRC
    val numberValue: Int = -1
  }

  case object NoLongerSupplies extends WithName("1") with ExclusionReason {
    val exclusionSource: ExclusionSource = TRADER
    val numberValue: Int = 1
  }

  case object CeasedTrade extends WithName("2") with ExclusionReason {
    val exclusionSource: ExclusionSource = HMRC
    val numberValue: Int = 2
  }

  case object NoLongerMeetsConditions extends WithName("3") with ExclusionReason {
    val exclusionSource: ExclusionSource = TRADER
    val numberValue: Int = 3
  }

  case object FailsToComply extends WithName("4") with ExclusionReason {
    val exclusionSource: ExclusionSource = HMRC
    val numberValue: Int = 4
  }

  case object VoluntarilyLeaves extends WithName("5") with ExclusionReason {
    val exclusionSource: ExclusionSource = TRADER
    val numberValue: Int = 5
  }

  case object TransferringMSID extends WithName("6") with ExclusionReason {
    val exclusionSource: ExclusionSource = TRADER
    val numberValue: Int = 6
  }

  val values: Seq[ExclusionReason] = Seq(
    Reversal,
    NoLongerSupplies,
    CeasedTrade,
    NoLongerMeetsConditions,
    FailsToComply,
    VoluntarilyLeaves,
    TransferringMSID
  )

  implicit val enumerable: Enumerable[ExclusionReason] =
    Enumerable(values.map(v => v.toString -> v): _*)
}