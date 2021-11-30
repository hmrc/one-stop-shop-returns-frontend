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
import models.{CheckMode, CheckThirdLoopMode, Index, Mode, NormalMode, UserAnswers}
import play.twirl.api.HtmlFormat
import queries.AllSalesFromEuQuery
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem

object SalesFromEuSummary {

  def addToListRows(answers: UserAnswers, currentMode: Mode): Seq[ListItem] =
    answers.get(AllSalesFromEuQuery).getOrElse(List.empty).zipWithIndex.map {
      case (details, index) =>
        val newMode = if(currentMode == NormalMode) CheckThirdLoopMode else currentMode
        ListItem(
          name = HtmlFormat.escape(details.countryOfSale.name).toString,
          changeUrl = routes.SalesToEuListController.onPageLoad(newMode, answers.period, Index(index)).url,
          removeUrl = routes.DeleteSalesFromEuController.onPageLoad(currentMode, answers.period, Index(index)).url
        )
    }

}
