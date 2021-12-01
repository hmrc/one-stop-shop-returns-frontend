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

package viewmodels.previousReturn.corrections

import models.corrections.{CorrectionPayload, CorrectionToCountry}
import models.domain.VatReturn
import play.api.i18n.Messages
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
import viewmodels.TitledSummaryList
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import utils.CurrencyFormatter._


object CorrectionSummary {

  def getCorrectionPeriods(maybeCorrectionPayload: Option[CorrectionPayload])(implicit messages: Messages): Map[String, Seq[TitledSummaryList]] = {
    maybeCorrectionPayload match {
      case Some(correctionPayload) =>
        (for {
          correction <- correctionPayload.corrections
        } yield Map(
          messages("previousReturn.correction.correctionPeriod.title", correction.correctionReturnPeriod.displayText) ->
            summaryRowsOfCorrectionToCountry(correction.correctionsToCountry)
        )).flatten.toMap
      case _ => Map.empty
    }
  }

  private[this] def summaryRowsOfCorrectionToCountry(correctionsToCountry: List[CorrectionToCountry])(implicit messages: Messages): Seq[TitledSummaryList] = {
    for {
      correctionToCountry <- correctionsToCountry
    } yield TitledSummaryList(
      title = messages("previousReturn.correction.correctionToCountry.title", correctionToCountry.correctionCountry.name),
      list = SummaryListViewModel(
        rows = summaryRowsOfCountryVatCorrection(correctionToCountry.countryVatCorrection)
      )
    )
  }

  def getDeclaredVatAfterCorrections(maybeCorrectionPayload: Option[CorrectionPayload], vatReturn: VatReturn)(implicit messages: Messages): Seq[TitledSummaryList] = {
    maybeCorrectionPayload match {
      case Some(correctionPayload) =>
        val correctionAmountsToCountries = groupByCountry(correctionPayload, vatReturn)
        val negativeAndZeroValues = correctionAmountsToCountries.filter(_.countryVatCorrection <= 0)
        val positiveValues = correctionAmountsToCountries.filter(_.countryVatCorrection > 0)

        summaryRowsOfNegativeAndZeroValues(negativeAndZeroValues) ++
          summaryRowsOfPositiveValues(positiveValues)
      case _ => Seq.empty
    }
  }

  private[this] def summaryRowsOfValues(correctionsToCountry: List[CorrectionToCountry])(implicit messages: Messages): List[SummaryListRow] = {

    if (correctionsToCountry.nonEmpty) {
      val columnHeading =
        SummaryListRowViewModel(
          key = Key("previousReturn.correction.vatDeclared.countryLabel")
            .withCssClass("govuk-!-font-weight-bold"),
          value = ValueViewModel(messages("previousReturn.correction.vatDeclared.countryAmountLabel"))
            .withCssClass("govuk-!-font-weight-bold"),
        )

      val items = for {
        correctionToCountry <- correctionsToCountry
      } yield {
        SummaryListRowViewModel(
          key = Key(HtmlContent(correctionToCountry.correctionCountry.name))
            .withCssClass("govuk-!-font-weight-regular"),
          value = ValueViewModel(HtmlContent(currencyFormat(correctionToCountry.countryVatCorrection))),
        )
      }

      List(columnHeading) ++ items
    } else {
      List.empty
    }
  }

  private[this] def summaryRowsOfNegativeAndZeroValues(correctionsToCountry: List[CorrectionToCountry])(implicit messages: Messages): Seq[TitledSummaryList] = {
    Seq(
      TitledSummaryList(
        title = messages("previousReturn.correction.vatDeclared.noPaymentDue.title"),
        list = SummaryListViewModel(
          rows = summaryRowsOfValues(correctionsToCountry)
        ),
        hint = Some(messages("previousReturn.correction.vatDeclared.noPaymentDue.hint"))
      )
    )
  }

  private[this] def summaryRowsOfPositiveValues(correctionsToCountry: List[CorrectionToCountry])(implicit messages: Messages): Seq[TitledSummaryList] = {
    Seq(TitledSummaryList(
      title = messages("previousReturn.correction.vatDeclared.paymentDue.title"),
      list = SummaryListViewModel(
        rows = summaryRowsOfValues(correctionsToCountry)
      )
    ))
  }

  def groupByCountry(correctionPayload: CorrectionPayload, vatReturn: VatReturn): List[CorrectionToCountry] = {
    val returnAmountsToAllCountriesFromNi = (for {
      salesFromNi <- vatReturn.salesFromNi
    } yield {
      Map(salesFromNi.countryOfConsumption -> salesFromNi.amounts.map(_.vatOnSales.amount).sum)
    }).flatten.toMap

    val returnAmountsToAllCountries = vatReturn.salesFromEu.flatMap(_.sales).groupBy(_.countryOfConsumption).flatMap {
      case (country, salesToCountry) => {
        val totalAmount = salesToCountry.flatMap(_.amounts.map(_.vatOnSales.amount)).sum
        val totalAmountFromNi = returnAmountsToAllCountriesFromNi.getOrElse(country, BigDecimal(0))
        val totalOfBoth = totalAmount + totalAmountFromNi

        Map(country -> totalOfBoth)
      }
    }

    val correctionsToAllCountries = for {
      correctionPeriods <- correctionPayload.corrections
      correctionToCountry <- correctionPeriods.correctionsToCountry
    } yield correctionToCountry

    correctionsToAllCountries.groupBy(_.correctionCountry).map {
      case (country, corrections) =>
        val total = corrections.map(_.countryVatCorrection).sum
        val totalOfReturn = total + returnAmountsToAllCountries.getOrElse(country, BigDecimal(0))

        CorrectionToCountry(country, totalOfReturn)
    }.toList
  }

  private[this] def summaryRowsOfCountryVatCorrection(correctionAmount: BigDecimal)(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      SummaryListRowViewModel(
        key = Key("previousReturn.correction.correctionToCountry.previousDeclared.label")
          .withCssClass("govuk-!-font-weight-regular"),
        value = ValueViewModel(HtmlContent("TODO")),
      ),
      SummaryListRowViewModel(
        key = Key("previousReturn.correction.correctionToCountry.correctionAmount.label")
          .withCssClass("govuk-!-font-weight-regular"),
        value = ValueViewModel(HtmlContent(currencyFormat(correctionAmount))),
      )
    )

}
