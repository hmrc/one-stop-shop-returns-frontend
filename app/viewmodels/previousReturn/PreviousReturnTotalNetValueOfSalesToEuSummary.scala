/*
 * Copyright 2025 HM Revenue & Customs
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

package viewmodels.previousReturn

import models.Country
import models.Country.getCountryName
import models.etmp.EtmpVatReturnGoodsDispatched
import play.api.i18n.Messages
import viewmodels.govuk.all.currencyFormat

object PreviousReturnTotalNetValueOfSalesToEuSummary {

  case class SalesToEuCountryRow(
                                country: String,
                                salesAmount: String,
                                vatAmount: String
                              )

  def rows(goodsSupplied: Seq[EtmpVatReturnGoodsDispatched])(implicit messages: Messages): Map[String, Seq[SalesToEuCountryRow]] = {

    goodsSupplied.groupBy(_.msOfEstablishment).flatMap {
      case (country, goodsSuppliedFromCountry) =>

        val countryRows = goodsSuppliedFromCountry.map { singleGoodsSupplied =>
          SalesToEuCountryRow(
            country = messages("newPreviousReturn.salesToEu.toCountry", getCountryName(singleGoodsSupplied.msOfConsumption)),
            salesAmount = currencyFormat(singleGoodsSupplied.taxableAmountGBP),
            vatAmount = currencyFormat(singleGoodsSupplied.vatAmountGBP)
          )
        }

        Map(getCountryName(country) -> countryRows)
    }
  }
}

