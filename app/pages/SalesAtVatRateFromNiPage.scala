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

package pages

import controllers.routes
import models.{CheckLoopMode, CheckMode, Index, Mode, NormalMode, SalesAtVatRate, UserAnswers}
import pages.PageConstants._
import play.api.libs.json.JsPath
import play.api.mvc.Call

case class SalesAtVatRateFromNiPage(countryIndex: Index, vatRateIndex: Index) extends QuestionPage[SalesAtVatRate] {

  override def path: JsPath = JsPath \ salesFromNi \ countryIndex.position \ toString \ vatRateIndex.position

  override def toString: String = salesAtVatRate

  override def navigateInNormalMode(answers: UserAnswers): Call =
    commonNavigate(NormalMode, answers)

  override def navigateInCheckMode(answers: UserAnswers): Call =
    commonNavigate(CheckMode, answers)

  override def navigateInCheckLoopMode(answers: UserAnswers): Call =
    answers.get(VatRatesFromNiPage(countryIndex)).map {
      _ =>
        routes.CheckSalesFromNiController.onPageLoad(NormalMode, answers.period, countryIndex)
    }.getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def commonNavigate(mode: Mode, answers: UserAnswers) = {
    answers.get(VatRatesFromNiPage(countryIndex)).map {
      rates =>
        if (rates.size > vatRateIndex.position + 1) {
          routes.SalesAtVatRateFromNiController.onPageLoad(mode, answers.period, countryIndex, vatRateIndex + 1)
        } else {
          routes.CheckSalesFromNiController.onPageLoad(mode, answers.period, countryIndex)
        }
    }.getOrElse(routes.JourneyRecoveryController.onPageLoad())
  }
}
