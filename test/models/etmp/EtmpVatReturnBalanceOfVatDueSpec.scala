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

class EtmpVatReturnBalanceOfVatDueSpec extends SpecBase {

  private val etmpVatReturnBalanceOfVatDue: EtmpVatReturnBalanceOfVatDue = arbitraryEtmpVatReturnBalanceOfVatDue.arbitrary.sample.value

  "EtmpVatReturnBalanceOfVatDue" - {

    "must serialise and deserialise from and to an EtmpVatReturnBalanceOfVatDue" in {

      val json = Json.obj(
        "msOfConsumption" -> etmpVatReturnBalanceOfVatDue.msOfConsumption,
        "totalVATDueGBP" -> etmpVatReturnBalanceOfVatDue.totalVATDueGBP
      )

      val expectedResult = EtmpVatReturnBalanceOfVatDue(
        msOfConsumption = etmpVatReturnBalanceOfVatDue.msOfConsumption,
        totalVATDueGBP = etmpVatReturnBalanceOfVatDue.totalVATDueGBP
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpVatReturnBalanceOfVatDue] mustBe JsSuccess(expectedResult)
    }
  }
}
