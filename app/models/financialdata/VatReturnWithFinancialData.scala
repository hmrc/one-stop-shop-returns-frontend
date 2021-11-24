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

package models.financialdata

import models.PaymentState
import models.PaymentState.{NoneDue, Paid, PaymentDue}
import models.corrections.CorrectionPayload
import models.domain.VatReturn
import play.api.libs.json.{Json, OFormat}

case class VatReturnWithFinancialData(
                                       vatReturn: VatReturn,
                                       charge: Option[Charge],
                                       vatOwed: Option[Long],
                                       corrections: Option[CorrectionPayload]
                                     ){

  val showPayNow: Boolean = (vatOwed.isDefined && vatOwed.getOrElse(0L) > 0L) &&
    (charge.isEmpty || charge.exists(c => c.outstandingAmount > 0))

  val showUpdating: Boolean = charge.isEmpty && vatOwed.getOrElse(0L) > 0L

  def paymentState: PaymentState = {
    if(showPayNow) {
      PaymentDue
    } else if(charge.exists(c => c.originalAmount > 0 && c.outstandingAmount == 0)) {
      Paid
    } else {
      NoneDue
    }
  }
}

object VatReturnWithFinancialData {
  implicit val format: OFormat[VatReturnWithFinancialData] = Json.format[VatReturnWithFinancialData]
}


