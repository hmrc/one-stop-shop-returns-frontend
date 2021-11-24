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

import controllers.corrections.{routes => correctionRoutes}
import controllers.routes
import models.{Index, Mode, UserAnswers}
import pages.Page
import play.api.mvc.Call
import queries.corrections.DeriveNumberOfCorrectionPeriods

case object VatPeriodCorrectionsListPage extends Page {

  def navigate(mode: Mode, answers: UserAnswers, addAnother: Boolean): Call = {
    if(addAnother) {
      answers.get(DeriveNumberOfCorrectionPeriods) match {
        case Some(size) =>
          correctionRoutes.CorrectionReturnPeriodController.onPageLoad(mode, answers.period, Index(size))
        case None =>
          correctionRoutes.CorrectionReturnPeriodController.onPageLoad(mode, answers.period, Index(0))
      }
    } else {
      routes.CheckYourAnswersController.onPageLoad(answers.period)
    }
  }
}