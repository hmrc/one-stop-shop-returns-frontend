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

package models.etmp

import base.SpecBase
import play.api.libs.json.{JsSuccess, Json}

class EtmpVatReturnSpec extends SpecBase {

  private val etmpVatReturn: EtmpVatReturn = arbitraryEtmpVatReturn.arbitrary.sample.value

  "EtmpVatReturn" - {

    "must serialise and deserialise from and to an EtmpVatReturn" in {

      val json = Json.obj(
        "returnReference" -> etmpVatReturn.returnReference,
        "returnVersion" -> etmpVatReturn.returnVersion,
        "periodKey" -> etmpVatReturn.periodKey,
        "returnPeriodFrom" -> etmpVatReturn.returnPeriodFrom,
        "returnPeriodTo" -> etmpVatReturn.returnPeriodTo,
        "goodsSupplied" -> etmpVatReturn.goodsSupplied,
        "totalVATGoodsSuppliedGBP" -> etmpVatReturn.totalVATGoodsSuppliedGBP,
        "goodsDispatched" -> etmpVatReturn.goodsDispatched,
        "totalVATAmountPayable" -> etmpVatReturn.totalVATAmountPayable,
        "totalVATAmountPayableAllSpplied" -> etmpVatReturn.totalVATAmountPayableAllSpplied,
        "correctionPreviousVATReturn" -> etmpVatReturn.correctionPreviousVATReturn,
        "totalVATAmountFromCorrectionGBP" -> etmpVatReturn.totalVATAmountFromCorrectionGBP,
        "balanceOfVATDueForMS" -> etmpVatReturn.balanceOfVATDueForMS,
        "totalVATAmountDueForAllMSGBP" -> etmpVatReturn.totalVATAmountDueForAllMSGBP,
        "paymentReference" -> etmpVatReturn.paymentReference,
      )

      val expectedResult = EtmpVatReturn(
        returnReference = etmpVatReturn.returnReference,
        returnVersion = etmpVatReturn.returnVersion,
        periodKey = etmpVatReturn.periodKey,
        returnPeriodFrom = etmpVatReturn.returnPeriodFrom,
        returnPeriodTo = etmpVatReturn.returnPeriodTo,
        goodsSupplied = etmpVatReturn.goodsSupplied,
        totalVATGoodsSuppliedGBP = etmpVatReturn.totalVATGoodsSuppliedGBP,
        goodsDispatched = etmpVatReturn.goodsDispatched,
        totalVATAmountPayable = etmpVatReturn.totalVATAmountPayable,
        totalVATAmountPayableAllSpplied = etmpVatReturn.totalVATAmountPayableAllSpplied,
        correctionPreviousVATReturn = etmpVatReturn.correctionPreviousVATReturn,
        totalVATAmountFromCorrectionGBP = etmpVatReturn.totalVATAmountFromCorrectionGBP,
        balanceOfVATDueForMS = etmpVatReturn.balanceOfVATDueForMS,
        totalVATAmountDueForAllMSGBP = etmpVatReturn.totalVATAmountDueForAllMSGBP,
        paymentReference = etmpVatReturn.paymentReference
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpVatReturn] mustBe JsSuccess(expectedResult)
    }
  }
}

