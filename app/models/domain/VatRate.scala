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

package models.domain

import models.{Enumerable, WithName}
import play.api.libs.json.{Json, OFormat}

case class VatRate(rate: BigDecimal, rateType: VatRateType) {
  lazy val rateForDisplay: String = if(rate.isWhole) {
    rate.toString.split('.').headOption.getOrElse(rate.toString) + "%"
  } else {
    rate.toString + "%"
  }
}

object VatRate {

  implicit val format: OFormat[VatRate] = Json.format[VatRate]
}

sealed trait VatRateType

object VatRateType extends Enumerable.Implicits {

  case object Standard extends WithName("STANDARD") with VatRateType
  case object Reduced extends WithName("REDUCED") with VatRateType

  val values: Seq[VatRateType] = Seq(Standard, Reduced)

  implicit val enumerable: Enumerable[VatRateType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
