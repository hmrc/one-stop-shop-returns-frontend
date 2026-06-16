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

class UpscanInitiateRequestSpec extends SpecBase {

  "UpscanInitiateRequest" - {

    "must serialise/deserialise to and from UpscanInitiateRequest" in {

      val json = Json.obj(
        "callbackUrl" -> "http://localhost/callback",
        "successRedirect" -> "http://localhost/success",
        "errorRedirect" -> "http://localhost/error",
        "minimumFileSize" -> 1,
        "maximumFileSize" -> 10485760,
        "expectedContentType" -> "text/csv"
      )

      val expectedResult = UpscanInitiateRequest(
        callbackUrl = "http://localhost/callback",
        successRedirect = Some("http://localhost/success"),
        errorRedirect = Some("http://localhost/error"),
        minimumFileSize = Some(1),
        maximumFileSize = Some(10485760),
        expectedContentType = Some("text/csv")
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[UpscanInitiateRequest] mustBe JsSuccess(expectedResult)
    }

    "must serialise/deserialise with only callbackUrl" in {

      val json = Json.obj(
        "callbackUrl" -> "http://localhost/callback"
      )

      val expectedResult = UpscanInitiateRequest(
        callbackUrl = "http://localhost/callback"
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[UpscanInitiateRequest] mustBe JsSuccess(expectedResult)
    }

    "must fail when callbackUrl is missing" in {

      val json = Json.obj(
        "maximumFileSize" -> 10485760
      )

      json.validate[UpscanInitiateRequest] mustBe a[JsError]
    }
  }

  "UpscanUploadRequest" - {

    "must serialise/deserialise to and from UpscanUploadRequest" in {

      val json = Json.obj(
        "href" -> "http://example.com/upload",
        "fields" -> Json.obj(
          "key" -> "value",
          "policy" -> "test-policy"
        )
      )

      val expectedResult = UpscanUploadRequest(
        href = "http://example.com/upload",
        fields = Map(
          "key" -> "value",
          "policy" -> "test-policy"
        )
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[UpscanUploadRequest] mustBe JsSuccess(expectedResult)
    }

    "must serialise/deserialise with empty fields" in {

      val json = Json.obj(
        "href" -> "http://example.com/upload",
        "fields" -> Json.obj()
      )

      val expectedResult = UpscanUploadRequest(
        href = "http://example.com/upload",
        fields = Map.empty
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[UpscanUploadRequest] mustBe JsSuccess(expectedResult)
    }

    "must fail when href is missing" in {

      val json = Json.obj(
        "fields" -> Json.obj("key" -> "value")
      )

      json.validate[UpscanUploadRequest] mustBe a[JsError]
    }

    "must fail when fields is missing" in {

      val json = Json.obj(
        "href" -> "http://example.com/upload"
      )

      json.validate[UpscanUploadRequest] mustBe a[JsError]
    }
  }

  "UpscanFileReference" - {

    "must serialise/deserialise to and from UpscanFileReference" in {

      val json = Json.obj(
        "reference" -> "reference-1234"
      )

      val expectedResult = UpscanFileReference(reference = "reference-1234")

      Json.toJson(expectedResult) mustBe json
      json.validate[UpscanFileReference] mustBe JsSuccess(expectedResult)
    }

    "must fail when reference is missing" in {
      val json = Json.obj()

      json.validate[UpscanFileReference] mustBe a[JsError]
    }
  }

  "UpscanInitiateResponse" - {

    "must serialise/deserialise to and from UpscanInitiateResponse" in {

      val json = Json.obj(
        "fileReference" -> Json.obj(
          "reference" -> "reference-1234"
        ),
        "postTarget" -> "http://example.com/upload",
        "formFields" -> Json.obj(
          "key" -> "value",
          "policy" -> "test-policy"
        )
      )

      val expectedResult = UpscanInitiateResponse(
        fileReference = UpscanFileReference("reference-1234"),
        postTarget = "http://example.com/upload",
        formFields = Map(
          "key" -> "value",
          "policy" -> "test-policy"
        )
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[UpscanInitiateResponse] mustBe JsSuccess(expectedResult)
    }

    "must fail when fileReference is missing" in {
      val json = Json.obj(
        "postTarget" -> "http://example.com/upload",
        "formFields" -> Json.obj("key" -> "value")
      )

      json.validate[UpscanInitiateResponse] mustBe a[JsError]
    }

    "must fail when postTarget is missing" in {
      val json = Json.obj(
        "fileReference" -> Json.obj("reference" -> "reference-1234"),
        "formFields" -> Json.obj("key" -> "value")
      )

      json.validate[UpscanInitiateResponse] mustBe a[JsError]
    }

    "must fail when formFields is missing" in {
      val json = Json.obj(
        "fileReference" -> Json.obj("reference" -> "reference-1234"),
        "postTarget" -> "http://example.com/upload"
      )

      json.validate[UpscanInitiateResponse] mustBe a[JsError]
    }
  }

  "FileUploadOutcome" - {

    "must serialise/deserialise to and from FileUploadOutcome" in {

      val json = Json.obj(
        "fileName" -> "test.csv",
        "status" -> "READY",
        "failureReason" -> "QUARANTINE"
      )

      val expectedResult = FileUploadOutcome(
        fileName = Some("test.csv"),
        status = "READY",
        failureReason = Some("QUARANTINE")
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[FileUploadOutcome] mustBe JsSuccess(expectedResult)
    }

    "must serialise/deserialise with only required fields" in {

      val json = Json.obj(
        "status" -> "READY"
      )

      val expectedResult = FileUploadOutcome(
        fileName = None,
        status = "READY"
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[FileUploadOutcome] mustBe JsSuccess(expectedResult)
    }

    "must fail when status is missing" in {

      val json = Json.obj(
        "fileName" -> "test.csv"
      )

      json.validate[FileUploadOutcome] mustBe a[JsError]
    }
  }
}
