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

package viewmodels.yourAccount

import models.financialdata.{Payment, PaymentStatus}
import play.api.i18n.Messages
import utils.CurrencyFormatter.currencyFormat
import utils.ReturnsUtils.isThreeYearsOld
import viewmodels.LinkModel

case class PaymentsViewModel(sections: Seq[PaymentsSection], warning: Option[String] = None, link: Option[LinkModel] = None)
case class PaymentsSection(contents: Seq[String], heading: Option[String] = None)

object PaymentsViewModel{
  def apply(duePayments: Seq[Payment],
            overduePayments: Seq[Payment],
            hasDueReturnThreeYearsOld: Boolean)(implicit messages: Messages): PaymentsViewModel = {
    if(duePayments.isEmpty && overduePayments.isEmpty){
      PaymentsViewModel(
        sections = Seq(PaymentsSection(
          contents = Seq(messages("index.payment.nothingOwed"))
        ))
      )
    } else {
      val duePaymentsSection = getPaymentsSection(duePayments, "due")
      val overduePaymentsSection = getPaymentsSection(overduePayments, "overdue")

      val link = if(hasDueReturnThreeYearsOld) {
        None
      } else {
        Some(
          LinkModel(
            linkText = messages("index.payment.makeAPayment"),
            id = "make-a-payment",
            url = controllers.routes.WhichVatPeriodToPayController.onPageLoad().url
          )
        )
      }

      PaymentsViewModel(
        sections = Seq(duePaymentsSection, overduePaymentsSection).flatten,
        warning = Some(messages("index.payment.pendingPayments")),
        link = link
      )
    }
  }

  private def getPaymentsSection(payments: Seq[Payment], key: String)(implicit messages: Messages) = {
    if(payments.nonEmpty){
      Some(
        PaymentsSection(
          heading = Some(messages(s"index.payment.${key}Heading")),
          contents = payments.map(
            payment => payment.paymentStatus match {
              case PaymentStatus.Unknown =>
                messages(
                  s"index.payment.${key}AmountMaybeOwed",
                  payment.period.displayShortText,
                  payment.period.paymentDeadlineDisplay
                )
              case _ => if(isThreeYearsOld(payment.dateDue)){
                messages(
                  "index.payment.amountOwedThreeYearsOld",
                  payment.period.displayShortText
                )
              } else {
                messages(
                  s"index.payment.${key}AmountOwed",
                  currencyFormat(payment.amountOwed),
                  payment.period.displayShortText,
                  payment.period.paymentDeadlineDisplay
                )
              }
            }
          )
        )
      )
    } else {
      None
    }
  }

}