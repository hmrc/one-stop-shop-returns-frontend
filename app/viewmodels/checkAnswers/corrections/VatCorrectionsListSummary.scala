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

package viewmodels.checkAnswers.corrections

import controllers.corrections.routes
import models.{CheckMode, Index, Mode, UserAnswers}
import play.api.i18n.Messages
import queries.corrections.AllCorrectionCountriesQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
import utils.CurrencyFormatter.currencyFormat
import viewmodels.TitledSummaryList

object VatCorrectionsListSummary {

  def addToListRows(answers: UserAnswers, currentMode: Mode, periodIndex: Index)(implicit messages: Messages): List[TitledSummaryList] =
    answers.get(AllCorrectionCountriesQuery(periodIndex)).getOrElse(List.empty).zipWithIndex.map {
      case (correctionToCountry, countryIndex) =>

        val row = SummaryListRowViewModel(
          key = messages("vatCorrectionsList.correctionAmount"),
          value = ValueViewModel(HtmlContent(currencyFormat(correctionToCountry.countryVatCorrection))),
          actions = Seq(
            ActionItemViewModel(
              content = "site.change",
              href = routes.CountryVatCorrectionController.onPageLoad(
                currentMode,
                answers.period,
                periodIndex,
                Index(countryIndex)).url
            ).withVisuallyHiddenText(messages("vatCorrectionsList.change.hidden"))
          )
        )

        TitledSummaryList(
          title = correctionToCountry.correctionCountry.name,
          list = SummaryListViewModel(
            rows = Seq(row)
          )
        )
    }
}
