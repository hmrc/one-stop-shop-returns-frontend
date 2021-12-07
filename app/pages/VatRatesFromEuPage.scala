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
import models.{CheckLoopMode, CheckMode, CheckSecondLoopMode, CheckThirdLoopMode, Index, Mode, NormalMode, UserAnswers, VatRate, VatRateAndSales}
import pages.PageConstants._
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.{AllEuVatRateAndSalesQuery, AllSalesFromEuQuery, EuSalesAtVatRateQuery}

import scala.util.{Failure, Success, Try}

case class VatRatesFromEuPage(countryFromIndex: Index, countryToIndex: Index) extends QuestionPage[List[VatRate]] {

  override def path: JsPath = JsPath \ salesFromEu \ countryFromIndex.position \ salesFromCountry \ countryToIndex.position \ toString

  override def toString: String = PageConstants.vatRates

  override def navigate(mode: Mode, answers: UserAnswers): Call = {

    val vatRateIndex: Option[Int] =
      answers.get(AllEuVatRateAndSalesQuery(countryFromIndex, countryToIndex)).flatMap(_.zipWithIndex.find(_._1.sales.isEmpty).map(_._2))

    vatRateIndex match {
      case Some(index) => routes.NetValueOfSalesFromEuController.onPageLoad(mode, answers.period, countryFromIndex, countryToIndex, Index(index))
      case None => routes.CheckSalesToEuController.onPageLoad(mode, answers.period, countryFromIndex, countryToIndex)
    }

  }
//
//  override def navigateInNormalMode(answers: UserAnswers): Call =
//    routes.NetValueOfSalesFromEuController.onPageLoad(NormalMode, answers.period, countryFromIndex, countryToIndex, Index(0))
//
//  override def navigateInCheckMode(answers: UserAnswers): Call =
//    routes.NetValueOfSalesFromEuController.onPageLoad(CheckMode, answers.period, countryFromIndex, countryToIndex, Index(0))
//
//  override def navigateInCheckLoopMode(answers: UserAnswers): Call =
//    routes.NetValueOfSalesFromEuController.onPageLoad(CheckLoopMode, answers.period, countryFromIndex, countryToIndex, Index(0))
//
//  override def navigateInCheckSecondLoopMode(answers: UserAnswers): Call =
//    routes.NetValueOfSalesFromEuController.onPageLoad(CheckSecondLoopMode, answers.period, countryFromIndex, countryToIndex, Index(0))
//
//  override def navigateInCheckThirdLoopMode(answers: UserAnswers): Call =
//    routes.NetValueOfSalesFromEuController.onPageLoad(CheckThirdLoopMode, answers.period, countryFromIndex, countryToIndex, Index(0))

//  override def cleanup(value: Option[List[VatRate]], userAnswers: UserAnswers): Try[UserAnswers] = {
//    val x: Option[Try[UserAnswers]] = for {
//       presentVatRates <- userAnswers.get(VatRatesFromEuPage(countryFromIndex, countryToIndex))
//       vatRatesOnSales <- userAnswers.get(AllSalesFromEuQuery)
//    } yield {
//      val vatRates = vatRatesOnSales.flatMap(_.salesFromCountry.flatMap(_.salesAtVatRate.map(_.vatOnSales.vatRate))).zipWithIndex
//      val indexesToRemove = vatRates.filterNot(rateWithIndex => presentVatRates.contains(rateWithIndex._1)).map(_._2)
//      indexesToRemove.foldLeft(Try(userAnswers))((ua, index) => ua.flatMap(_.remove(EuSalesAtVatRateQuery(countryFromIndex, countryToIndex, Index(index)))))
//    }
//
//    x.getOrElse(Success(userAnswers))
//  }
}
