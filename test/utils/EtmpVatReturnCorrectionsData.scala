/*
 * Copyright 2024 HM Revenue & Customs
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

import base.SpecBase
import formats.Format.etmpDateTimeFormatter
import models.etmp.*
import org.scalacheck.Arbitrary.arbitrary

import java.time.{LocalDate, LocalDateTime}

object EtmpVatReturnCorrectionsData extends SpecBase {

  val etmpVatReturnCorrectionSingleCountryScenario: EtmpVatReturn = EtmpVatReturn(
    returnReference = arbitrary[String].sample.value,
    returnVersion = arbitrary[LocalDateTime].sample.value,
    periodKey = "23C4",
    returnPeriodFrom = LocalDate.of(2023, 12, 1),
    returnPeriodTo = LocalDate.of(2023, 12, 31),
    goodsSupplied = Seq(
      EtmpVatReturnGoodsSupplied(
        msOfConsumption = "DE",
        msOfEstablishment = "NI",
        vatRateType = EtmpVatRateType.StandardVatRate,
        taxableAmountGBP = BigDecimal(1000),
        vatAmountGBP = BigDecimal(100)
      ),
      EtmpVatReturnGoodsSupplied(
        msOfConsumption = "FR",
        msOfEstablishment = "DE",
        vatRateType = EtmpVatRateType.ReducedVatRate,
        taxableAmountGBP = BigDecimal(10000),
        vatAmountGBP = BigDecimal(1000)
      )
    ),
    totalVATGoodsSuppliedGBP = BigDecimal(1100),
    totalVATAmountPayable = BigDecimal(0),
    totalVATAmountPayableAllSpplied = BigDecimal(1100),
    correctionPreviousVATReturn = Seq(
      EtmpVatReturnCorrection(
        periodKey = "23C3",
        periodFrom = LocalDate.of(2023, 8, 1).format(etmpDateTimeFormatter),
        periodTo = LocalDate.of(2023, 8, 31).format(etmpDateTimeFormatter),
        msOfConsumption = "DE",
        totalVATAmountCorrectionGBP = BigDecimal(-1000),
        totalVATAmountCorrectionEUR = BigDecimal(-1100.41)
      ),
      EtmpVatReturnCorrection(
        periodKey = "23C3",
        periodFrom = LocalDate.of(2023, 9, 1).format(etmpDateTimeFormatter),
        periodTo = LocalDate.of(2023, 9, 30).format(etmpDateTimeFormatter),
        msOfConsumption = "DE",
        totalVATAmountCorrectionGBP = BigDecimal(-1000),
        totalVATAmountCorrectionEUR = BigDecimal(-1100.41)
      ),
      EtmpVatReturnCorrection(
        periodKey = "23C4",
        periodFrom = LocalDate.of(2023, 10, 1).format(etmpDateTimeFormatter),
        periodTo = LocalDate.of(2023, 10, 31).format(etmpDateTimeFormatter),
        msOfConsumption = "DE",
        totalVATAmountCorrectionGBP = BigDecimal(-1000),
        totalVATAmountCorrectionEUR = BigDecimal(-1100.41)
      ),
    ),
    totalVATAmountFromCorrectionGBP = BigDecimal(-3000),
    balanceOfVATDueForMS = Seq(
      EtmpVatReturnBalanceOfVatDue(
        msOfConsumption = "DE",
        totalVATDueGBP = BigDecimal(0),
        totalVATEUR = BigDecimal(0)
      ),
      EtmpVatReturnBalanceOfVatDue(
        msOfConsumption = "FR",
        totalVATDueGBP = BigDecimal(1000),
        totalVATEUR = BigDecimal(1100)
      )
    ),
    totalVATAmountDueForAllMSGBP = BigDecimal(1000),
    paymentReference = arbitrary[String].sample.value
  )

  val etmpVatReturnCorrectionMultipleCountryScenario: EtmpVatReturn = EtmpVatReturn(
    returnReference = arbitrary[String].sample.value,
    returnVersion = arbitrary[LocalDateTime].sample.value,
    periodKey = "23C4",
    returnPeriodFrom = LocalDate.of(2023, 12, 1),
    returnPeriodTo = LocalDate.of(2023, 12, 31),
    goodsSupplied = Seq(
      EtmpVatReturnGoodsSupplied(
        msOfConsumption = "IT",
        msOfEstablishment = "ES",
        vatRateType = EtmpVatRateType.StandardVatRate,
        taxableAmountGBP = BigDecimal(1500),
        vatAmountGBP = BigDecimal(150)
      ),
      EtmpVatReturnGoodsSupplied(
        msOfConsumption = "LT",
        msOfEstablishment = "DE",
        vatRateType = EtmpVatRateType.ReducedVatRate,
        taxableAmountGBP = BigDecimal(9500),
        vatAmountGBP = BigDecimal(950)
      )
    ),
    totalVATGoodsSuppliedGBP = BigDecimal(1100),
    totalVATAmountPayable = BigDecimal(0),
    totalVATAmountPayableAllSpplied = BigDecimal(1100),
    correctionPreviousVATReturn = Seq(
      EtmpVatReturnCorrection(
        periodKey = "23C3",
        periodFrom = LocalDate.of(2023, 8, 1).format(etmpDateTimeFormatter),
        periodTo = LocalDate.of(2023, 8, 31).format(etmpDateTimeFormatter),
        msOfConsumption = "IT",
        totalVATAmountCorrectionGBP = BigDecimal(-1000),
        totalVATAmountCorrectionEUR = BigDecimal(-1100.41)
      ),
      EtmpVatReturnCorrection(
        periodKey = "23C3",
        periodFrom = LocalDate.of(2023, 9, 1).format(etmpDateTimeFormatter),
        periodTo = LocalDate.of(2023, 9, 30).format(etmpDateTimeFormatter),
        msOfConsumption = "IT",
        totalVATAmountCorrectionGBP = BigDecimal(-1000),
        totalVATAmountCorrectionEUR = BigDecimal(-1100.41)
      ),
      EtmpVatReturnCorrection(
        periodKey = "23C4",
        periodFrom = LocalDate.of(2023, 10, 1).format(etmpDateTimeFormatter),
        periodTo = LocalDate.of(2023, 10, 31).format(etmpDateTimeFormatter),
        msOfConsumption = "IT",
        totalVATAmountCorrectionGBP = BigDecimal(-500),
        totalVATAmountCorrectionEUR = BigDecimal(-510.40)
      ),
      EtmpVatReturnCorrection(
        periodKey = "23C3",
        periodFrom = LocalDate.of(2023, 9, 1).format(etmpDateTimeFormatter),
        periodTo = LocalDate.of(2023, 9, 30).format(etmpDateTimeFormatter),
        msOfConsumption = "LT",
        totalVATAmountCorrectionGBP = BigDecimal(-1000),
        totalVATAmountCorrectionEUR = BigDecimal(-1100.41)
      ),
      EtmpVatReturnCorrection(
        periodKey = "23C4",
        periodFrom = LocalDate.of(2023, 10, 1).format(etmpDateTimeFormatter),
        periodTo = LocalDate.of(2023, 10, 31).format(etmpDateTimeFormatter),
        msOfConsumption = "LT",
        totalVATAmountCorrectionGBP = BigDecimal(-1000),
        totalVATAmountCorrectionEUR = BigDecimal(-1100.41)
      ),
    ),
    totalVATAmountFromCorrectionGBP = BigDecimal(-4500),
    balanceOfVATDueForMS = Seq(
      EtmpVatReturnBalanceOfVatDue(
        msOfConsumption = "IT",
        totalVATDueGBP = BigDecimal(0),
        totalVATEUR = BigDecimal(0)
      ),
      EtmpVatReturnBalanceOfVatDue(
        msOfConsumption = "LT",
        totalVATDueGBP = BigDecimal(0),
        totalVATEUR = BigDecimal(0)
      )
    ),
    totalVATAmountDueForAllMSGBP = BigDecimal(0),
    paymentReference = arbitrary[String].sample.value
  )

  val etmpVatReturnCorrectionMultipleCountryNilScenario: EtmpVatReturn = EtmpVatReturn(
    returnReference = arbitrary[String].sample.value,
    returnVersion = arbitrary[LocalDateTime].sample.value,
    periodKey = "23C4",
    returnPeriodFrom = LocalDate.of(2023, 12, 1),
    returnPeriodTo = LocalDate.of(2023, 12, 31),
    goodsSupplied = Seq(
      EtmpVatReturnGoodsSupplied(
        msOfConsumption = "EE",
        msOfEstablishment = "DE",
        vatRateType = EtmpVatRateType.StandardVatRate,
        taxableAmountGBP = BigDecimal(2500),
        vatAmountGBP = BigDecimal(250)
      ),
      EtmpVatReturnGoodsSupplied(
        msOfConsumption = "PL",
        msOfEstablishment = "LT",
        vatRateType = EtmpVatRateType.ReducedVatRate,
        taxableAmountGBP = BigDecimal(23000),
        vatAmountGBP = BigDecimal(2300)
      ),
      EtmpVatReturnGoodsSupplied(
        msOfConsumption = "LV",
        msOfEstablishment = "ES",
        vatRateType = EtmpVatRateType.ReducedVatRate,
        taxableAmountGBP = BigDecimal(7000),
        vatAmountGBP = BigDecimal(700)
      )
    ),
    totalVATGoodsSuppliedGBP = BigDecimal(3250),
    totalVATAmountPayable = BigDecimal(0),
    totalVATAmountPayableAllSpplied = BigDecimal(3250),
    correctionPreviousVATReturn = Seq(
      EtmpVatReturnCorrection(
        periodKey = "23C3",
        periodFrom = LocalDate.of(2023, 8, 1).format(etmpDateTimeFormatter),
        periodTo = LocalDate.of(2023, 8, 31).format(etmpDateTimeFormatter),
        msOfConsumption = "EE",
        totalVATAmountCorrectionGBP = BigDecimal(-250),
        totalVATAmountCorrectionEUR = BigDecimal(-275.10)
      ),
      EtmpVatReturnCorrection(
        periodKey = "23C3",
        periodFrom = LocalDate.of(2023, 9, 1).format(etmpDateTimeFormatter),
        periodTo = LocalDate.of(2023, 9, 30).format(etmpDateTimeFormatter),
        msOfConsumption = "PL",
        totalVATAmountCorrectionGBP = BigDecimal(-1000),
        totalVATAmountCorrectionEUR = BigDecimal(-1100.41)
      ),
      EtmpVatReturnCorrection(
        periodKey = "23C4",
        periodFrom = LocalDate.of(2023, 10, 1).format(etmpDateTimeFormatter),
        periodTo = LocalDate.of(2023, 10, 31).format(etmpDateTimeFormatter),
        msOfConsumption = "PL",
        totalVATAmountCorrectionGBP = BigDecimal(-500),
        totalVATAmountCorrectionEUR = BigDecimal(-510.40)
      ),
      EtmpVatReturnCorrection(
        periodKey = "23C3",
        periodFrom = LocalDate.of(2023, 9, 1).format(etmpDateTimeFormatter),
        periodTo = LocalDate.of(2023, 9, 30).format(etmpDateTimeFormatter),
        msOfConsumption = "PL",
        totalVATAmountCorrectionGBP = BigDecimal(-800),
        totalVATAmountCorrectionEUR = BigDecimal(-880.33)
      ),
    ),
    totalVATAmountFromCorrectionGBP = BigDecimal(-2550),
    balanceOfVATDueForMS = Seq(
      EtmpVatReturnBalanceOfVatDue(
        msOfConsumption = "EE",
        totalVATDueGBP = BigDecimal(0),
        totalVATEUR = BigDecimal(0)
      ),
      EtmpVatReturnBalanceOfVatDue(
        msOfConsumption = "PL",
        totalVATDueGBP = BigDecimal(0),
        totalVATEUR = BigDecimal(0)
      ),
      EtmpVatReturnBalanceOfVatDue(
        msOfConsumption = "LV",
        totalVATDueGBP = BigDecimal(700),
        totalVATEUR = BigDecimal(770.29)
      ),
    ),
    totalVATAmountDueForAllMSGBP = BigDecimal(700),
    paymentReference = arbitrary[String].sample.value
  )

  val etmpVatReturnCorrectionMultipleCountryMixPosAndNegCorrectionsScenario: EtmpVatReturn = EtmpVatReturn(
    returnReference = arbitrary[String].sample.value,
    returnVersion = arbitrary[LocalDateTime].sample.value,
    periodKey = "23C4",
    returnPeriodFrom = LocalDate.of(2023, 12, 1),
    returnPeriodTo = LocalDate.of(2023, 12, 31),
    goodsSupplied = Seq(
      EtmpVatReturnGoodsSupplied(
        msOfConsumption = "AT",
        msOfEstablishment = "DE",
        vatRateType = EtmpVatRateType.StandardVatRate,
        taxableAmountGBP = BigDecimal(1500),
        vatAmountGBP = BigDecimal(150)
      ),
      EtmpVatReturnGoodsSupplied(
        msOfConsumption = "HR",
        msOfEstablishment = "ES",
        vatRateType = EtmpVatRateType.ReducedVatRate,
        taxableAmountGBP = BigDecimal(10000),
        vatAmountGBP = BigDecimal(1000)
      )
    ),
    totalVATGoodsSuppliedGBP = BigDecimal(1150),
    totalVATAmountPayable = BigDecimal(0),
    totalVATAmountPayableAllSpplied = BigDecimal(1150),
    correctionPreviousVATReturn = Seq(
      EtmpVatReturnCorrection(
        periodKey = "23C3",
        periodFrom = LocalDate.of(2023, 8, 1).format(etmpDateTimeFormatter),
        periodTo = LocalDate.of(2023, 8, 31).format(etmpDateTimeFormatter),
        msOfConsumption = "AT",
        totalVATAmountCorrectionGBP = BigDecimal(-250),
        totalVATAmountCorrectionEUR = BigDecimal(-275.10)
      ),
      EtmpVatReturnCorrection(
        periodKey = "23C3",
        periodFrom = LocalDate.of(2023, 9, 1).format(etmpDateTimeFormatter),
        periodTo = LocalDate.of(2023, 9, 30).format(etmpDateTimeFormatter),
        msOfConsumption = "AT",
        totalVATAmountCorrectionGBP = BigDecimal(500),
        totalVATAmountCorrectionEUR = BigDecimal(550.21)
      ),
      EtmpVatReturnCorrection(
        periodKey = "23C4",
        periodFrom = LocalDate.of(2023, 10, 1).format(etmpDateTimeFormatter),
        periodTo = LocalDate.of(2023, 10, 31).format(etmpDateTimeFormatter),
        msOfConsumption = "AT",
        totalVATAmountCorrectionGBP = BigDecimal(-750),
        totalVATAmountCorrectionEUR = BigDecimal(-825.31)
      ),
      EtmpVatReturnCorrection(
        periodKey = "23C3",
        periodFrom = LocalDate.of(2023, 9, 1).format(etmpDateTimeFormatter),
        periodTo = LocalDate.of(2023, 9, 30).format(etmpDateTimeFormatter),
        msOfConsumption = "HR",
        totalVATAmountCorrectionGBP = BigDecimal(-800),
        totalVATAmountCorrectionEUR = BigDecimal(-880.33)
      ),
      EtmpVatReturnCorrection(
        periodKey = "23C4",
        periodFrom = LocalDate.of(2023, 10, 1).format(etmpDateTimeFormatter),
        periodTo = LocalDate.of(2023, 10, 31).format(etmpDateTimeFormatter),
        msOfConsumption = "HR",
        totalVATAmountCorrectionGBP = BigDecimal(-500),
        totalVATAmountCorrectionEUR = BigDecimal(-550.21)
      ),
      EtmpVatReturnCorrection(
        periodKey = "23C4",
        periodFrom = LocalDate.of(2023, 11, 1).format(etmpDateTimeFormatter),
        periodTo = LocalDate.of(2023, 11, 30).format(etmpDateTimeFormatter),
        msOfConsumption = "HR",
        totalVATAmountCorrectionGBP = BigDecimal(250),
        totalVATAmountCorrectionEUR = BigDecimal(275.10)
      ),
    ),
    totalVATAmountFromCorrectionGBP = BigDecimal(-1550),
    balanceOfVATDueForMS = Seq(
      EtmpVatReturnBalanceOfVatDue(
        msOfConsumption = "AT",
        totalVATDueGBP = BigDecimal(0),
        totalVATEUR = BigDecimal(0)
      ),
      EtmpVatReturnBalanceOfVatDue(
        msOfConsumption = "HR",
        totalVATDueGBP = BigDecimal(0),
        totalVATEUR = BigDecimal(0)
      )
    ),
    totalVATAmountDueForAllMSGBP = BigDecimal(0),
    paymentReference = arbitrary[String].sample.value
  )
}
