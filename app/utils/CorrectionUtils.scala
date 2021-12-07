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

import models.corrections.CorrectionPayload
import models.domain.VatReturn
import models.Country

object CorrectionUtils {

  def groupByCountryAndSum(correctionPayload: CorrectionPayload, vatReturn: VatReturn): Map[Country, BigDecimal] = {
    val returnAmountsToAllCountriesFromNi = (for {
      salesFromNi <- vatReturn.salesFromNi
    } yield {
      Map(salesFromNi.countryOfConsumption -> salesFromNi.amounts.map(_.vatOnSales.amount).sum)
    }).flatten.toMap

    val returnAmountsToAllCountriesFromEu = vatReturn.salesFromEu.flatMap(_.sales).groupBy(_.countryOfConsumption).flatMap {
      case (country, salesToCountry) => {
        val totalAmount = salesToCountry.flatMap(_.amounts.map(_.vatOnSales.amount)).sum

        Map(country -> totalAmount)
      }
    }

    val returnAmountsToAllCountries = returnAmountsToAllCountriesFromNi ++ returnAmountsToAllCountriesFromEu.map {
      case (country, amount) =>
        country -> (amount + returnAmountsToAllCountriesFromNi.getOrElse(country, BigDecimal(0)))
    }


    val correctionsToAllCountries = for {
      correctionPeriods <- correctionPayload.corrections
      correctionToCountry <- correctionPeriods.correctionsToCountry
    } yield correctionToCountry

    val correctionAmountsToAllCountries = correctionsToAllCountries.groupBy(_.correctionCountry).flatMap {
      case (country, corrections) =>
        val total = corrections.map(_.countryVatCorrection).sum

        Map(country -> total)
    }

    correctionAmountsToAllCountries ++ returnAmountsToAllCountries.map {
      case (country, amount) =>
        country -> (amount + correctionAmountsToAllCountries.getOrElse(country, BigDecimal(0)))
    }
  }

}
