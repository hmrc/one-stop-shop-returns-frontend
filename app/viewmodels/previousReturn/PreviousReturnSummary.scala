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

package viewmodels.previousReturn

import formats.Format
import models.domain.VatReturn
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.Key
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CurrencyFormatter
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object PreviousReturnSummary extends CurrencyFormatter {

  def totalVatSummaryRows(totalVatOwed: BigDecimal)(implicit messages: Messages): Seq[SummaryListRow] = {
    Seq(SummaryListRowViewModel(
      key = "previousReturn.correction.vatDeclared.totalVatOwed",
      value = ValueViewModel(HtmlContent(currencyFormat(totalVatOwed)))
        .withCssClass("govuk-!-font-weight-bold")
    ))
  }

  def mainListRows(vatReturn: VatReturn, vatOwed: BigDecimal, clearedAmount: Option[BigDecimal], amountOutstanding: Option[BigDecimal])(implicit messages: Messages): Seq[SummaryListRow] = {
    Seq(
      vatOwedRow(vatOwed),
      clearedAmountRow(clearedAmount),
      amountOutstandingRow(amountOutstanding),
      dateSubmittedRow(vatReturn),
      dateDueRow(vatReturn),
      returnReferenceNumber(vatReturn),
      paymentReferenceNumber(vatReturn)
    ).flatten
  }

  private[this] def vatOwedRow(vatOwed: BigDecimal)(implicit messages: Messages): Option[SummaryListRow] = {
      Some(SummaryListRowViewModel(
        key = Key("previousReturn.vatOwed.label")
          .withCssClass("govuk-!-width-one-half"),
        value = ValueViewModel(HtmlContent(currencyFormat(vatOwed)))
          .withCssClass("govuk-!-width-one-half")
      ))
  }

  private[this] def clearedAmountRow(clearedAmount: Option[BigDecimal])(implicit messages: Messages): Option[SummaryListRow] = {
    clearedAmount.map(amount =>
    SummaryListRowViewModel(
      key = Key("previousReturn.clearedAmount.label")
        .withCssClass("govuk-!-width-one-half"),
      value = ValueViewModel(HtmlContent(currencyFormat(amount)))
        .withCssClass("govuk-!-width-one-half")
    ))
  }

  private[this] def amountOutstandingRow(amountOutstanding: Option[BigDecimal])(implicit messages: Messages): Option[SummaryListRow] = {
    amountOutstanding.map(outstandingAmount =>
    SummaryListRowViewModel(
      key = Key("previousReturn.amountOutstanding.label")
        .withCssClass("govuk-!-width-one-half"),
      value = ValueViewModel(HtmlContent(currencyFormat(outstandingAmount)))
        .withCssClass("govuk-!-width-one-half")
    ))
  }

  private[this] def dateSubmittedRow(vatReturn: VatReturn)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(SummaryListRowViewModel(
      key = "previousReturn.dateSubmitted.label",
      value = ValueViewModel(
        HtmlContent(Format.localDateFormatter.format(vatReturn.submissionReceived)))
    ))
  }

  private[this] def dateDueRow(vatReturn: VatReturn)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(SummaryListRowViewModel(
      key = "previousReturn.dateDue.label",
      value = ValueViewModel(
        HtmlContent(Format.localDateFormatter.format(vatReturn.period.paymentDeadline)))
    ))
  }

  private[this] def returnReferenceNumber(vatReturn: VatReturn)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(SummaryListRowViewModel(
      key = "previousReturn.returnReference.label",
      value = ValueViewModel(HtmlFormat.escape(vatReturn.reference.value).toString)
    ))
  }

  private[this] def paymentReferenceNumber(vatReturn: VatReturn)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(SummaryListRowViewModel(
      key = "previousReturn.paymentReference.label",
      value = ValueViewModel(HtmlFormat.escape(vatReturn.paymentReference.value).toString)
    ))
  }
}
