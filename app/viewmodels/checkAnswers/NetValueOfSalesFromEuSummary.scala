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
import models.{CheckFinalInnerLoopMode, CheckInnerLoopMode, CheckMode, CheckSecondInnerLoopMode, CheckSecondLoopMode, CheckThirdInnerLoopMode, CheckThirdLoopMode, Index, Mode, NormalMode, UserAnswers, VatRate}
import pages.NetValueOfSalesFromEuPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CurrencyFormatter.currencyFormat
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object NetValueOfSalesFromEuSummary  {

  def row(answers: UserAnswers, countryFromIndex: Index, countryToIndex: Index, vatRateIndex: Index, vatRate: VatRate, currentMode: Mode)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(NetValueOfSalesFromEuPage(countryFromIndex, countryToIndex, vatRateIndex)).map {
      answer =>
        val newMode = currentMode match {
          case NormalMode => CheckInnerLoopMode
          case CheckSecondLoopMode => CheckSecondInnerLoopMode
          case CheckThirdLoopMode => CheckThirdInnerLoopMode
          case CheckMode => CheckFinalInnerLoopMode
        }
        SummaryListRowViewModel(
          key     = "netValueOfSalesFromEu.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlContent(currencyFormat(answer))),
          actions = Seq(
            ActionItemViewModel(
              "site.change",
              routes.NetValueOfSalesFromEuController.onPageLoad(newMode, answers.period, countryFromIndex, countryToIndex, vatRateIndex).url
            )
            .withVisuallyHiddenText(messages("netValueOfSalesFromEu.change.hidden", vatRate.rateForDisplay))
              .withAttribute(("id", s"change-net-value-sales-${vatRate.rate}-percent"))
          )
        )
    }
}
