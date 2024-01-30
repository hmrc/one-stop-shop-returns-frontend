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
import models.{Index, Mode, UserAnswers}
import pages.PageConstants.{corrections, correctionsToCountry}
import pages.QuestionPage
import play.api.libs.json.JsPath
import play.api.mvc.Call

case class CountryVatCorrectionPage(periodIndex: Index, countryIndex: Index) extends QuestionPage[BigDecimal] {

  override def path: JsPath = JsPath \ corrections \ periodIndex.position \ correctionsToCountry \ countryIndex.position \ toString

  override def toString: String = "countryVatCorrection"

  override def navigate(mode: Mode, answers: UserAnswers): Call =
    answers.get(CountryVatCorrectionPage(periodIndex, countryIndex)) match {
      case Some(vatAmount) =>
        controllers.corrections.routes.VatPayableForCountryController.onPageLoad(mode, answers.period, periodIndex, countryIndex)
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
}
