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

import models.{CheckLoopMode, CheckMode, Index, NormalMode, UserAnswers}
import pages.QuestionPage
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.corrections.{DeriveNumberOfCorrectionPeriods, DeriveNumberOfCorrections}

case class RemoveCountryCorrectionPage(periodIndex: Index) extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "removeCountryCorrection"

  override def navigateInNormalMode(answers: UserAnswers): Call =
    answers.get(DeriveNumberOfCorrections(periodIndex)) match {
      case Some(n) if n > 0 => controllers.corrections.routes.VatCorrectionsListController.onPageLoad(NormalMode, answers.period, periodIndex)
      case _ => answers.get(DeriveNumberOfCorrectionPeriods) match {
        case Some(x) if x > 0 => controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(NormalMode, answers.period)
        case _ => controllers.corrections.routes.CorrectPreviousReturnController.onPageLoad(NormalMode, answers.period)
      }
    }

  override def navigateInCheckMode(answers: UserAnswers): Call =
    answers.get(DeriveNumberOfCorrections(periodIndex)) match {
      case Some(n) if n > 0 => controllers.corrections.routes.VatCorrectionsListController.onPageLoad(CheckMode, answers.period, periodIndex)
      case _ => answers.get(DeriveNumberOfCorrectionPeriods) match {
        case Some(x) if x > 0 => controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(CheckMode, answers.period)
        case _ => controllers.corrections.routes.CorrectPreviousReturnController.onPageLoad(CheckMode, answers.period)
      }
    }
}
