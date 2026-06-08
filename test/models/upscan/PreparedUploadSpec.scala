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

class PreparedUploadSpec extends SpecBase {

  "PreparedUpload" - {

    "must serialise/deserialise to and from PreparedUpload" in {

      val json = Json.obj(
        "reference" -> "reference-1234",
        "uploadRequest" -> Json.obj(
          "href" -> "http://example.com/upload",
          "fields" -> Json.obj(
            "key" -> "value",
            "policy" -> "test-policy"
          )
        )
      )

      val expectedResult = PreparedUpload(
        reference = Reference("reference-1234"),
        uploadRequest = UploadForm(
          href = "http://example.com/upload",
          fields = Map(
            "key" -> "value",
            "policy" -> "test-policy"
          )
        )
      )

      json.validate[PreparedUpload] mustBe JsSuccess(expectedResult)
    }

    "must fail to deserialise when reference is missing" in {

      val json = Json.obj(
        "uploadRequest" -> Json.obj(
          "href" -> "http://example.com/upload",
          "fields" -> Json.obj(
            "key" -> "value",
            "policy" -> "test-policy"
          )
        )
      )

      json.validate[PreparedUpload] mustBe a[JsError]

    }

    "must fail to deserialise when uploadRequest is missing" in {

      val json = Json.obj(
        "reference" -> "reference-1234"
      )

      json.validate[PreparedUpload] mustBe a[JsError]
    }

    "must fail to deserialise when uploadRequest is invalid" in {

      val json = Json.obj(
        "reference" -> "reference-1234",
        "uploadRequest" -> Json.obj(
          "href" -> "http://example.com/upload"
        )
      )

      json.validate[PreparedUpload] mustBe a[JsError]
    }

    "must fail to deserialise when reference is an object" in {

      val json = Json.obj(
        "reference" -> Json.obj("reference" -> "reference-1234"),
        "uploadRequest" -> Json.obj(
          "href" -> "http://example.com/upload",
          "fields" -> Json.obj(
            "key" -> "value",
            "policy" -> "test-policy"
          )
        )
      )

      json.validate[PreparedUpload] mustBe a[JsError]
    }
  }
}
