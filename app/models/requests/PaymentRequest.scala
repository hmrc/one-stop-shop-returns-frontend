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

package models.requests

import models.{Period, Quarter}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.domain.Vrn

case class PaymentRequest(vrn: Vrn, period: PaymentPeriod, amountInPence: Long)

object PaymentRequest {

  implicit val format: OFormat[PaymentRequest] = Json.format[PaymentRequest]
}

case class PaymentPeriod(year: Int, quarter: String)

object PaymentPeriod {

  def apply(period: Period): PaymentPeriod = {
    val year    = period.year
    val quarter = period.quarter match {
      case Quarter.Q1 => "JanuaryToMarch"
      case Quarter.Q2 => "AprilToJune"
      case Quarter.Q3 => "JulyToSeptember"
      case Quarter.Q4 => "OctoberToDecember"
    }

    PaymentPeriod(year, quarter)
  }

  implicit val format: OFormat[PaymentPeriod] = Json.format[PaymentPeriod]
}