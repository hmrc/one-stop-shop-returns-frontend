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

package pages

import controllers.routes
import models.{CheckLoopMode, CheckMode, CheckSecondLoopMode, CheckThirdLoopMode, Index, Mode, NormalMode, UserAnswers, VatOnSales}
import pages.PageConstants._
import play.api.libs.json.JsPath
import play.api.mvc.Call

case class VatOnSalesFromEuPage(countryFromIndex: Index, countryToIndex: Index, vatRateIndex: Index) extends QuestionPage[VatOnSales] {

  override def path: JsPath =
    JsPath \ salesFromEu \ countryFromIndex.position \ salesFromCountry \ countryToIndex.position \ vatRates \ vatRateIndex.position \ salesAtVatRate \ toString

  override def toString: String = vatOnSales

  private def navigateWithChecks(currentMode: Mode, nextMode:Mode, answers: UserAnswers): Call =
    answers.get(VatRatesFromEuPage(countryFromIndex, countryToIndex)).map {
      rates =>
        if (rates.size > vatRateIndex.position + 1) {
          routes.NetValueOfSalesFromEuController.onPageLoad(currentMode, answers.period, countryFromIndex, countryToIndex, vatRateIndex + 1)
        } else {
          routes.CheckSalesToEuController.onPageLoad(nextMode, answers.period, countryFromIndex, countryToIndex)
        }
    }.getOrElse(routes.JourneyRecoveryController.onPageLoad())

  override def navigateInNormalMode(answers: UserAnswers): Call = navigateWithChecks(NormalMode, NormalMode, answers)

  override def navigateInCheckMode(answers: UserAnswers): Call = navigateWithChecks(CheckMode, CheckMode, answers)

  override def navigateInCheckLoopMode(answers: UserAnswers): Call = navigateWithChecks(CheckLoopMode, NormalMode, answers)

  override def navigateInCheckSecondLoopMode(answers: UserAnswers): Call = navigateWithChecks(CheckSecondLoopMode, CheckSecondLoopMode, answers)

  override def navigateInCheckThirdLoopMode(answers: UserAnswers): Call = navigateWithChecks(CheckThirdLoopMode, CheckThirdLoopMode, answers)

  override def navigateInCheckInnerLoopMode(answers: UserAnswers): Call =
    routes.CheckSalesToEuController.onPageLoad(NormalMode, answers.period, countryFromIndex, countryToIndex)

  override def navigateInCheckSecondInnerLoopMode(answers: UserAnswers): Call =
    routes.CheckSalesToEuController.onPageLoad(CheckSecondLoopMode, answers.period, countryFromIndex, countryToIndex)

  override def navigateInCheckThirdInnerLoopMode(answers: UserAnswers): Call =
    routes.CheckSalesToEuController.onPageLoad(CheckThirdLoopMode, answers.period, countryFromIndex, countryToIndex)

  override def navigateInCheckFinalInnerLoopMode(answers: UserAnswers): Call =
    routes.CheckSalesToEuController.onPageLoad(CheckMode, answers.period, countryFromIndex, countryToIndex)
}
