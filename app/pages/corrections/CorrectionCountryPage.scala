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
import models.{Country, Index, Mode, UserAnswers}
import pages.PageConstants.{correctionToCountry, corrections}
import pages.QuestionPage
import play.api.libs.json.JsPath
import play.api.mvc.Call

case class CorrectionCountryPage(periodIndex: Index, countryIndex: Index) extends QuestionPage[Country] {

  override def path: JsPath = JsPath \ corrections \ periodIndex.position \ correctionToCountry \ countryIndex.position \ toString

  override def toString: String = "correctionCountry"

   def navigate(mode: Mode, answers: UserAnswers, countriesFromNi: Seq[Country], countriesFromEU: Seq[Country]): Call = {
    answers.get(CorrectionCountryPage(periodIndex, countryIndex)) match {
      case Some(country) =>
        if(countriesFromNi.contains(country) || countriesFromEU.contains(country)){
          controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(mode, answers.period, periodIndex, countryIndex)
        }else{
          controllers.corrections.routes.UndeclaredCountryCorrectionController.onPageLoad(mode, answers.period, periodIndex, countryIndex)}
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
  }
}
