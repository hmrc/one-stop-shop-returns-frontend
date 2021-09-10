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

import models.domain.{SalesDetails, SalesToCountry, VatReturn}
import models.Country.northernIreland
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import utils.CurrencyFormatter._
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
import viewmodels.TitledSummaryList

object SaleAtVatRateSummary {

  def rows(saleAtVatRate: SalesDetails)(implicit messages: Messages): Seq[SummaryListRow] = {

    Seq(
      SummaryListRowViewModel(
        key     = Key("previousReturn.saleAtVatRate.netValueOfSales.label")
          .withCssClass("govuk-!-font-weight-regular")
          .withCssClass("govuk-!-width-one-half"),
        value   = ValueViewModel(HtmlContent(currencyFormat(saleAtVatRate.netValueOfSales)))
          .withCssClass("govuk-!-width-one-half"),
      ),
      SummaryListRowViewModel(
        key     = Key("previousReturn.saleAtVatRate.vatOnSales.label")
          .withCssClass("govuk-!-font-weight-regular"),
        value   = ValueViewModel(HtmlContent(currencyFormat(saleAtVatRate.vatOnSales))),
      )
    )

  }

  def getAllNiSales(vatReturn: VatReturn)(implicit messages: Messages): Map[String, Seq[TitledSummaryList]] = {
    (for {
      saleFromNi <- vatReturn.salesFromNi
      countryFrom = northernIreland
      countryTo = saleFromNi.countryOfConsumption
    } yield Map(
      messages("previousReturn.salesAtVatRate.title", countryFrom.name, countryTo.name) ->
        summaryRowsOfSales(saleFromNi)
    )).flatten.toMap
  }

  def getAllEuSales(vatReturn: VatReturn)(implicit messages: Messages): Map[String, Seq[TitledSummaryList]] = {
    (for {
      saleFromEu <- vatReturn.salesFromEu
      countryFrom = saleFromEu.countryOfSale
      saleToCountry <- saleFromEu.sales
      countryTo = saleToCountry.countryOfConsumption
    } yield Map(
      messages("previousReturn.salesAtVatRate.title", countryFrom.name, countryTo.name) ->
        summaryRowsOfSales(saleToCountry))).flatten.toMap
  }

  private[this] def summaryRowsOfSales(saleToCountry: SalesToCountry)(implicit messages: Messages): Seq[TitledSummaryList] = {
    for {
      amount <- saleToCountry.amounts
    } yield TitledSummaryList(
      title = messages("previousReturn.salesAtVatRate.vatRate", amount.vatRate.rateForDisplay),
      list = SummaryListViewModel(
        rows = SaleAtVatRateSummary.rows(amount)
      )
    )
  }
}
