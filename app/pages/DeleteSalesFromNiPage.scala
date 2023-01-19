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
import models.{CheckMode, Index, NormalMode, UserAnswers}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.DeriveNumberOfSalesFromNi

case class DeleteSalesFromNiPage(index: Index) extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ PageConstants.salesFromNi \ index.position \ toString

  override def toString: String = "deleteSales"

  override def navigateInNormalMode(answers: UserAnswers): Call =
    answers.get(DeriveNumberOfSalesFromNi) match {
      case Some(n) if n > 0 => routes.SalesFromNiListController.onPageLoad(NormalMode, answers.period)
      case _                => routes.SoldGoodsFromNiController.onPageLoad(NormalMode, answers.period)
    }

  override def navigateInCheckMode(answers: UserAnswers): Call =
    answers.get(DeriveNumberOfSalesFromNi) match {
      case Some(n) if n > 0 => routes.SalesFromNiListController.onPageLoad(CheckMode, answers.period)
      case _                => routes.SoldGoodsFromNiController.onPageLoad(CheckMode, answers.period)
    }
}
