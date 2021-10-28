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

import models.{CheckMode, Period, UserAnswers}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object VatPeriodCorrectionsListSummary {

  def row(answers: UserAnswers, correctionPeriod: Period)(implicit messages: Messages): SummaryListRow =
      SummaryListRowViewModel(
        key = "vatPeriodCorrectionsList.checkYourAnswersLabel",
        value = ValueViewModel(correctionPeriod.displayText),
        actions = Seq(
          ActionItemViewModel("site.change", controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(CheckMode, answers.period).url)
            .withVisuallyHiddenText(messages("vatPeriodCorrectionsList.change.hidden")),
          ActionItemViewModel("site.remove", controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(CheckMode, answers.period).url)
            .withVisuallyHiddenText(messages("vatPeriodCorrectionsList.remove.hidden"))
        )
      )
}
