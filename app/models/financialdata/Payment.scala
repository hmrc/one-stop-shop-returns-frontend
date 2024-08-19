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

package models.financialdata

import models.StandardPeriod
import play.api.i18n.Messages
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

import java.time.LocalDate

case class Payment(period: StandardPeriod,
                   amountOwed: BigDecimal,
                   dateDue: LocalDate,
                   paymentStatus: PaymentStatus
                  ) {
  val showPayNow: Boolean = amountOwed > 0

  val showUpdating: Boolean = paymentStatus == PaymentStatus.Unknown
}

object Payment {
  implicit val formatPayment: Format[Payment] = Json.format[Payment]

  def options(payments: Seq[Payment])(implicit messages: Messages): Seq[RadioItem] = {

    lazy val hasUnknownPayments: Boolean = payments.exists(_.paymentStatus == PaymentStatus.Unknown)

    def getLabel(payment: Payment): String =
      if (hasUnknownPayments) {
        payment.period.displayText
      } else {
        messages("whichVatPeriodToPay.amountKnown", payment.amountOwed, payment.period.displayShortText)
      }

    payments.zipWithIndex.map {
      case (value, index) =>
        RadioItem(
          content = HtmlContent(getLabel(value)),
          value = Some(value.period.toString),
          id = Some(s"value_$index")
        )
    }
  }
}

