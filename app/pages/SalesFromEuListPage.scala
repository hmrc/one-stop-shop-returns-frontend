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

package pages

import controllers.routes
import models.{CheckMode, Index, Mode, UserAnswers}
import play.api.mvc.Call
import queries.DeriveNumberOfSalesFromEu

case object SalesFromEuListPage extends Page {

  def navigate(answers: UserAnswers, mode: Mode, addAnother: Boolean): Call =
    if (addAnother) {
      answers.get(DeriveNumberOfSalesFromEu) match {
        case Some(size) => routes.CountryOfSaleFromEuController.onPageLoad(mode, answers.period, Index(size))
        case None       => routes.JourneyRecoveryController.onPageLoad()
      }
    } else {
        if(mode == CheckMode){
          routes.CheckYourAnswersController.onPageLoad(answers.period)
        } else {
          controllers.corrections.routes.CorrectPreviousReturnController.onPageLoad(mode, answers.period)
        }
    }
}
