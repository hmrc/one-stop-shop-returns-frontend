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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.LocalDate

case class VatRate(
                    rate: BigDecimal,
                    rateType: VatRateType,
                    validFrom: LocalDate,
                    validUntil: Option[LocalDate] = None
                  ) {

  lazy val rateForDisplay: String = if(rate.isWhole) {
    rate.toString.split('.').headOption.getOrElse(rate.toString) + "%"
  } else {
    rate.toString + "%"
  }

  lazy val asPercentage: BigDecimal =
    rate / 100
}

object VatRate {

  val stringReads: Reads[VatRate] = (
    (__ \ "rate").read[String].map(r => BigDecimal(r)) and
      (__ \ "rateType").read[VatRateType] and
      (__ \ "validFrom").read[LocalDate] and
      (__ \ "validUntil").readNullable[LocalDate]
    ) (VatRate.apply _)

  val decimalReads: Reads[VatRate] = (
    (__ \ "rate").read[BigDecimal] and
      (__ \ "rateType").read[VatRateType] and
      (__ \ "validFrom").read[LocalDate] and
      (__ \ "validUntil").readNullable[LocalDate]
    ) (VatRate.apply _)

  implicit val reads: Reads[VatRate] = decimalReads or stringReads

  implicit val writes: OWrites[VatRate] = new OWrites[VatRate] {

    override def writes(o: VatRate): JsObject = {

      val validUntilJson = o.validUntil.map {
        v =>
          Json.obj("validUntil" -> Json.toJson(v))
      }.getOrElse(Json.obj())

      Json.obj(
        "rate"      -> o.rate.toString,
        "rateType"  -> Json.toJson(o.rateType),
        "validFrom" -> Json.toJson(o.validFrom)
      ) ++ validUntilJson
    }
  }
}
sealed trait VatRateType

object VatRateType extends Enumerable.Implicits {

  case object Standard extends WithName("STANDARD") with VatRateType
  case object Reduced extends WithName("REDUCED") with VatRateType

  val values: Seq[VatRateType] = Seq(Standard, Reduced)

  implicit val enumerable: Enumerable[VatRateType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
