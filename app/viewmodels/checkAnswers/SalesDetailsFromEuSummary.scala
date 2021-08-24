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

package viewmodels.checkAnswers

import controllers.routes
import models.{CheckMode, Index, UserAnswers}
import pages.SalesDetailsFromEuPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CurrencyFormatter._
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object SalesDetailsFromEuSummary  {

  def row(answers: UserAnswers, countryFromIndex: Index, countryToIndex: Index, vatRateIndex: Index)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(SalesDetailsFromEuPage(countryFromIndex, countryToIndex, vatRateIndex)).map {
      answer =>

      val value = HtmlFormat.escape(
        currencyFormat(answer.netValueOfSales)) +
        "<br/>" +
        HtmlFormat.escape(currencyFormat(answer.vatOnSales))

        SummaryListRowViewModel(
          key     = "salesDetailsFromEu.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlContent(value)),
          actions = Seq(
            ActionItemViewModel(
              "site.change",
              routes.SalesDetailsFromEuController.onPageLoad(CheckMode, answers.period, countryFromIndex, countryToIndex, vatRateIndex).url
            )
            .withVisuallyHiddenText(messages("salesDetailsFromEu.change.hidden"))
          )
        )
    }
}
