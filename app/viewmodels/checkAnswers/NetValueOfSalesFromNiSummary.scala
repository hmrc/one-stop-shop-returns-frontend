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
import models.{CheckFinalInnerLoopMode, CheckInnerLoopMode, CheckMode, CheckSecondInnerLoopMode, CheckSecondLoopMode, Index, Mode, NormalMode, UserAnswers, VatRate}
import pages.NetValueOfSalesFromNiPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CurrencyFormatter.currencyFormat
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object NetValueOfSalesFromNiSummary {

  def row(answers: UserAnswers, countryIndex: Index, vatRateIndex: Index, vatRate: VatRate, currentMode: Mode)
         (implicit messages: Messages): Option[SummaryListRow] =
    for {
      newMode <- currentMode match {
        case NormalMode => Some(CheckInnerLoopMode)
        case CheckSecondLoopMode => Some(CheckSecondInnerLoopMode)
        case CheckMode => Some(CheckFinalInnerLoopMode)
        case _ => None
      }
      answer <- answers.get(NetValueOfSalesFromNiPage(countryIndex, vatRateIndex))
    } yield {
      SummaryListRowViewModel(
        key = "netValueOfSalesFromNi.checkYourAnswersLabel",
        value = ValueViewModel(HtmlContent(currencyFormat(answer))),
        actions = Seq(
          ActionItemViewModel("site.change", routes.NetValueOfSalesFromNiController.onPageLoad(
            newMode,
            answers.period,
            countryIndex,
            vatRateIndex).url)
            .withVisuallyHiddenText(messages("netValueOfSalesFromNi.change.hidden", vatRate.rateForDisplay))
            .withAttribute(("id", s"change-net-value-sales-${vatRate.rate}-percent"))
        )
      )
    }
}
