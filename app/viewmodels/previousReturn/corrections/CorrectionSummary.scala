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
import models.Country
import play.api.i18n.Messages
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
import viewmodels.TitledSummaryList
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import utils.CorrectionUtils
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
        val correctionAmountsToCountries = CorrectionUtils.groupByCountryAndSum(correctionPayload, vatReturn)
        val negativeAndZeroValues = correctionAmountsToCountries.filter {
          case (_, amount) => amount <= 0
        }
        val positiveValues = correctionAmountsToCountries.filter {
          case (_, amount) => amount > 0
        }

        summaryRowsOfNegativeAndZeroValues(negativeAndZeroValues) ++
          summaryRowsOfPositiveValues(positiveValues)
      case _ => Seq.empty
    }
  }

  private[this] def summaryRowsOfValues(correctionsToCountry: Map[Country, BigDecimal])(implicit messages: Messages): List[SummaryListRow] = {

    if (correctionsToCountry.nonEmpty) {
      val columnHeading =
        SummaryListRowViewModel(
          key = Key("previousReturn.correction.vatDeclared.countryLabel")
            .withCssClass("govuk-!-font-weight-bold")
            .withCssClass("govuk-!-width-two-thirds"),
          value = ValueViewModel(messages("previousReturn.correction.vatDeclared.countryAmountLabel"))
            .withCssClass("govuk-!-font-weight-bold")
            .withCssClass("govuk-!-width-one-third"),
        )

      val items = for {
        (country, amount) <- correctionsToCountry
      } yield {
        SummaryListRowViewModel(
          key = Key(HtmlContent(country.name))
            .withCssClass("govuk-!-font-weight-regular"),
          value = ValueViewModel(HtmlContent(currencyFormat(amount))),
        )
      }

      List(columnHeading) ++ items
    } else {
      List.empty
    }
  }

  private[this] def summaryRowsOfNegativeAndZeroValues(correctionsToCountry: Map[Country, BigDecimal])(implicit messages: Messages): Seq[TitledSummaryList] = {
    if(correctionsToCountry.nonEmpty) {
      Seq(
        TitledSummaryList(
          title = messages("previousReturn.correction.vatDeclared.noPaymentDue.title"),
          list = SummaryListViewModel(
            rows = summaryRowsOfValues(correctionsToCountry)
          ),
          hint = Some(messages("previousReturn.correction.vatDeclared.noPaymentDue.hint"))
        )
      )
    } else {
      Seq.empty
    }
  }

  private[this] def summaryRowsOfPositiveValues(correctionsToCountry: Map[Country, BigDecimal])(implicit messages: Messages): Seq[TitledSummaryList] = {
    if(correctionsToCountry.nonEmpty) {
      Seq(TitledSummaryList(
        title = messages("previousReturn.correction.vatDeclared.paymentDue.title"),
        list = SummaryListViewModel(
          rows = summaryRowsOfValues(correctionsToCountry)
        )
      ))
    } else {
      Seq.empty
    }
  }

  private[this] def summaryRowsOfCountryVatCorrection(correctionAmount: BigDecimal)(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      SummaryListRowViewModel(
        key = Key("previousReturn.correction.correctionToCountry.correctionAmount.label")
          .withCssClass("govuk-!-font-weight-regular")
          .withCssClass("govuk-!-width-two-thirds"),
        value = ValueViewModel(HtmlContent(currencyFormat(correctionAmount)))
          .withCssClass("govuk-!-width-one-third"),
      )
    )

}
