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

package services

import config.FrontendAppConfig
import models.{TotalVatToCountry, UserAnswers}
import queries.AllSalesFromEuQuery
import queries.AllSalesFromNiQuery
import queries.corrections.AllCorrectionPeriodsQuery

import javax.inject.Inject

class SalesAtVatRateService @Inject()(config: FrontendAppConfig) {

  def getNiTotalNetSales(userAnswers: UserAnswers): Option[BigDecimal]   = {
    userAnswers.get(AllSalesFromNiQuery).map(allSales =>
      allSales.map(saleFromNi =>
        saleFromNi.salesAtVatRate.map(_.netValueOfSales).sum
      ).sum
    )
  }

  def getNiTotalVatOnSales(userAnswers: UserAnswers): Option[BigDecimal] = {
    userAnswers.get(AllSalesFromNiQuery).map(allSales =>
      allSales.map(saleFromNi =>
        saleFromNi.salesAtVatRate.map(_.vatOnSales.amount).sum
      ).sum
    )
  }

  def getEuTotalNetSales(userAnswers: UserAnswers): Option[BigDecimal]   = {
    userAnswers.get(AllSalesFromEuQuery).map(allSalesFromEu =>
      allSalesFromEu.map(
        _.salesFromCountry.map(_.salesAtVatRate.map(_.netValueOfSales).sum).sum
      ).sum
    )
  }

  def getEuTotalVatOnSales(userAnswers: UserAnswers): Option[BigDecimal] = {
    userAnswers.get(AllSalesFromEuQuery).map(allSalesFromEu =>
      allSalesFromEu.map (
        _.salesFromCountry.map (_.salesAtVatRate.map(_.vatOnSales.amount).sum).sum
      ).sum
    )
  }

  def getVatOwedToEuCountries(userAnswers: UserAnswers): List[TotalVatToCountry] = {

    val vatOwedToEuCountriesFromEu =
      for {
        allSalesFromEu   <- userAnswers.get(AllSalesFromEuQuery).toSeq
        saleFromEu       <- allSalesFromEu
        salesFromCountry <- saleFromEu.salesFromCountry
        salesAtVatRate   <- salesFromCountry.salesAtVatRate
      } yield TotalVatToCountry(salesFromCountry.countryOfConsumption, salesAtVatRate.vatOnSales.amount)

    val vatOwedToEuCountriesFromNI =
      for {
        allSalesFromNi <- userAnswers.get(AllSalesFromNiQuery).toSeq
        saleFromNi     <- allSalesFromNi
        salesAtVatRate <- saleFromNi.salesAtVatRate
      } yield TotalVatToCountry(saleFromNi.countryOfConsumption, salesAtVatRate.vatOnSales.amount)

    val correctionCountriesAmount = if(config.correctionToggle) {
      for {
        allCorrectionPeriods <- userAnswers.get(AllCorrectionPeriodsQuery).toSeq
        periodWithCorrections <- allCorrectionPeriods
        countryCorrection <- periodWithCorrections.correctionsToCountry.filter(_.countryVatCorrection.isDefined)
      } yield TotalVatToCountry(countryCorrection.correctionCountry, countryCorrection.countryVatCorrection.get)
    } else List.empty

    val vatOwedToEuCountries =
      vatOwedToEuCountriesFromEu ++ vatOwedToEuCountriesFromNI ++ correctionCountriesAmount

    vatOwedToEuCountries.groupBy(_.country).map {
      case (country, salesToCountry) =>
        val totalVatToCountry = salesToCountry.map(_.totalVat).sum
        TotalVatToCountry(country, totalVatToCountry)
    }.toList
  }

  def getTotalVatOwedAfterCorrections(userAnswers: UserAnswers) : BigDecimal =
    getVatOwedToEuCountries(userAnswers).filter(vat => vat.totalVat > 0).map(_.totalVat).sum
}
