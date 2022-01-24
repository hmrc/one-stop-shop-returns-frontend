/*
 * Copyright 2022 HM Revenue & Customs
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

import models.domain.{SalesToCountry, VatReturn}
import models.Country
import models.Country.northernIreland
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import utils.CurrencyFormatter
import viewmodels.govuk.table._
import viewmodels.TitledTable

object SaleAtVatRateSummary extends CurrencyFormatter {

  def getAllNiSales(vatReturn: VatReturn)(implicit messages: Messages): Seq[TitledTable] = {
    for {
      saleFromNi <- vatReturn.salesFromNi
      countryFrom = northernIreland
    } yield summaryRowsOfSales(countryFrom, Seq(saleFromNi))
  }

  def getAllEuSales(vatReturn: VatReturn)(implicit messages: Messages): Seq[TitledTable] = {
    (for {
      saleFromEu <- vatReturn.salesFromEu
      countryFrom = saleFromEu.countryOfSale
    } yield summaryRowsOfSales(countryFrom, saleFromEu.sales))
  }

  private[this] def summaryRowsOfSales(salesFromCountry: Country, salesToCountry: Seq[SalesToCountry])(implicit messages: Messages): TitledTable = {

    TitledTable(
      title = messages("previousReturn.salesAtVatRate.title", salesFromCountry.name),
      list = TableViewModel(
        rows = Seq(headers) ++ rows(salesToCountry)
      )
    )
  }

  private[this] def rows(salesToCountry: Seq[SalesToCountry])(implicit messages: Messages): Seq[Seq[TableRow]] = {
    for {
      saleToCountry <- salesToCountry
      countryOfConsumption = saleToCountry.countryOfConsumption.name
      amount <- saleToCountry.amounts
    } yield {
      Seq(
        TableRowViewModel(
          content = Text(countryOfConsumption)
        ),
        TableRowViewModel(
          content = Text(amount.vatRate.rateForDisplay)
        ).withCssClass("govuk-table__cell--numeric"),
        TableRowViewModel(
          content = HtmlContent(currencyFormat(amount.netValueOfSales))
        ).withCssClass("govuk-table__cell--numeric"),
        TableRowViewModel(
          content = HtmlContent(currencyFormat(amount.vatOnSales.amount))
        ).withCssClass("govuk-table__cell--numeric")
      )
    }
  }

  private def headers()(implicit messages: Messages): Seq[TableRow] =
    Seq(
      TableRowViewModel(
        content = Text(messages("previousReturn.salesAtVatRate.toCountry.label"))
      ).withCssClass("govuk-!-font-weight-bold")
        .withCssClass("oss-to-country"),
      TableRowViewModel(
        content = Text(messages("previousReturn.salesAtVatRate.vatRate.label"))
      ).withCssClass("govuk-!-font-weight-bold")
        .withCssClass("oss-vat-rate"),
      TableRowViewModel(
        content = Text(messages("previousReturn.saleAtVatRate.netValueOfSales.label"))
      ).withCssClass("govuk-!-font-weight-bold")
        .withCssClass("oss-sales-excluding-vat"),
      TableRowViewModel(
        content = Text(messages("previousReturn.saleAtVatRate.vatOnSales.label"))
      ).withCssClass("govuk-!-font-weight-bold")
        .withCssClass("oss-vat")
    )
}
