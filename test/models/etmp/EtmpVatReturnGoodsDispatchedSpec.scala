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

package models.etmp

import base.SpecBase
import play.api.libs.json.{JsSuccess, Json}

class EtmpVatReturnGoodsDispatchedSpec extends SpecBase {

  private val etmpVatReturnGoodsDispatched: EtmpVatReturnGoodsDispatched = arbitraryEtmpVatReturnGoodsDispatched.arbitrary.sample.value

  "EtmpVatReturnGoodsDispatched" - {

    "must serialise and deserialise from and to an EtmpVatReturnGoodsDispatched" in {

      val json = Json.obj(
        "msOfEstablishment" -> etmpVatReturnGoodsDispatched.msOfEstablishment,
        "msOfConsumption" -> etmpVatReturnGoodsDispatched.msOfConsumption,
        "vatRateType" -> etmpVatReturnGoodsDispatched.vatRateType,
        "taxableAmountGBP" -> etmpVatReturnGoodsDispatched.taxableAmountGBP,
        "vatAmountGBP" -> etmpVatReturnGoodsDispatched.vatAmountGBP
      )

      val expectedResult = EtmpVatReturnGoodsDispatched(
        msOfEstablishment = etmpVatReturnGoodsDispatched.msOfEstablishment,
        msOfConsumption = etmpVatReturnGoodsDispatched.msOfConsumption,
        vatRateType = etmpVatReturnGoodsDispatched.vatRateType,
        taxableAmountGBP = etmpVatReturnGoodsDispatched.taxableAmountGBP,
        vatAmountGBP = etmpVatReturnGoodsDispatched.vatAmountGBP
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpVatReturnGoodsDispatched] mustBe JsSuccess(expectedResult)
    }
  }
}
