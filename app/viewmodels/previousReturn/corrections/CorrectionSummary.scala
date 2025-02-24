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

package viewmodels.previousReturn.corrections

import models.Country
import models.corrections.{CorrectionPayload, PeriodWithCorrections}
import models.domain.VatReturn
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow
import utils.CorrectionUtils
import utils.CurrencyFormatter._
import viewmodels.govuk.summarylist._
import viewmodels.govuk.table._
import viewmodels.implicits._
import viewmodels.{TitledSummaryList, TitledTable}

object CorrectionSummary {

  def getCorrectionPeriods(maybeCorrectionPayload: Option[CorrectionPayload])(implicit messages: Messages): Option[TitledTable] = {
    maybeCorrectionPayload.map { correctionPayload =>
      TitledTable(
        title = messages("previousReturn.correction.correctionPeriod.title"),
        list = TableViewModel(
          rows = Seq(correctionHeaders()) ++ summaryRowsOfCorrectionToCountry(correctionPayload.corrections)
        )
      )
    }
  }

  private[this] def correctionHeaders()(implicit messages: Messages): Seq[TableRow] = {
    Seq(
      TableRowViewModel(
        content = Text(messages("previousReturn.correction.vatDeclared.periodLabel"))
      ).withCssClass("govuk-!-font-weight-bold"),
      TableRowViewModel(
        content = Text(messages("previousReturn.correction.vatDeclared.countryLabel"))
      ).withCssClass("govuk-!-font-weight-bold"),
      TableRowViewModel(
        content = Text(messages("previousReturn.correction.vatDeclared.countryAmountLabel"))
      ).withCssClass("govuk-!-font-weight-bold")
        .withCssClass("govuk-table__cell--numeric")
    )
  }

  private[this] def summaryRowsOfCorrectionToCountry(periodsWithCorrections: List[PeriodWithCorrections])(implicit messages: Messages): Seq[Seq[TableRow]] = {
    periodsWithCorrections.flatMap{
      periodWithCorrections =>
          periodWithCorrections.correctionsToCountry.getOrElse(List.empty).groupBy(_.correctionCountry).map{
            case (country, corrections) =>
              Seq(
                TableRowViewModel(
                  content = Text(periodWithCorrections.correctionReturnPeriod.displayText)
                ),
                TableRowViewModel(
                  content = Text(country.name)
                ),
                TableRowViewModel(
                  content = HtmlContent(currencyFormat(corrections.map(_.countryVatCorrection.getOrElse(BigDecimal(0))).sum))
                ).withCssClass("govuk-table__cell--numeric")
              )
          }
    }
  }

  def getDeclaredVat(maybeCorrectionPayload: Option[CorrectionPayload], vatReturn: VatReturn)
                    (implicit messages: Messages): Seq[TitledSummaryList] = {
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
      case _ =>
        val countryValues = CorrectionUtils.groupByCountryAndSum(vatReturn)

        summaryRowsOfPositiveValues(countryValues)
    }
  }

  private[this] def summaryRowsOfNegativeAndZeroValues(correctionsToCountry: Map[Country, BigDecimal])(implicit messages: Messages): Seq[TitledSummaryList] = {
    if (correctionsToCountry.nonEmpty) {
      Seq(
        TitledSummaryList(
          title = Some(messages("previousReturn.correction.vatDeclared.noPaymentDue.title")),
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

  private[this] def summaryRowsOfPositiveValues(correctionsToCountry: Map[Country, BigDecimal])(implicit messages: Messages): Seq[TitledSummaryList] = {
    if (correctionsToCountry.nonEmpty) {
      Seq(TitledSummaryList(
        title = Some(messages("previousReturn.correction.vatDeclared.paymentDue.title")),
        list = SummaryListViewModel(
          rows = summaryRowsOfValues(correctionsToCountry)
        )
      ))
    } else {
      Seq.empty
    }
  }

}
