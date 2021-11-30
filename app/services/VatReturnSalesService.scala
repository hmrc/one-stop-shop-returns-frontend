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

import models.corrections.{CorrectionPayload, PeriodWithCorrections}
import models.domain.{SalesFromEuCountry, SalesToCountry, VatReturn}

import javax.inject.Inject

class VatReturnSalesService @Inject()() {

  def getTotalVatOnSalesBeforeCorrection(vatReturn: VatReturn): BigDecimal = {
    val niVat = getTotalVatOnSalesToCountry(vatReturn.salesFromNi)
    val euVat = getEuTotalVatOnSales(vatReturn.salesFromEu)

    niVat + euVat
  }

  def getTotalVatOnSalesAfterCorrection(vatReturn: VatReturn, maybeCorrectionPayload: Option[CorrectionPayload]): BigDecimal = {
    val correction = maybeCorrectionPayload.map {
      correctionPayload =>
        getCorrectionAmount(correctionPayload.corrections)
    }.getOrElse(BigDecimal(0))

    getTotalVatOnSalesBeforeCorrection(vatReturn) + correction
  }

  def getEuTotalVatOnSales(allSalesFromEu: List[SalesFromEuCountry]): BigDecimal = {
    allSalesFromEu.map { salesFromAnEuCountry =>
      getTotalVatOnSalesToCountry(salesFromAnEuCountry.sales)
    }.sum
  }

  def getTotalVatOnSalesToCountry(allSales: List[SalesToCountry]): BigDecimal = {
    allSales.map(salesToCountry =>
      salesToCountry.amounts.map(_.vatOnSales.amount).sum
    ).sum
  }

  def getEuTotalNetSales(allSalesFromEu: List[SalesFromEuCountry]): BigDecimal = {
    allSalesFromEu.map { salesFromAnEuCountry =>
      getTotalNetSalesToCountry(salesFromAnEuCountry.sales)
    }.sum
  }

  def getTotalNetSalesToCountry(allSales: List[SalesToCountry]): BigDecimal = {
    allSales.map(salesToCountry =>
      salesToCountry.amounts.map(_.netValueOfSales).sum
    ).sum
  }

  def getCorrectionAmount(periodWithCorrections: List[PeriodWithCorrections]): BigDecimal = {
    periodWithCorrections.map { periodWithCorrection =>
      periodWithCorrection.correctionsToCountry.map {
        correctionToCountry =>
          if (correctionToCountry.countryVatCorrection < 0) {
            BigDecimal(0)
          } else {
            correctionToCountry.countryVatCorrection
          }
      }.sum
    }.sum
  }

}
