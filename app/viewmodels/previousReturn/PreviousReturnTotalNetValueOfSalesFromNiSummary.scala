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
import models.etmp.EtmpVatReturnGoodsSupplied
import play.api.i18n.Messages
import viewmodels.govuk.all.currencyFormat

object PreviousReturnTotalNetValueOfSalesFromNiSummary {

  case class SalesFromNiCountryRow(
                                  country: String,
                                  salesAmount: String,
                                  vatAmount: String
                                )
  
  def rows(goodsSupplied: Seq[EtmpVatReturnGoodsSupplied])(implicit messages: Messages): Seq[SalesFromNiCountryRow] = {

    goodsSupplied.map { goodsSuppliedToCountry =>

      SalesFromNiCountryRow(
        country = messages("newPreviousReturn.salesFromNi.toCountry", getCountryName(goodsSuppliedToCountry.msOfConsumption)),
        salesAmount = currencyFormat(goodsSuppliedToCountry.taxableAmountGBP),
        vatAmount = currencyFormat(goodsSuppliedToCountry.vatAmountGBP)
      )
    }
  }
}

