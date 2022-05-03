/*
 * Copyright 2022 HM Revenue & Customs
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

import cats.implicits._
import models.corrections.CorrectionToCountry
import models.requests.DataRequest
import models.{Country, Index, Period, SalesFromCountryWithOptionalVat, SalesFromEuWithOptionalVat, VatRateAndSalesWithOptionalVat}
import play.api.mvc.{AnyContent, Result}
import queries.corrections.{AllCorrectionCountriesQuery, AllCorrectionPeriodsQuery, CorrectionToCountryQuery}
import queries.{AllNiVatRateAndSalesWithOptionalVatQuery, AllSalesFromEuQueryWithOptionalVatQuery, AllSalesFromNiWithOptionalVatQuery, AllSalesToEuWithOptionalVatQuery}

import scala.concurrent.Future

trait CompletionChecks {

  protected def withCompleteData[A]( index: Index, data: Index => Seq[A], onFailure: Seq[A] => Result)
                                ( onSuccess: => Result): Result = {

    val incomplete = data(index)
    if(incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }

  protected def withCompleteDataAsync[A](data: () => Seq[A], onFailure: Seq[A] => Future[Result])
                                   ( onSuccess: => Future[Result]): Future[Result] = {

    val incomplete = data()
    if(incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }

  protected def withCompleteData[A]( data: () => Seq[A], onFailure: Seq[A] => Result)
                                   ( onSuccess: => Result): Result = {
    val incomplete = data()
    if(incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }

  // Corrections queries

  def getIncompleteCorrectionToCountry(periodIndex: Index, countryIndex: Index)(implicit request: DataRequest[AnyContent]): Option[CorrectionToCountry] = {
    request.userAnswers
      .get(CorrectionToCountryQuery(periodIndex, countryIndex))
      .find(_.countryVatCorrection.isEmpty)
  }

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

  def firstIndexedIncompleteCorrection(periodIndex: Index, incompleteCorrections: Seq[CorrectionToCountry])
                                      (implicit request: DataRequest[AnyContent]): Option[(CorrectionToCountry, Int)] = {
    request.userAnswers.get(AllCorrectionCountriesQuery(periodIndex))
      .getOrElse(List.empty).zipWithIndex
      .find(indexedCorrection => incompleteCorrections.contains(indexedCorrection._1))
  }

  // NI queries

  def getNiCountriesWithIncompleteSales()(implicit request: DataRequest[AnyContent]): Seq[Country] = {
    request.userAnswers
      .get(AllSalesFromNiWithOptionalVatQuery)
      .map(_.filter(sales =>
        sales.vatRates.isEmpty ||
          sales.vatRates.getOrElse(List.empty).exists(
            rate => rate.sales.isEmpty || rate.sales.exists(_.vatOnSales.isEmpty)
          )
      ))
      .map(_.map(_.countryOfConsumption))
      .getOrElse(Seq.empty)
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

  def firstIndexedIncompleteNiCountrySales(incompleteCountries: Seq[Country])
                                          (implicit request: DataRequest[AnyContent]): Option[(SalesFromCountryWithOptionalVat, Int)] = {
    request.userAnswers.get(AllSalesFromNiWithOptionalVatQuery)
      .getOrElse(List.empty).zipWithIndex
      .find(indexedCorrection => incompleteCountries.contains(indexedCorrection._1.countryOfConsumption))
  }

  // To EU Queries

  def getIncompleteToEuSales(index: Index)(implicit request: DataRequest[AnyContent]): List[SalesFromCountryWithOptionalVat] = {
    request.userAnswers
      .get(AllSalesToEuWithOptionalVatQuery(index))
      .map(_.filter(sales => sales.vatRates.isEmpty || sales.vatRates.exists(_.exists(_.sales.isEmpty))))
      .getOrElse(List.empty)
  }

  def firstIndexedIncompleteSaleToEu(index: Index)
                                    (implicit request: DataRequest[AnyContent]): Option[(SalesFromCountryWithOptionalVat, Int)] = {
    request.userAnswers.get(AllSalesToEuWithOptionalVatQuery(index))
      .getOrElse(List.empty).zipWithIndex
      .find(indexedSale => indexedSale._1.vatRates.isEmpty || indexedSale._1.vatRates.exists(_.exists(_.sales.isEmpty)))
  }

  // From EU Queries

  def getIncompleteFromEuSales()(implicit request: DataRequest[AnyContent]): List[SalesFromEuWithOptionalVat] = {
    request.userAnswers
      .get(AllSalesFromEuQueryWithOptionalVatQuery)
      .map(_.filter(sales =>
        sales.salesFromCountry.isEmpty ||
        sales.salesFromCountry.getOrElse(List.empty).exists(_.vatRates.isEmpty) ||
        sales.salesFromCountry.getOrElse(List.empty).exists(_.vatRates.exists(_.exists(_.sales.isEmpty)))
      ))
      .getOrElse(List.empty)
  }

  def firstIndexedIncompleteSaleFromEu()
                                    (implicit request: DataRequest[AnyContent]): Option[(SalesFromEuWithOptionalVat, Int)] = {
    request.userAnswers.get(AllSalesFromEuQueryWithOptionalVatQuery)
      .getOrElse(List.empty).zipWithIndex
      .find(indexedSale =>
        indexedSale._1.salesFromCountry.isEmpty ||
        indexedSale._1.salesFromCountry.getOrElse(List.empty).exists(_.vatRates.isEmpty) ||
        indexedSale._1.salesFromCountry.getOrElse(List.empty).exists(_.vatRates.exists(_.exists(_.sales.isEmpty))
        ))
  }
}
