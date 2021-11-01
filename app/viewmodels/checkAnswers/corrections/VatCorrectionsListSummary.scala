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
import play.twirl.api.HtmlFormat
import queries.corrections.AllCorrectionCountriesQuery
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem

object VatCorrectionsListSummary {

  def addToListRows(answers: UserAnswers, currentMode: Mode, periodIndex: Index)(implicit messages: Messages): List[ListItem] =
    answers.get(AllCorrectionCountriesQuery(periodIndex)).getOrElse(List.empty).zipWithIndex.map {
      case (details, index) =>

        ListItem(
          name = HtmlFormat.escape(details.name).toString,
          changeUrl = routes.CountryVatCorrectionController.onPageLoad(CheckMode, answers.period).url,
          removeUrl = routes.RemoveCountryCorrectionController.onPageLoad(currentMode, answers.period).url
        )
    }
}
