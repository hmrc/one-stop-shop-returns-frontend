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

import controllers.routes
import models.{Country, Index, Mode, UserAnswers}
import pages.PageConstants.*
import pages.QuestionPage
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.corrections.PreviouslyDeclaredCorrectionAmountQuery

case class CorrectionCountryPage(periodIndex: Index, countryIndex: Index) extends QuestionPage[Country] {

  override def path: JsPath = JsPath \ corrections \ periodIndex.position \ correctionsToCountry \ countryIndex.position \ toString

  override def toString: String = "correctionCountry"

   def navigate(mode: Mode, answers: UserAnswers, countries: Seq[Country], strategicReturnApiEnabled: Boolean): Call = {
    answers.get(CorrectionCountryPage(periodIndex, countryIndex)) match {
      case Some(country) =>
        if (strategicReturnApiEnabled) {
          answers.get(PreviouslyDeclaredCorrectionAmountQuery(periodIndex, countryIndex)) match {
            case Some(n) if n.amount > 0 =>
              controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(
                mode, answers.period, periodIndex, countryIndex, undeclaredCountry = false
              )
            case _ =>
              controllers.corrections.routes.UndeclaredCountryCorrectionController.onPageLoad(
                mode, answers.period, periodIndex, countryIndex
              )
          }
        } else {
          if (countries.contains(country)) {
            controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(
              mode, answers.period, periodIndex, countryIndex, undeclaredCountry = false
            )
          } else {
            controllers.corrections.routes.UndeclaredCountryCorrectionController.onPageLoad(
              mode, answers.period, periodIndex, countryIndex
            )
          }
        }
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
  }
}
