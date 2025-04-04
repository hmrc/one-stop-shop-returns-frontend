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

package models.emails

import play.api.libs.json.{JsResult, JsValue, Json, Reads, Writes}

sealed trait EmailParameters

object EmailParameters {
  implicit val reads: Reads[EmailParameters] = new Reads[EmailParameters] {
    override def reads(json: JsValue): JsResult[EmailParameters] = {
      json.validate[ReturnsConfirmationEmailParameters].orElse(
        json.validate[ReturnsConfirmationEmailNoVatOwedParameters]
      )
    }
  }
  implicit val writes: Writes[EmailParameters] = Writes[EmailParameters] {
    case returnsEmailConfirmation: ReturnsConfirmationEmailParameters =>
      Json.toJson(returnsEmailConfirmation)(ReturnsConfirmationEmailParameters.writes)
    case nilReturnEmailConfirmation : ReturnsConfirmationEmailNoVatOwedParameters =>
      Json.toJson(nilReturnEmailConfirmation)(ReturnsConfirmationEmailNoVatOwedParameters.writes)
  }
}

case class ReturnsConfirmationEmailParameters(
   recipientName_line1: String,
   businessName:String,
   period: String,
   paymentDeadline: String
) extends EmailParameters

object ReturnsConfirmationEmailParameters {
  implicit val writes: Writes[ReturnsConfirmationEmailParameters] =
    Json.writes[ReturnsConfirmationEmailParameters]
  implicit val reads: Reads[ReturnsConfirmationEmailParameters] =
    Json.reads[ReturnsConfirmationEmailParameters]
}

case class ReturnsConfirmationEmailNoVatOwedParameters(
  recipientName_line1: String,
  period: String
) extends EmailParameters

object ReturnsConfirmationEmailNoVatOwedParameters {
  implicit val writes: Writes[ReturnsConfirmationEmailNoVatOwedParameters] =
    Json.writes[ReturnsConfirmationEmailNoVatOwedParameters]
  implicit val reads: Reads[ReturnsConfirmationEmailNoVatOwedParameters] =
    Json.reads[ReturnsConfirmationEmailNoVatOwedParameters]
}
