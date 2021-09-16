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
import models.{CheckMode, Index, NormalMode, UserAnswers, VatOnSales}
import pages.PageConstants.{salesAtVatRate, salesFromCountry, salesFromEu, vatOnSales}
import play.api.libs.json.JsPath
import play.api.mvc.Call

case class VatOnSalesFromEuPage(countryFromIndex: Index, countryToIndex: Index, vatRateIndex: Index) extends QuestionPage[VatOnSales] {

  override def path: JsPath =
    JsPath \ salesFromEu \ countryFromIndex.position \ salesFromCountry \ countryToIndex.position \ salesAtVatRate \ vatRateIndex.position \ toString

  override def toString: String = vatOnSales

  override def navigateInNormalMode(answers: UserAnswers): Call =
    answers.get(VatRatesFromEuPage(countryFromIndex, countryToIndex)).map {
      rates =>
        if (rates.size > vatRateIndex.position + 1) {
          routes.NetValueOfSalesFromEuController.onPageLoad(NormalMode, answers.period, countryFromIndex, countryToIndex, vatRateIndex + 1)
        } else {
          routes.CheckSalesToEuController.onPageLoad(NormalMode, answers.period, countryFromIndex, countryToIndex)
        }
    }.getOrElse(routes.JourneyRecoveryController.onPageLoad())

  override def navigateInCheckMode(answers: UserAnswers): Call =
    answers.get(VatRatesFromEuPage(countryFromIndex, countryToIndex)).map {
      rates =>
        if (rates.size > vatRateIndex.position + 1) {
          routes.NetValueOfSalesFromEuController.onPageLoad(CheckMode, answers.period, countryFromIndex, countryToIndex, vatRateIndex + 1)
        } else {
          routes.CheckSalesToEuController.onPageLoad(CheckMode, answers.period, countryFromIndex, countryToIndex)
        }
    }.getOrElse(routes.JourneyRecoveryController.onPageLoad())

  override def navigateInCheckLoopMode(answers: UserAnswers): Call =
    routes.CheckSalesToEuController.onPageLoad(NormalMode, answers.period, countryFromIndex, countryToIndex)
}
