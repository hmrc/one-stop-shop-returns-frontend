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

class EtmpVatReturnCorrectionSpec extends SpecBase {

  private val etmpVatReturnCorrection: EtmpVatReturnCorrection = arbitraryEtmpVatReturnCorrection.arbitrary.sample.value

  "EtmpVatReturnCorrection" - {

    "must serialise and deserialise from and to an EtmpVatReturnCorrection" in {

      val json = Json.obj(
        "periodKey" -> etmpVatReturnCorrection.periodKey,
        "periodFrom" -> etmpVatReturnCorrection.periodFrom,
        "periodTo" -> etmpVatReturnCorrection.periodTo,
        "msOfConsumption" -> etmpVatReturnCorrection.msOfConsumption,
        "totalVATAmountCorrectionGBP" -> etmpVatReturnCorrection.totalVATAmountCorrectionGBP
      )

      val expectedResult = EtmpVatReturnCorrection(
        periodKey = etmpVatReturnCorrection.periodKey,
        periodFrom = etmpVatReturnCorrection.periodFrom,
        periodTo = etmpVatReturnCorrection.periodTo,
        msOfConsumption = etmpVatReturnCorrection.msOfConsumption,
        totalVATAmountCorrectionGBP = etmpVatReturnCorrection.totalVATAmountCorrectionGBP
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpVatReturnCorrection] mustBe JsSuccess(expectedResult)
    }
  }
}
