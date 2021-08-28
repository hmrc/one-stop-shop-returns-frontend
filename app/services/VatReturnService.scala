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

import cats.implicits._
import models.domain.{SalesDetails, SalesFromEuCountry, SalesToCountry}
import models._
import models.requests.VatReturnRequest
import pages._
import queries.{AllSalesFromEuQuery, AllSalesFromNiQuery, AllSalesToEuQuery, SalesFromEuQuery}
import uk.gov.hmrc.domain.Vrn

import javax.inject.Inject

class VatReturnService @Inject()() {

  def fromUserAnswers(answers: UserAnswers, vrn: Vrn, period: Period): ValidationResult[VatReturnRequest] =
    (
      getSalesFromNi(answers),
      getSalesFromEu(answers)
    ).mapN(
      (salesFromNi, salesFromEu) =>
        VatReturnRequest(vrn, period, None, None, salesFromNi, salesFromEu)
    )

  private def getSalesFromNi(answers: UserAnswers): ValidationResult[List[SalesToCountry]] =
    answers.get(SoldGoodsFromNiPage) match {
      case Some(true) =>
        processSalesFromNi(answers)

      case Some(false) =>
        List.empty[SalesToCountry].validNec

      case None =>
        DataMissingError(SoldGoodsFromNiPage).invalidNec
    }

  private def processSalesFromNi(answers: UserAnswers): ValidationResult[List[SalesToCountry]] = {
    answers.get(AllSalesFromNiQuery) match {
      case Some(salesFromNi) if salesFromNi.nonEmpty =>
        salesFromNi.zipWithIndex.map {
          case (_, index) =>
            processSalesFromNiToCountry(answers, Index(index))
        }.sequence.map {
          salesDetails =>
            salesFromNi.zip(salesDetails).map {
              case (sales, salesDetails) =>
                SalesToCountry(sales.countryOfConsumption, salesDetails)
            }
        }

      case None =>
        DataMissingError(AllSalesFromNiQuery).invalidNec
    }
  }

  private def processSalesFromNiToCountry(answers: UserAnswers, countryIndex: Index): ValidationResult[List[SalesDetails]] =
    answers.get(VatRatesFromNiPage(countryIndex)) match {
      case Some(list) if list.nonEmpty =>
        list.zipWithIndex.map {
          case (vatRate, index) =>
            processSalesFromNiAtVatRate(answers, countryIndex, Index(index), vatRate)
        }.sequence

      case _ =>
        DataMissingError(VatRatesFromNiPage(countryIndex)).invalidNec
    }

  private def processSalesFromNiAtVatRate(answers: UserAnswers, countryIndex: Index, vatRateIndex: Index, vatRate: VatRate): ValidationResult[SalesDetails] =
    answers.get(SalesAtVatRateFromNiPage(countryIndex, vatRateIndex)) match {
      case Some(sales) =>
        SalesDetails(
          vatRate         = vatRate,
          netValueOfSales = sales.netValueOfSales,
          vatOnSales      = sales.vatOnSales
        ).validNec

      case None =>
        DataMissingError(SalesAtVatRateFromNiPage(countryIndex, vatRateIndex)).invalidNec
    }


  private def getSalesFromEu(answers: UserAnswers): ValidationResult[List[SalesFromEuCountry]] =
    answers.get(SoldGoodsFromEuPage) match {
      case Some(true) =>
        processSalesFromEu(answers)

      case Some(false) =>
        List.empty[SalesFromEuCountry].validNec

      case None =>
        DataMissingError(SoldGoodsFromEuPage).invalidNec
    }

  // TODO: Include tax identifier if we have it, instead of hardcoded `None`
  private def processSalesFromEu(answers: UserAnswers): ValidationResult[List[SalesFromEuCountry]] =
    answers.get(AllSalesFromEuQuery) match {
      case Some(salesFromEu) if salesFromEu.nonEmpty =>
        salesFromEu.zipWithIndex.map {
          case (_, index) =>
            processSalesFromEuCountry(answers, Index(index))
        }.sequence.map {
          salesDetails =>
            salesFromEu.zip(salesDetails).map {
              case (sales, salesDetails) =>
                SalesFromEuCountry(sales.countryOfSale, None, salesDetails)
            }
        }

      case None =>
        DataMissingError(AllSalesFromEuQuery).invalidNec
    }

  private def processSalesFromEuCountry(answers: UserAnswers, countryFromIndex: Index): ValidationResult[List[SalesToCountry]] =
    answers.get(AllSalesToEuQuery(countryFromIndex)) match {
      case Some(salesToEu) if salesToEu.nonEmpty =>
        salesToEu.zipWithIndex.map {
          case (_, index) =>
            processSalesToEuCountry(answers, countryFromIndex, Index(index))
        }.sequence.map {
          salesDetails =>
            salesToEu.zip(salesDetails).map {
              case (sales, salesDetails) =>
                SalesToCountry(sales.countryOfConsumption, salesDetails)
            }
        }

      case None =>
        DataMissingError(AllSalesToEuQuery(countryFromIndex)).invalidNec
    }

  private def processSalesToEuCountry(answers: UserAnswers, countryFromIndex: Index, countryToIndex: Index): ValidationResult[List[SalesDetails]] =
    answers.get(VatRatesFromEuPage(countryFromIndex, countryToIndex)) match {
      case Some(list) if list.nonEmpty =>
        list.zipWithIndex.map {
          case (vatRate, index) =>
            processSalesFromEuAtVatRate(answers, countryFromIndex, countryToIndex, Index(index), vatRate)
        }.sequence

      case _ =>
        DataMissingError(VatRatesFromEuPage(countryFromIndex, countryToIndex)).invalidNec
    }

  private def processSalesFromEuAtVatRate(
                                           answers: UserAnswers,
                                           countryFromIndex: Index,
                                           countryToIndex: Index,
                                           vatRateIndex: Index,
                                           vatRate: VatRate
                                         ): ValidationResult[SalesDetails] =
    answers.get(SalesAtVatRateFromEuPage(countryFromIndex, countryToIndex, vatRateIndex)) match {
      case Some(sales) =>
        SalesDetails(
          vatRate         = vatRate,
          netValueOfSales = sales.netValueOfSales,
          vatOnSales      = sales.vatOnSales
        ).validNec

      case None =>
        DataMissingError(SalesAtVatRateFromEuPage(countryFromIndex, countryToIndex, vatRateIndex)).invalidNec
    }
}