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
import models.{CheckMode, Index, Mode, NormalMode, UserAnswers}
import play.api.mvc.Call
import queries.DeriveNumberOfSalesFromNi

case object SalesFromNiListPage extends Page {

  def navigate(answers: UserAnswers, mode: Mode, addAnother: Boolean): Call =
    if (addAnother) {
      answers.get(DeriveNumberOfSalesFromNi) match {
        case Some(size) => routes.CountryOfConsumptionFromNiController.onPageLoad(mode, answers.period, Index(size))
        case None => routes.JourneyRecoveryController.onPageLoad()
      }
    } else {
      mode match {
        case NormalMode =>
          routes.SoldGoodsFromEuController.onPageLoad(mode, answers.period)
        case CheckMode =>
          routes.CheckYourAnswersController.onPageLoad(answers.period)
        case _ =>
          routes.JourneyRecoveryController.onPageLoad()
      }
    }
}
