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

package viewmodels.checkAnswers

import controllers.routes
import models.{CheckLoopMode, Index, Mode, NormalMode, UserAnswers}
import pages.VatRatesFromEuPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object VatRatesFromEuSummary  {

  def row(answers: UserAnswers, countryFromIndex: Index, countryToIndex: Index, currentMode: Mode)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(VatRatesFromEuPage(countryFromIndex, countryToIndex)).map {
      vatRatesFromEu =>
        val newMode = if(currentMode == NormalMode) CheckLoopMode else currentMode

        val value = ValueViewModel(
          HtmlContent(
            vatRatesFromEu.map {
              answer => HtmlFormat.escape(answer.rateForDisplay).toString
            }
            .mkString(",<br>")
          )
        )

        SummaryListRowViewModel(
          key     = "vatRatesFromEu.checkYourAnswersLabel",
          value   = value,
          actions = Seq(
            ActionItemViewModel("site.change", routes.VatRatesFromEuController.onPageLoad(newMode, answers.period, countryFromIndex, countryToIndex).url)
              .withVisuallyHiddenText(messages("vatRatesFromEu.change.hidden"))
              .withAttribute(("id", "change-vat-rates"))
          )
        )
    }
}
