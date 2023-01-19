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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.LocalDate

case class VatRateAndSales(
                    rate: BigDecimal,
                    rateType: VatRateType,
                    validFrom: LocalDate,
                    validUntil: Option[LocalDate] = None,
                    sales: Option[SalesAtVatRate]
                  ) {

  lazy val rateForDisplay: String = if(rate.isWhole) {
    rate.toString.split('.').headOption.getOrElse(rate.toString) + "%"
  } else {
    rate.toString + "%"
  }
}

object VatRateAndSales {

  def convert(vatRate: VatRate): VatRateAndSales =
    new VatRateAndSales(vatRate.rate, vatRate.rateType, vatRate.validFrom, vatRate.validUntil, None)

  val stringReads: Reads[VatRateAndSales] = (
    (__ \ "rate").read[String].map(r => BigDecimal(r)) and
    (__ \ "rateType").read[VatRateType] and
    (__ \ "validFrom").read[LocalDate] and
    (__ \ "validUntil").readNullable[LocalDate] and
    (__  \ "salesAtVatRate").readNullable[SalesAtVatRate]
  ) (VatRateAndSales.apply _)

  val decimalReads: Reads[VatRateAndSales] = (
    (__ \ "rate").read[BigDecimal] and
    (__ \ "rateType").read[VatRateType] and
    (__ \ "validFrom").read[LocalDate] and
    (__ \ "validUntil").readNullable[LocalDate] and
    (__  \ "salesAtVatRate").readNullable[SalesAtVatRate]
  ) (VatRateAndSales.apply _)

  implicit val reads: Reads[VatRateAndSales] = decimalReads or stringReads

  implicit val bigDecimalWrites: Writes[BigDecimal] = new Writes[BigDecimal] {
    override def writes(o: BigDecimal): JsValue = {
      JsString.apply(o.toString())
    }
  }

  implicit val writes: OWrites[VatRateAndSales] = (
      (__ \ "rate").write[BigDecimal] and
        (__ \ "rateType").write[VatRateType] and
        (__ \ "validFrom").write[LocalDate] and
        (__ \ "validUntil").writeNullable[LocalDate] and
        (__  \ "salesAtVatRate").writeNullable[SalesAtVatRate]
      ) (unlift(VatRateAndSales.unapply))
  }
