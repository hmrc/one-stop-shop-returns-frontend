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

package models.domain

import models.{Enumerable, WithName}
import play.api.libs.json._

sealed trait EuTaxIdentifierType

object EuTaxIdentifierType extends Enumerable.Implicits {

  case object Vat extends WithName("vat") with EuTaxIdentifierType
  case object Other extends WithName("other") with EuTaxIdentifierType

  val values: Seq[EuTaxIdentifierType] =
    Seq(Vat, Other)

  implicit val enumerable: Enumerable[EuTaxIdentifierType] =
    Enumerable(values.map(v => v.toString -> v): _*)

  implicit def reads: Reads[EuTaxIdentifierType] = Reads[EuTaxIdentifierType] {
    case JsString(Vat.toString)   => JsSuccess(Vat)
    case JsString(Other.toString) => JsSuccess(Other)
    case _                        => JsError("error.invalid")
  }

  implicit def writes: Writes[EuTaxIdentifierType] = {
    Writes(value => JsString(value.toString))
  }
}
