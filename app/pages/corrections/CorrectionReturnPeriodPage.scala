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
import models.{Index, NormalMode, Period, UserAnswers}
import pages.PageConstants.period
import pages.QuestionPage
import play.api.libs.json.JsPath
import play.api.mvc.Call

case class CorrectionReturnPeriodPage(index: Index) extends QuestionPage[Period] {

  override def path: JsPath = JsPath \ period \ index.position \ toString

  override def toString: String = "correctionReturnPeriod"

  override def navigateInNormalMode(answers: UserAnswers): Call =
    answers.get(CorrectionReturnPeriodPage(index)) match {
      case Some(period) => controllers.corrections.routes.CorrectionCountryController.onPageLoad(NormalMode, period, index)
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
}
