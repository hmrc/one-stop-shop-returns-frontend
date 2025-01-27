/*
 * Copyright 2025 HM Revenue & Customs
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

package viewmodels.previousReturn

import formats.Format.etmpDateTimeFormatter
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.all.currencyFormat
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

import java.time.{LocalDate, LocalDateTime}

object NewPreviousReturnSummary {

  def rowPayableVatDeclared(totalVatAmountDue: BigDecimal)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(SummaryListRowViewModel(
      key = "newPreviousReturn.summary.payableVatDeclared",
      value = ValueViewModel(HtmlContent(currencyFormat(totalVatAmountDue)))
        .withCssClass("govuk-table__cell--numeric")
    ))
  }

  def rowAmountLeftToPay(outstandingAmount: Option[BigDecimal])(implicit messages: Messages): Option[SummaryListRow] = {
    outstandingAmount.map { amount =>
      SummaryListRowViewModel(
        key = "newPreviousReturn.summary.amountRemaining",
        value = ValueViewModel(HtmlContent(currencyFormat(amount)))
          .withCssClass("govuk-table__cell--numeric govuk-!-padding-right-0")
      )
    }
  }

  def rowReturnSubmittedDate(dateSubmitted: LocalDateTime)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(SummaryListRowViewModel(
      key = "newPreviousReturn.summary.dateSubmitted",
      value = ValueViewModel(HtmlContent(dateSubmitted.format(etmpDateTimeFormatter)))
        .withCssClass("govuk-table__cell--numeric govuk-!-padding-right-0")
    ))
  }

  def rowPaymentDueDate(paymentDeadlineDate: LocalDate)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(SummaryListRowViewModel(
      key = "newPreviousReturn.summary.paymentDeadline",
      value = ValueViewModel(HtmlContent(paymentDeadlineDate.format(etmpDateTimeFormatter)))
        .withCssClass("govuk-table__cell--numeric govuk-!-padding-right-0")
    ))
  }

  def rowReturnReference(returnReference: String)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(SummaryListRowViewModel(
      key = "newPreviousReturn.summary.returnReference",
      value = ValueViewModel(HtmlContent(returnReference))
        .withCssClass("govuk-table__cell--numeric govuk-!-padding-right-0")
    ))
  }

  def rowPaymentReference(paymentReference: String)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(SummaryListRowViewModel(
      key = "newPreviousReturn.summary.paymentReference",
      value = ValueViewModel(HtmlContent(paymentReference))
        .withCssClass("govuk-table__cell--numeric govuk-!-padding-right-0")
    ))
  }
}
