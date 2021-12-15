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

package utils

import models.{Index, Period, VatRateAndSales}
import models.corrections.CorrectionToCountry
import models.requests.DataRequest
import pages.corrections.CorrectionReturnPeriodPage
import play.api.mvc.{AnyContent, Result}
import queries.{AllNiVatRateAndSalesQuery, AllSalesFromNiQuery}
import queries.corrections.{AllCorrectionCountriesQuery, AllCorrectionPeriodsQuery, DeriveCompletedCorrectionPeriods, DeriveNumberOfCorrections}

import scala.concurrent.Future

trait CompletionChecks {

  def getIncompleteCorrections(periodIndex: Index)(implicit request: DataRequest[AnyContent]): List[CorrectionToCountry] = {
    request.userAnswers
      .get(AllCorrectionCountriesQuery(periodIndex))
      .map(_.filter(_.countryVatCorrection.isEmpty)).getOrElse(List.empty)
  }

  def getPeriodsWithIncompleteCorrections()(implicit request: DataRequest[AnyContent]): List[Period] = {
    request.userAnswers
      .get(AllCorrectionPeriodsQuery)
      .map(_.filter(_.correctionsToCountry.getOrElse(List.empty).exists(_.countryVatCorrection.isEmpty)))
      .map(_.map(_.correctionReturnPeriod))
      .getOrElse(List())
  }

  def firstIndexedIncompleteCorrection(periodIndex: Index, incompleteCorrections: List[CorrectionToCountry])
                                      (implicit request: DataRequest[AnyContent]): Option[(CorrectionToCountry, Int)] = {
    request.userAnswers.get(AllCorrectionCountriesQuery(periodIndex))
      .getOrElse(List.empty).zipWithIndex
      .find(indexedCorrection => incompleteCorrections.contains(indexedCorrection._1))
  }

  protected def withCompleteCorrections(periodIndex: Index, onFailure: List[CorrectionToCountry] => Result)
                                      (onSuccess: => Result)
                                      (implicit request: DataRequest[AnyContent]): Result = {

    val incomplete = getIncompleteCorrections(periodIndex)
    if(incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }

  protected def withCompleteCorrections(onFailure: List[Period] => Result)
                                       (onSuccess: => Result)
                                       (implicit request: DataRequest[AnyContent]): Result = {

    val incomplete = getPeriodsWithIncompleteCorrections()
    if(incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }

  protected def withCompleteCorrectionsAsync(onFailure: List[Period] => Future[Result])
                                       (onSuccess: => Future[Result])
                                       (implicit request: DataRequest[AnyContent]): Future[Result] = {

    val incomplete = getPeriodsWithIncompleteCorrections()
    if(incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }

  def getIncompleteNiVatRateAndSales(countryIndex: Index)(implicit request: DataRequest[AnyContent]): Seq[VatRateAndSales] = {
    request.userAnswers
      .get(AllNiVatRateAndSalesQuery(countryIndex))
      .map(_.filter(_.sales.isEmpty)).getOrElse(List.empty)
  }

  protected def withCompleteVatRateAndSales(countryIndex: Index, onFailure: Seq[VatRateAndSales] => Result)
                                       (onSuccess: => Result)
                                       (implicit request: DataRequest[AnyContent]): Result = {

    val incomplete = getIncompleteNiVatRateAndSales(countryIndex)
    if(incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }
}
