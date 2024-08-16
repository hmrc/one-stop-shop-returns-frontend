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
import models.requests.RegistrationRequest
import play.api.i18n.Messages
import utils.CurrencyFormatter.currencyFormat
import utils.ReturnsUtils.isThreeYearsOld
import viewmodels.LinkModel

import java.time.Clock

case class PaymentsViewModel(sections: Seq[PaymentsSection], warning: Option[String] = None, link: Option[LinkModel] = None)
case class PaymentsSection(contents: Seq[String], heading: Option[String] = None)

object PaymentsViewModel {
  def apply(duePayments: Seq[Payment],
            overduePayments: Seq[Payment],
            excludedPayments: Seq[Payment],
            hasDueReturnThreeYearsOld: Boolean)(implicit messages: Messages, clock: Clock, request: RegistrationRequest[_]): PaymentsViewModel = {
    if(duePayments.isEmpty && overduePayments.isEmpty && excludedPayments.isEmpty){
      PaymentsViewModel(
        sections = Seq(PaymentsSection(
          contents = Seq(messages("index.payment.nothingOwed"))
        ))
      )
    } else {
      val duePaymentsSection = getPaymentsSection(
        Some(messages(s"index.payment.dueHeading")), duePayments, "due"
      )

      val overduePaymentsSection = getPaymentsSection(
        Some(messages(s"index.payment.overdueHeading")), overduePayments, "overdue")

      val excludedPaymentsSection =
        getPaymentsSection(None, excludedPayments, "excluded")

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
        sections = Seq(excludedPaymentsSection, duePaymentsSection, overduePaymentsSection).flatten,
        warning = Some(messages("index.payment.pendingPayments")),
        link = link
      )
    }
  }

  private def getPaymentsSection(
                                  heading: Option[String],
                                  payments: Seq[Payment],
                                  key: String
                                )(implicit messages: Messages, clock: Clock, request: RegistrationRequest[_]) = {
    if(payments.nonEmpty){
      Some(
        PaymentsSection(
          heading = heading,
          contents = payments.map(
            payment => payment.paymentStatus match {
              case PaymentStatus.Unknown =>
                messages(
                  s"index.payment.${key}AmountMaybeOwed",
                  payment.period.displayShortText,
                  payment.period.paymentDeadlineDisplay
                )
              case _ => if(request.registration.excludedTrader.exists(_.isExcludedNotReversed) && isThreeYearsOld(payment.dateDue)){
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