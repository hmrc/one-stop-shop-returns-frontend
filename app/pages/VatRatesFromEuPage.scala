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
import models.{CheckLoopMode, Index, Mode, NormalMode, UserAnswers, VatRate}
import pages.PageConstants._
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.AllEuVatRateAndSalesQuery

case class VatRatesFromEuPage(countryFromIndex: Index, countryToIndex: Index) extends QuestionPage[List[VatRate]] {

  override def path: JsPath = JsPath \ salesFromEu \ countryFromIndex.position \ salesFromCountry \ countryToIndex.position \ toString

  override def toString: String = PageConstants.vatRates

  override def navigate(mode: Mode, answers: UserAnswers): Call = {

    val vatRateIndex: Option[Int] =
      answers.get(AllEuVatRateAndSalesQuery(countryFromIndex, countryToIndex)).flatMap(_.zipWithIndex.find(_._1.sales.isEmpty).map(_._2))

    vatRateIndex match {
      case Some(index) => routes.NetValueOfSalesFromEuController.onPageLoad(mode, answers.period, countryFromIndex, countryToIndex, Index(index))
      case None =>
        mode match{
          case CheckLoopMode => routes.CheckSalesToEuController.onPageLoad(NormalMode, answers.period, countryFromIndex, countryToIndex)
          case _ => routes.CheckSalesToEuController.onPageLoad(mode, answers.period, countryFromIndex, countryToIndex)
        }

    }

  }
}
