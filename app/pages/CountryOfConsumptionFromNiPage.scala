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
import models.{CheckMode, Country, Index, NormalMode, UserAnswers}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.AllSalesAtVatRateQuery

import scala.util.Try

case class CountryOfConsumptionFromNiPage(index: Index) extends QuestionPage[Country] {

  override def path: JsPath = JsPath \ PageConstants.salesFromNi \ index.position \ toString

  override def toString: String = "countryOfConsumption"

  override def navigateInNormalMode(answers: UserAnswers): Call =
    routes.VatRatesFromNiController.onPageLoad(NormalMode, answers.period, index)

  override def navigateInCheckMode(answers: UserAnswers): Call =
    routes.VatRatesFromNiController.onPageLoad(CheckMode, answers.period, index)

  override def cleanup(value: Option[Country], userAnswers: UserAnswers): Try[UserAnswers] = {
    value match {
      case Some(_) => userAnswers
        .remove(VatRatesFromNiPage(index))
        .flatMap(_.remove(AllSalesAtVatRateQuery(index)))
      case _ => super.cleanup(value, userAnswers)
    }
  }
}
