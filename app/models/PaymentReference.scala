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

import play.api.libs.json._
import uk.gov.hmrc.domain.Vrn

import scala.util.matching.Regex

case class PaymentReference private(value: String)

object PaymentReference {

  private val pattern: Regex = """NI(\d{9})(Q[1-4])(\d{2})""".r.anchored

  def apply(vrn: Vrn, period: Period): PaymentReference =
    PaymentReference(s"NI${vrn.vrn}${period.quarter.toString}${twoDigitYear(period.year)}")

  private[models] def fromString(string: String): Option[PaymentReference] =
    string match {
      case pattern(vrn, quarter, year) =>
        Period(year, quarter)
          .toOption
          .map(period => PaymentReference(Vrn(vrn), period))
      case _ =>
        None
    }

  private def twoDigitYear(year: Int): String = year.toString takeRight 2

  implicit def writes: Writes[PaymentReference] = new Writes[PaymentReference] {
    override def writes(o: PaymentReference): JsValue =
      JsString(o.value)
  }

  implicit def reads: Reads[PaymentReference] = new Reads[PaymentReference] {
    override def reads(json: JsValue): JsResult[PaymentReference] =
      json match {
        case JsString(string) => fromString(string) match {
          case Some(reference) => JsSuccess(reference)
          case None            => JsError("Payment reference is not in the correct format")
        }
        case _ =>
          JsError("Payment reference is not a JsString")
      }
  }
}
