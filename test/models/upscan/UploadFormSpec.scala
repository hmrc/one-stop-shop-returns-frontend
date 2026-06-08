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
import play.api.libs.json.{JsError, JsSuccess, Json}

class UploadFormSpec extends SpecBase {

  "UploadForm" - {

    "must serialise/deserialise to and from UploadForm" in {

      val json = Json.obj(
        "href" -> "http://example.com/upload",
        "fields" -> Json.obj(
          "key" -> "value",
          "policy" -> "test-policy"
        )
      )

      val expectedResult = UploadForm(
        href = "http://example.com/upload",
        fields = Map(
          "key" -> "value",
          "policy" -> "test-policy"
        )
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[UploadForm] mustBe JsSuccess(expectedResult)
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "fields" -> Json.obj(
          "key" -> "value",
          "policy" -> "test-policy"
        )
      )

      json.validate[UploadForm] mustBe a[JsError]
    }

    "must handle missing fields during deserialization" in {
      val json = Json.obj(
        "href" -> "http://example.com/upload"
      )

      json.validate[UploadForm] mustBe a[JsError]
    }

    "must handle invalid href during deserialization" in {
      val json = Json.obj(
        "href" -> 123,
        "fields" -> Json.obj("key" -> "value")
      )

      json.validate[UploadForm] mustBe a[JsError]
    }

    "must handle invalid fields during deserialization" in {
      val json = Json.obj(
        "href" -> "http://example.com/upload",
        "fields" -> "not-an-object"
      )

      json.validate[UploadForm] mustBe a[JsError]
    }

    "must handle non-string field values during deserialization" in {
      val json = Json.obj(
        "href" -> "http://example.com/upload",
        "fields" -> Json.obj(
          "key" -> 123
        )
      )

      json.validate[UploadForm] mustBe a[JsError]
    }

    "must serialise/deserialise with empty fields" in {
      val json = Json.obj(
        "href" -> "http://example.com/upload",
        "fields" -> Json.obj()
      )

      val expectedResult = UploadForm(
        href = "http://example.com/upload",
        fields = Map.empty
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[UploadForm] mustBe JsSuccess(expectedResult)
    }
  }
}
