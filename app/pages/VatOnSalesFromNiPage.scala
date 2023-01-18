/*
 * Copyright 2023 HM Revenue & Customs
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
import models.{CheckLoopMode, CheckMode, CheckSecondLoopMode, Index, Mode, NormalMode, UserAnswers, VatOnSales}
import pages.PageConstants.{salesAtVatRate, salesFromNi, vatOnSales, vatRates}
import play.api.libs.json.JsPath
import play.api.mvc.Call

case class VatOnSalesFromNiPage(countryIndex: Index, vatRateIndex: Index) extends QuestionPage[VatOnSales] {

  override def path: JsPath =
    JsPath \ salesFromNi \ countryIndex.position \ vatRates \ vatRateIndex.position \ salesAtVatRate \ toString

  override def toString: String = vatOnSales

  override def navigateInNormalMode(answers: UserAnswers): Call =
    commonNavigate(NormalMode, answers)

  override def navigateInCheckMode(answers: UserAnswers): Call =
    commonNavigate(CheckMode, answers)

  override def navigateInCheckLoopMode(answers: UserAnswers): Call =
    answers.get(VatRatesFromNiPage(countryIndex)).map {
      rates =>
        if (rates.size > vatRateIndex.position + 1) {
          routes.NetValueOfSalesFromNiController.onPageLoad(CheckLoopMode, answers.period, countryIndex, vatRateIndex + 1)
        } else {
          routes.CheckSalesFromNiController.onPageLoad(NormalMode, answers.period, countryIndex)
        }
    }.getOrElse(routes.JourneyRecoveryController.onPageLoad())

  override def navigateInCheckSecondLoopMode(answers: UserAnswers): Call =
    commonNavigate(CheckSecondLoopMode, answers)

  override def navigateInCheckInnerLoopMode(answers: UserAnswers): Call =
    routes.CheckSalesFromNiController.onPageLoad(NormalMode, answers.period, countryIndex)

  override def navigateInCheckSecondInnerLoopMode(answers: UserAnswers): Call =
    routes.CheckSalesFromNiController.onPageLoad(CheckSecondLoopMode, answers.period, countryIndex)

  override def navigateInCheckFinalInnerLoopMode(answers: UserAnswers): Call =
    routes.CheckSalesFromNiController.onPageLoad(CheckMode, answers.period, countryIndex)


  private def commonNavigate(mode: Mode, answers: UserAnswers): Call = {
    answers.get(VatRatesFromNiPage(countryIndex)).map {
      rates =>
        if (rates.size > vatRateIndex.position + 1) {
          routes.NetValueOfSalesFromNiController.onPageLoad(mode, answers.period, countryIndex, vatRateIndex + 1)
        } else {
          routes.CheckSalesFromNiController.onPageLoad(mode, answers.period, countryIndex)
        }
    }.getOrElse(routes.JourneyRecoveryController.onPageLoad())
  }
}
