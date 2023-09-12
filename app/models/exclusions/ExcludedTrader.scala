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

import logging.Logging
import models.Period
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.domain.Vrn


case class ExcludedTrader(
                           vrn: Vrn,
                           exclusionReason: Int,
                           effectivePeriod: Period
                         ) {
  val exclusionSource: String = derriveExclusionSource(exclusionReason)

  private def derriveExclusionSource(code: Int) = {
    code match {
      case x if x == 2 || x == 4 => "HMRC"
      case _ => "TRADER"
    }
  }
}

object ExcludedTrader extends Logging {

  implicit val format: OFormat[ExcludedTrader] = Json.format[ExcludedTrader]

}
