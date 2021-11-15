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

import models.{CheckMode, Index, UserAnswers}
import pages.corrections.CorrectionCountryPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object CorrectionCountrySummary {

  def row(answers: UserAnswers, periodIndex: Index, countryIndex: Index)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(CorrectionCountryPage(periodIndex, countryIndex)).map {
      answer =>

        SummaryListRowViewModel(
          key = "correctionCountry.checkYourAnswersLabel",
          value = ValueViewModel(answer.toString),
          actions = Seq(
            ActionItemViewModel("site.change", controllers.corrections.routes.CorrectionCountryController.onPageLoad(CheckMode, answers.period, periodIndex, countryIndex).url)
              .withVisuallyHiddenText(messages("correctionCountry.change.hidden"))
          )
        )
    }
}
