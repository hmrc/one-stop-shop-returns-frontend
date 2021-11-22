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
import models.{CheckLoopMode, CheckMode, CheckSecondLoopMode, CheckThirdLoopMode, Index, NormalMode, UserAnswers}
import pages.PageConstants.{netValueOfSales, salesAtVatRate, salesFromCountry, salesFromEu}
import play.api.libs.json.JsPath
import play.api.mvc.Call

import scala.util.Try

case class NetValueOfSalesFromEuPage(countryFromIndex: Index, countryToIndex: Index, vatRateIndex: Index) extends QuestionPage[BigDecimal] {

  override def path: JsPath =
    JsPath \ salesFromEu \ countryFromIndex.position \ salesFromCountry \ countryToIndex.position \ salesAtVatRate \ vatRateIndex.position \ toString

  override def toString: String = netValueOfSales

  override def navigateInNormalMode(answers: UserAnswers): Call =
    routes.VatOnSalesFromEuController.onPageLoad(NormalMode, answers.period, countryFromIndex, countryToIndex, vatRateIndex)

  override def navigateInCheckMode(answers: UserAnswers): Call =
    routes.VatOnSalesFromEuController.onPageLoad(CheckMode, answers.period, countryFromIndex, countryToIndex, vatRateIndex)

  override def navigateInCheckLoopMode(answers: UserAnswers): Call =
    routes.VatOnSalesFromEuController.onPageLoad(CheckLoopMode, answers.period, countryFromIndex, countryToIndex, vatRateIndex)

  override def navigateInCheckSecondLoopMode(answers: UserAnswers): Call =
    routes.VatOnSalesFromEuController.onPageLoad(CheckSecondLoopMode, answers.period, countryFromIndex, countryToIndex, vatRateIndex)

  override def navigateInCheckThirdLoopMode(answers: UserAnswers): Call =
    routes.VatOnSalesFromEuController.onPageLoad(CheckThirdLoopMode, answers.period, countryFromIndex, countryToIndex, vatRateIndex)

  override def cleanup(value: Option[BigDecimal], userAnswers: UserAnswers): Try[UserAnswers] =
    userAnswers.remove(VatOnSalesFromEuPage(countryFromIndex, countryToIndex, vatRateIndex))
}
