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
import pages.SalesAtVatRateFromEuPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CurrencyFormatter._
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object SalesAtVatRateFromEuSummary  {

  def row(answers: UserAnswers, countryFromIndex: Index, countryToIndex: Index, vatRateIndex: Index)(implicit messages: Messages): Seq[SummaryListRow] =
    answers.get(SalesAtVatRateFromEuPage(countryFromIndex, countryToIndex, vatRateIndex)).toSeq.flatMap {
      answer =>
        Seq(SummaryListRowViewModel(
          key     = "salesAtVatRateFromEu.netValueOfSales.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlContent(currencyFormat(answer.netValueOfSales))),
          actions = Seq(
            ActionItemViewModel("site.change",
              routes.SalesAtVatRateFromEuController.onPageLoad(CheckMode, answers.period, countryFromIndex, countryToIndex, vatRateIndex).url)
              .withVisuallyHiddenText(messages("salesAtVatRateFromEu.netValueOfSales.change.hidden"))
          )
        ),
          SummaryListRowViewModel(
            key     = "salesAtVatRateFromEu.vatOnSales.checkYourAnswersLabel",
            value   = ValueViewModel(HtmlContent(currencyFormat(answer.vatOnSales))),
            actions = Seq(
              ActionItemViewModel("site.change",
                routes.SalesAtVatRateFromEuController.onPageLoad(CheckMode, answers.period, countryFromIndex, countryToIndex, vatRateIndex).url)
                .withVisuallyHiddenText(messages("salesAtVatRateFromEu.vatOnSales.change.hidden"))
            )
          ))

    }
}
