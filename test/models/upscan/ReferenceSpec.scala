/*
 * Copyright 2026 HM Revenue & Customs
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

package models.upscan

import base.SpecBase
import play.api.libs.json.{JsError, JsNull, JsNumber, JsString, JsSuccess, Json}

class ReferenceSpec extends SpecBase {

  "Reference" - {

    "must serialise/deserialise to and from Reference" in {

      val json = JsString("reference-1234")

      val expectedResult = Reference(reference = "reference-1234")

      Json.toJson(expectedResult) mustBe json
      json.validate[Reference] mustBe JsSuccess(expectedResult)
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj()

      json.validate[Reference] mustBe a[JsError]
    }

    "must fail to deserialise from a JSON object with reference field" in {
      val json = Json.obj("reference" -> "reference-1234")

      json.validate[Reference] mustBe a[JsError]
    }

    "must fail to deserialise from a number" in {
      val json = JsNumber(1234)

      json.validate[Reference] mustBe a[JsError]
    }

    "must fail to deserialise from null" in {
      val json = JsNull

      json.validate[Reference] mustBe a[JsError]
    }

    "must serialise an empty reference" in {
      val expectedResult = Reference("")

      Json.toJson(expectedResult) mustBe JsString("")
    }
  }

}
