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

import models.{Country, Index, Period, SalesFromCountryWithOptionalVat, SalesFromEuWithOptionalVat, VatRateAndSales, VatRateAndSalesWithOptionalVat}
import models.corrections.CorrectionToCountry
import models.requests.DataRequest
import org.checkerframework.checker.units.qual.A
import pages.corrections.CorrectionReturnPeriodPage
import play.api.mvc.{AnyContent, Result}
import queries.{AllNiVatRateAndSalesQuery, AllNiVatRateAndSalesWithOptionalVatQuery, AllSalesFromEuQuery, AllSalesFromEuQueryWithOptionalVatQuery, AllSalesFromNiQuery, AllSalesFromNiWithOptionalVatQuery, AllSalesToEuWithOptionalVatQuery}
import queries.corrections.{AllCorrectionCountriesQuery, AllCorrectionPeriodsQuery, DeriveCompletedCorrectionPeriods, DeriveNumberOfCorrections}

import scala.concurrent.Future

trait CompletionChecks {

  def getIncompleteCorrections(periodIndex: Index)(implicit request: DataRequest[AnyContent]): List[CorrectionToCountry] = {
    request.userAnswers
      .get(AllCorrectionCountriesQuery(periodIndex))
      .map(_.filter(_.countryVatCorrection.isEmpty)).getOrElse(List.empty)
  }

  def getIncompleteToEuSales()(implicit request: DataRequest[AnyContent]): List[SalesFromCountryWithOptionalVat] = {
    request.userAnswers
      .get(AllSalesFromEuQueryWithOptionalVatQuery)
      .map(_.flatMap(_.salesFromCountry).filter(y => y.vatRates.isEmpty || y.vatRates.exists(_.exists(_.sales.isEmpty))))
      .getOrElse(List.empty)
  }

  def firstIndexedIncompleteSaleToEu(index: Index)
                                      (implicit request: DataRequest[AnyContent]): Option[(SalesFromCountryWithOptionalVat, Int)] = {
    request.userAnswers.get(AllSalesToEuWithOptionalVatQuery(index))
      .getOrElse(List.empty).zipWithIndex
      .find(indexedSale => indexedSale._1.vatRates.isEmpty || indexedSale._1.vatRates.exists(_.exists(_.sales.isEmpty)))
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

  protected def withCompleteEuSales(onFailure: List[SalesFromCountryWithOptionalVat] => Result)
                                       (onSuccess: => Result)
                                       (implicit request: DataRequest[AnyContent]): Result = {

    val incomplete = getIncompleteToEuSales()
    if(incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
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

  def getNiCountriesWithIncompleteSales()(implicit request: DataRequest[AnyContent]): List[Country] = {
    request.userAnswers
      .get(AllSalesFromNiWithOptionalVatQuery)
      .map(_.filter(sales =>
        sales.vatRates.isEmpty ||
        sales.vatRates.getOrElse(List.empty).exists(
          rate => rate.sales.isEmpty || rate.sales.exists(_.vatOnSales.isEmpty)
        )
      ))
      .map(_.map(_.countryOfConsumption))
      .getOrElse(List())
  }

  def getIncompleteNiVatRateAndSales(countryIndex: Index)(implicit request: DataRequest[AnyContent]): Seq[VatRateAndSalesWithOptionalVat] = {
    val noSales = request.userAnswers
      .get(AllNiVatRateAndSalesWithOptionalVatQuery(countryIndex))
      .map(_.filter(_.sales.isEmpty)).getOrElse(List.empty)

    val noVat =  request.userAnswers
      .get(AllNiVatRateAndSalesWithOptionalVatQuery(countryIndex))
      .map(
        _.filter(v =>
        v.sales.exists(_.vatOnSales.isEmpty)
        )
      ).getOrElse(List.empty)

    noSales ++ noVat
  }

  protected def withCompleteVatRateAndSales(countryIndex: Index, onFailure: Seq[VatRateAndSalesWithOptionalVat] => Result)
                                       (onSuccess: => Result)
                                       (implicit request: DataRequest[AnyContent]): Result = {

    val incomplete = getIncompleteNiVatRateAndSales(countryIndex)
    if(incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }

  protected def withCompleteNiSalesForCountry(onFailure: List[Country] => Result)
                                       (onSuccess: => Result)
                                       (implicit request: DataRequest[AnyContent]): Result = {

    val incomplete = getNiCountriesWithIncompleteSales()
    if(incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }

  def firstIndexedIncompleteNiCountrySales(incompleteCountries: List[Country])
                                      (implicit request: DataRequest[AnyContent]): Option[(SalesFromCountryWithOptionalVat, Int)] = {
    request.userAnswers.get(AllSalesFromNiWithOptionalVatQuery)
      .getOrElse(List.empty).zipWithIndex
      .find(indexedCorrection => incompleteCountries.contains(indexedCorrection._1.countryOfConsumption))
  }
}
