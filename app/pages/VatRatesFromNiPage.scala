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
import models.{CheckLoopMode, CheckMode, CheckSecondLoopMode, Index, NormalMode, UserAnswers, VatRate}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.AllSalesFromNiAtVatRateQuery

import scala.util.Try

case class VatRatesFromNiPage(index: Index) extends QuestionPage[List[VatRate]] {

  override def path: JsPath = JsPath \ PageConstants.salesFromNi \ index.position \ toString

  override def toString: String = PageConstants.vatRates

  override def navigateInNormalMode(answers: UserAnswers): Call =
    routes.NetValueOfSalesFromNiController.onPageLoad(NormalMode, answers.period, index, Index(0))

  override def navigateInCheckMode(answers: UserAnswers): Call =
    routes.NetValueOfSalesFromNiController.onPageLoad(CheckMode, answers.period, index, Index(0))

  override def navigateInCheckLoopMode(answers: UserAnswers): Call =
    routes.NetValueOfSalesFromNiController.onPageLoad(CheckLoopMode, answers.period, index, Index(0))

  override def navigateInCheckSecondLoopMode(answers: UserAnswers): Call =
    routes.NetValueOfSalesFromNiController.onPageLoad(CheckSecondLoopMode, answers.period, index, Index(0))

  override def cleanup(value: Option[List[VatRate]], userAnswers: UserAnswers): Try[UserAnswers] =
    userAnswers.remove(AllSalesFromNiAtVatRateQuery(index))
}
