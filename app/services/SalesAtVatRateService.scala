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

import models.Country.northernIreland
import models.{TotalVatToCountry, UserAnswers}
import queries.AllSalesFromEuQuery
import queries.AllSalesFromNiQuery

import javax.inject.Inject

class SalesAtVatRateService @Inject()() {

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
        saleFromNi.salesAtVatRate.map(_.vatOnSales).sum
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
        _.salesFromCountry.map (_.salesAtVatRate.map(_.vatOnSales).sum).sum
      ).sum
    )
  }

  def getTotalNetSales(userAnswers: UserAnswers): BigDecimal   = {
    val niNet = getNiTotalNetSales(userAnswers).getOrElse(BigDecimal(0))
    val euNet = getEuTotalNetSales(userAnswers).getOrElse(BigDecimal(0))

    niNet + euNet
  }

  def getTotalVatOnSales(userAnswers: UserAnswers): BigDecimal = {
    val niVat = getNiTotalVatOnSales(userAnswers).getOrElse(BigDecimal(0))
    val euVat = getEuTotalVatOnSales(userAnswers).getOrElse(BigDecimal(0))

    niVat + euVat
  }

  def getVatOwedToEuCountries(userAnswers: UserAnswers): List[TotalVatToCountry] = {

    val vatOwedToEuCountriesFromEu =
      userAnswers.get(AllSalesFromEuQuery).map(
        _.flatMap(
          _.salesFromCountry.flatMap(salesFromCountry =>
            salesFromCountry.salesAtVatRate.map(saleAtVatRate =>
              TotalVatToCountry(salesFromCountry.countryOfConsumption, saleAtVatRate.vatOnSales)
            )
          )
        )
      ).getOrElse(List.empty)

    val vatOwedToEuCountriesFromNI =
      userAnswers.get(AllSalesFromNiQuery).map(allSales =>
        allSales.flatMap(_.salesAtVatRate.map(saleAtVatRate =>
          TotalVatToCountry(northernIreland, saleAtVatRate.vatOnSales))
        )
      ).getOrElse(List.empty)

    val vatOwedToEuCountries =
      vatOwedToEuCountriesFromEu ++ vatOwedToEuCountriesFromNI

    vatOwedToEuCountries.groupBy(_.country).map {
      case (country, salesToCountry) =>
        val totalVatToCountry = salesToCountry.map(_.totalVat).sum
        TotalVatToCountry(country, totalVatToCountry)
    }.toList
  }
}
