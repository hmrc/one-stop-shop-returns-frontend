/*
 * Copyright 2023 HM Revenue & Customs
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

package pages

import controllers.routes
import models.{CheckThirdLoopMode, Index, Mode, NormalMode, UserAnswers}
import play.api.mvc.Call
import queries.DeriveNumberOfSalesToEu

case class SalesToEuListPage(index: Index) extends Page {

  def navigate(answers: UserAnswers, mode: Mode, addAnother: Boolean): Call = {
    val newMode = if(mode == CheckThirdLoopMode) NormalMode else mode
    if (addAnother) {
      answers.get(DeriveNumberOfSalesToEu(index)) match {
        case Some(size) => routes.CountryOfConsumptionFromEuController.onPageLoad(mode, answers.period, index, Index(size))
        case None       => routes.JourneyRecoveryController.onPageLoad()
      }
    } else {
      routes.SalesFromEuListController.onPageLoad(newMode, answers.period)
    }
  }
}
