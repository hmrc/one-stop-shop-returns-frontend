/*
 * Copyright 2022 HM Revenue & Customs
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

import models.{CheckThirdLoopMode, Index, Mode, NormalMode, UserAnswers}
import play.api.i18n.Messages
import queries.corrections.DeriveCompletedCorrectionPeriods
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem

object VatPeriodCorrectionsListSummary {

  def getCompletedRows(answers: UserAnswers, currentMode: Mode)(implicit messages: Messages): Seq[ListItem] = {
    val mode = if(currentMode == NormalMode) CheckThirdLoopMode else currentMode
    answers
      .get(DeriveCompletedCorrectionPeriods).getOrElse(List.empty).zipWithIndex.map{
      case (correctionPeriod, index) =>
        ListItem(
          name = correctionPeriod.displayText,
          changeUrl = controllers.corrections.routes.VatCorrectionsListController.onPageLoad(mode, answers.period, Index(index)).url,
          removeUrl = controllers.corrections.routes.RemovePeriodCorrectionController.onPageLoad(currentMode, answers.period, Index(index)).url
        )
    }
  }

}
