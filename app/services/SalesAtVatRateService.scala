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

package services

import models.{TotalVatToCountry, UserAnswers}
import queries.{AllSalesFromEuQuery, AllSalesFromNiQuery}
import queries.corrections.AllCorrectionPeriodsQuery

import javax.inject.Inject

class SalesAtVatRateService @Inject()() {

  def getNiTotalNetSales(userAnswers: UserAnswers): Option[BigDecimal] = {
    userAnswers.get(AllSalesFromNiQuery).map(allSales =>
      allSales.map(saleFromNi =>
        saleFromNi.vatRates.map(_.sales.map(_.netValueOfSales).getOrElse(BigDecimal(0))).sum
      ).sum
    )
  }

  def getNiTotalVatOnSales(userAnswers: UserAnswers): Option[BigDecimal] = {
    userAnswers.get(AllSalesFromNiQuery).map(allSales =>
      allSales.map(saleFromNi =>
        saleFromNi.vatRates.map(_.sales.map(
          _.vatOnSales.amount
        ).getOrElse(BigDecimal(0))).sum
      ).sum
    )
  }

  def getEuTotalNetSales(userAnswers: UserAnswers): Option[BigDecimal] = {
    userAnswers.get(AllSalesFromEuQuery).map(allSalesFromEu =>
      allSalesFromEu.map(
        _.salesFromCountry.map(_.vatRates.map(_.sales.map(_.netValueOfSales).getOrElse(BigDecimal(0))).sum
      ).sum
    ).sum
    )
  }

  def getEuTotalVatOnSales(userAnswers: UserAnswers): Option[BigDecimal] = {
    userAnswers.get(AllSalesFromEuQuery).map(allSalesFromEu =>
      allSalesFromEu.map (
        _.salesFromCountry.map (_.vatRates.map(_.sales.map(
          _.vatOnSales.amount
        ).getOrElse(BigDecimal(0))).sum
        ).sum
      ).sum
    )
  }

  def getTotalVatOwedAfterCorrections(userAnswers: UserAnswers): BigDecimal =
    getVatOwedToEuCountries(userAnswers).filter(vat => vat.totalVat > 0).map(_.totalVat).sum

  def getVatOwedToEuCountries(userAnswers: UserAnswers): List[TotalVatToCountry] = {

    val vatOwedToEuCountriesFromEu =
      for {
        allSalesFromEu <- userAnswers.get(AllSalesFromEuQuery).toSeq
        saleFromEu <- allSalesFromEu
        salesFromCountry <- saleFromEu.salesFromCountry
        salesAtVatRate   <- salesFromCountry.vatRates
      } yield TotalVatToCountry(salesFromCountry.countryOfConsumption, salesAtVatRate.sales.map(_.vatOnSales.amount).getOrElse(BigDecimal(0)))

    val vatOwedToEuCountriesFromNI =
      for {
        allSalesFromNi <- userAnswers.get(AllSalesFromNiQuery).toSeq
        saleFromNi     <- allSalesFromNi
        salesAtVatRate <- saleFromNi.vatRates
      } yield TotalVatToCountry(saleFromNi.countryOfConsumption, salesAtVatRate.sales.map(_.vatOnSales.amount).getOrElse(BigDecimal(0)))

    val correctionCountriesAmount =
      for {
        allCorrectionPeriods <- userAnswers.get(AllCorrectionPeriodsQuery).toSeq
        periodWithCorrections <- allCorrectionPeriods
        countryCorrection <- periodWithCorrections.correctionsToCountry.getOrElse(List.empty).filter(_.countryVatCorrection.isDefined)
      } yield TotalVatToCountry(countryCorrection.correctionCountry, countryCorrection.countryVatCorrection.getOrElse(BigDecimal(0)))

    val vatOwedToEuCountries =
      vatOwedToEuCountriesFromEu ++ vatOwedToEuCountriesFromNI ++ correctionCountriesAmount

    vatOwedToEuCountries.groupBy(_.country).map {
      case (country, salesToCountry) =>
        val totalVatToCountry = salesToCountry.map(_.totalVat).sum
        TotalVatToCountry(country, totalVatToCountry)
    }.toList
  }
}
