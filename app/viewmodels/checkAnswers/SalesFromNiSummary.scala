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

package viewmodels.checkAnswers

import controllers.routes
import models.{CheckSecondLoopMode, Index, Mode, NormalMode, UserAnswers}
import play.twirl.api.HtmlFormat
import queries.AllSalesFromNiWithOptionalVatQuery
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem

object SalesFromNiSummary {

  def addToListRows(answers: UserAnswers, currentMode: Mode): Seq[ListItem] =
    answers.get(AllSalesFromNiWithOptionalVatQuery).getOrElse(List.empty).zipWithIndex.map {
      case (details, index) =>
        val newMode = if(currentMode == NormalMode) CheckSecondLoopMode else currentMode
        ListItem(
          name      = HtmlFormat.escape(details.countryOfConsumption.name).toString,
          changeUrl = routes.CheckSalesFromNiController.onPageLoad(newMode, answers.period, Index(index)).url,
          removeUrl = routes.DeleteSalesFromNiController.onPageLoad(currentMode, answers.period, Index(index)).url
        )
    }

}
