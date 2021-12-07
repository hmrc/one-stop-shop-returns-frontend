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

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import utils.CurrencyFormatter
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object TotalSalesSummary extends CurrencyFormatter {

  def rows(netSalesFromNi: BigDecimal,
           netSalesFromEu: BigDecimal,
           vatOnSalesFromNi: BigDecimal,
           vatOnSalesFromEu: BigDecimal,
           totalVatOnSales: BigDecimal,
           showCorrections: Boolean)(implicit messages: Messages): Seq[SummaryListRow] = {
    Seq(
      netSalesFromNiRow(netSalesFromNi),
      netSalesFromEuRow(netSalesFromEu),
      vatOnSalesFromNiRow(vatOnSalesFromNi),
      vatOnSalesFromEuRow(vatOnSalesFromEu),
      totalVatOnSalesRow(totalVatOnSales, showCorrections)
    ).flatten
  }

  private[this] def netSalesFromNiRow(netSalesFromNi: BigDecimal)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(SummaryListRowViewModel(
      key = Key("previousReturn.netSalesFromNi.label")
        .withCssClass("govuk-!-font-weight-regular")
        .withCssClass("govuk-!-width-two-thirds"),
      value = ValueViewModel(HtmlContent(currencyFormat(netSalesFromNi)))
        .withCssClass("govuk-!-width-one-third"),
      actions = Seq.empty
    ))
  }

  private[this] def netSalesFromEuRow(netSalesFromEu: BigDecimal)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(SummaryListRowViewModel(
      key = Key("previousReturn.netSalesFromEu.label")
        .withCssClass("govuk-!-font-weight-regular"),
      value = ValueViewModel(HtmlContent(currencyFormat(netSalesFromEu))),
      actions = Seq.empty
    ))
  }

  private[this] def vatOnSalesFromNiRow(vatOnSalesFromNi: BigDecimal)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(SummaryListRowViewModel(
      key = Key("previousReturn.vatOnSalesFromNi.label")
        .withCssClass("govuk-!-font-weight-regular"),
      value = ValueViewModel(HtmlContent(currencyFormat(vatOnSalesFromNi))),
      actions = Seq.empty
    ))
  }

  private[this] def vatOnSalesFromEuRow(vatOnSalesFromEu: BigDecimal)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(SummaryListRowViewModel(
      key = Key("previousReturn.vatOnSalesFromEu.label")
        .withCssClass("govuk-!-font-weight-regular"),
      value = ValueViewModel(HtmlContent(currencyFormat(vatOnSalesFromEu))),
      actions = Seq.empty
    ))
  }

  private[this] def totalVatOnSalesRow(totalVatOnSales: BigDecimal, showCorrections: Boolean)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(SummaryListRowViewModel(
      key =
        if (showCorrections) {
          Key("previousReturn.totalVatOnSalesWithCorrections.label")
        } else {
          Key("previousReturn.totalVatOnSales.label")
        },
      value = ValueViewModel(HtmlContent(currencyFormat(totalVatOnSales)))
        .withCssClass("govuk-!-font-weight-bold"),
      actions = Seq.empty
    ))
  }

}
