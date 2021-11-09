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
import models.{CheckMode, Index, NormalMode, UserAnswers}
import pages.QuestionPage
import play.api.libs.json.JsPath
import play.api.mvc.Call

case class CorrectionReturnSinglePeriodPage(index: Index) extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "correctionReturnSinglePeriod"

  override def navigateInNormalMode(answers: UserAnswers): Call =
    answers.get(this) match {
      case Some(true) => controllers.corrections.routes.CorrectionCountryController.onPageLoad(NormalMode, answers.period, index, Index(0))
      case Some(false) => controllers.corrections.routes.NoOtherCorrectionPeriodsAvailableController.onPageLoad(answers.period)
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }

  override def navigateInCheckMode(answers: UserAnswers): Call =
    answers.get(CorrectionReturnSinglePeriodPage) match {
      case Some(true) => controllers.corrections.routes.CorrectionCountryController.onPageLoad(CheckMode, answers.period, Index(0), Index(0))
      case Some(false) => controllers.corrections.routes.NoOtherCorrectionPeriodsAvailableController.onPageLoad(answers.period)
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
}
