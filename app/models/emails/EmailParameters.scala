/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.{Json, Reads, Writes}

sealed trait EmailParameters

object EmailParameters {
  implicit val writes: Writes[EmailParameters] = Writes[EmailParameters] {
    case returnsEmailConfirmation: ReturnsConfirmationEmailParameters =>
      Json.toJson(returnsEmailConfirmation)(ReturnsConfirmationEmailParameters.writes)
  }

  implicit val reads: Reads[EmailParameters] = Json.reads[EmailParameters]
}

case class ReturnsConfirmationEmailParameters(
   recipientName_line1: String,
   businessName:String,
   period: String,
   paymentDeadline: String,
   VATOwed: String,
   reference: String
) extends EmailParameters

object ReturnsConfirmationEmailParameters {
  implicit val writes: Writes[ReturnsConfirmationEmailParameters] =
    Json.writes[ReturnsConfirmationEmailParameters]
  implicit val reads: Reads[ReturnsConfirmationEmailParameters] =
    Json.reads[ReturnsConfirmationEmailParameters]
}
