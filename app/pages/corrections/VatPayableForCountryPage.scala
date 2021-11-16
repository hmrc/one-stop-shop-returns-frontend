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
import models.{CheckMode, Index, Mode, NormalMode, UserAnswers}
import pages.QuestionPage
import play.api.libs.json.JsPath
import play.api.mvc.Call

case class VatPayableForCountryPage(periodIndex: Index, countryIndex: Index) extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "vatPayableForCountry"

  def navigate(mode: Mode, answers: UserAnswers, completeJourney: Boolean): Call =
    (answers.get(VatPayableForCountryPage(periodIndex, countryIndex)), mode) match {
      case (Some(true), NormalMode) => controllers.corrections.routes.CheckVatPayableAmountController.onPageLoad(NormalMode, answers.period, periodIndex, countryIndex, completeJourney)
      case (Some(true), _) => controllers.corrections.routes.CheckVatPayableAmountController.onPageLoad(CheckMode, answers.period, periodIndex, countryIndex, completeJourney)
      case (Some(false), NormalMode) => controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(NormalMode, answers.period, periodIndex, countryIndex, completeJourney)
      case (Some(false), _) => controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(mode, answers.period, periodIndex, countryIndex, completeJourney)
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
}
