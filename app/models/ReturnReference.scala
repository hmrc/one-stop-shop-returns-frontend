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

import play.api.libs.json._
import uk.gov.hmrc.domain.Vrn

import scala.util.matching.Regex

case class ReturnReference private(value: String)

object ReturnReference {

  private val pattern: Regex = """XI/XI(\d{9})/(Q[1-4])\.(\d{4})""".r.anchored

  def apply(vrn: Vrn, period: Period): ReturnReference =
    ReturnReference(s"XI/XI${vrn.vrn}/${period.quarter.toString}.${period.year}")

  private[models] def fromString(string: String): Option[ReturnReference] =
    string match {
      case pattern(vrn, quarter, year) =>
        Period(year, quarter)
          .toOption
          .map(period => ReturnReference(Vrn(vrn), period))
      case _ =>
        None
    }

  implicit def writes: Writes[ReturnReference] = new Writes[ReturnReference] {
    override def writes(o: ReturnReference): JsValue =
      JsString(o.value)
  }

  implicit def reads: Reads[ReturnReference] = new Reads[ReturnReference] {
    override def reads(json: JsValue): JsResult[ReturnReference] =
      json match {
        case JsString(string) => fromString(string) match {
          case Some(reference) => JsSuccess(reference)
          case None            => JsError("Return reference is not in the correct format")
        }
        case _ =>
          JsError("Return reference is not a JsString")
      }
  }
}
