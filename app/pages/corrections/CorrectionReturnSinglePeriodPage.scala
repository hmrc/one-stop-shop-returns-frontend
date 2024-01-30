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

package pages.corrections

import models.{Index, Mode, UserAnswers}
import pages.Page
import play.api.mvc.Call

case class CorrectionReturnSinglePeriodPage(index: Index) extends Page {

  override def toString: String = "correctionReturnSinglePeriod"

  def navigate(mode: Mode, answers: UserAnswers, addOnlyPeriod: Boolean): Call = {
      if (addOnlyPeriod) {
        controllers.corrections.routes.CorrectionCountryController.onPageLoad(mode, answers.period, index, Index(0))
      } else {
        controllers.corrections.routes.NoOtherCorrectionPeriodsAvailableController.onPageLoad(answers.period)
      }

  }
}
