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

import models.Country.getCountryName
import models.Period.fromEtmpPeriodKey
import models.etmp.EtmpVatReturnCorrection
import play.api.i18n.Messages
import viewmodels.govuk.all.currencyFormat

object PreviousReturnCorrectionsSummary {

  case class CorrectionRow(
                            period: String,
                            country: String,
                            totalVATAmountCorrectionGBP: String,
                            isFirst: Boolean,
                            isLastCountry: Boolean,
                            isLastPeriod: Boolean
                          )

  def correctionRows(corrections: Seq[EtmpVatReturnCorrection])(implicit messages: Messages): Seq[CorrectionRow] = {
    val correctionsGroupedByPeriod = corrections.groupBy(_.periodKey).toSeq.sortBy(_._1)

    correctionsGroupedByPeriod.zipWithIndex.flatMap {
      case ((periodKey, correctionPeriodVatReturns), periodIndex) =>
        correctionPeriodVatReturns.zipWithIndex.map {
          case (correctionPeriodVatReturn, index) if index == 0 =>
            val isFirst = true
            val isLastCountry = correctionPeriodVatReturns.size == index + 1
            val isLastPeriod = correctionsGroupedByPeriod.size == periodIndex + 1

            getCorrectionRow(periodKey, correctionPeriodVatReturn, isFirst, isLastCountry, isLastPeriod)
          case (correctionPeriodVatReturn, index) =>
            val isFirst = false
            val isLastCountry = correctionPeriodVatReturns.size == index + 1

            val isLastPeriod = correctionsGroupedByPeriod.size == periodIndex + 1

            getCorrectionRow(periodKey, correctionPeriodVatReturn, isFirst, isLastCountry, isLastPeriod)
        }
    }
  }

  private def getCorrectionRow(
                                periodKey: String,
                                correctionPeriodVatReturn: EtmpVatReturnCorrection,
                                isFirst: Boolean,
                                isLastCountry: Boolean,
                                isLastPeriod: Boolean
                              )(implicit messages: Messages): CorrectionRow = {
    val periodString = fromEtmpPeriodKey(periodKey).displayText
    val country = getCountryName(correctionPeriodVatReturn.msOfConsumption)

    CorrectionRow(
      periodString,
      country,
      currencyFormat(correctionPeriodVatReturn.totalVATAmountCorrectionGBP),
      isFirst,
      isLastCountry,
      isLastPeriod
    )
  }
}

