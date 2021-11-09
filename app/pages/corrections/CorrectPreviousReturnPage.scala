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

package pages.corrections

import controllers.routes
import models.{Index, Mode, UserAnswers}
import pages.QuestionPage
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.corrections.AllCorrectionPeriodsQuery

import scala.util.Try

case object CorrectPreviousReturnPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "correctPreviousReturn"

  def navigate(mode: Mode, answers: UserAnswers, period: Int): Call =
    answers.get(CorrectPreviousReturnPage) match {
      case Some(true) => if(period > 1) {
        controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(mode, answers.period, Index(0))
      } else {
        controllers.corrections.routes.CorrectionReturnSinglePeriodController.onPageLoad(mode, answers.period, Index(0))
      }
      case Some(false) =>  routes.CheckYourAnswersController.onPageLoad(answers.period)
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] =
    userAnswers.remove(AllCorrectionPeriodsQuery)
}
