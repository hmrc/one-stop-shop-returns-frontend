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

package models.iv

import base.SpecBase
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, JsNull, JsSuccess, Json}

class IdentityVerificationResponseSpec extends SpecBase with Matchers {

  "IdentityVerificationResponseSpec" - {

    "IdentityVerificationResult.fromString should return correct result for valid strings" in {
      IdentityVerificationResult.fromString("Success") mustBe Some(IdentityVerificationResult.Success)
      IdentityVerificationResult.fromString("Incomplete") mustBe Some(IdentityVerificationResult.Incomplete)
      IdentityVerificationResult.fromString("FailedMatching") mustBe Some(IdentityVerificationResult.FailedMatching)
      IdentityVerificationResult.fromString("FailedIV") mustBe Some(IdentityVerificationResult.FailedIdentityVerification)
      IdentityVerificationResult.fromString("InsufficientEvidence") mustBe Some(IdentityVerificationResult.InsufficientEvidence)
      IdentityVerificationResult.fromString("LockedOut") mustBe Some(IdentityVerificationResult.LockedOut)
      IdentityVerificationResult.fromString("UserAborted") mustBe Some(IdentityVerificationResult.UserAborted)
      IdentityVerificationResult.fromString("Timeout") mustBe Some(IdentityVerificationResult.Timeout)
      IdentityVerificationResult.fromString("TechnicalIssue") mustBe Some(IdentityVerificationResult.TechnicalIssue)
      IdentityVerificationResult.fromString("PreconditionFailed") mustBe Some(IdentityVerificationResult.PrecondFailed)
    }

    "IdentityVerificationResult.fromString should return None for invalid strings" in {
      IdentityVerificationResult.fromString("Invalid") mustBe None
      IdentityVerificationResult.fromString("") mustBe None
      IdentityVerificationResult.fromString("  ") mustBe None
    }

    "IdentityVerificationProgress should serialize and deserialize correctly using JSON format" in {
      val progress = IdentityVerificationProgress("InProgress")

      val json = Json.toJson(progress)
      json mustBe Json.parse("""{"result":"InProgress"}""")

      val deserialized = json.as[IdentityVerificationProgress]
      deserialized mustBe progress

      val roundTrip = Json.parse(json.toString()).as[IdentityVerificationProgress]
      roundTrip mustBe progress
    }

    "IdentityVerificationUnexpectedResponse should handle status codes" in {
      val unexpectedResponse = IdentityVerificationUnexpectedResponse(500)

      unexpectedResponse.status mustBe 500
    }
  }

  ".IdentityVerificationProgress" - {

    "must serialise/ to JSON correctly" in {

      val identityVerificationProgress = IdentityVerificationProgress("InProgress")

      val expectedJson = Json.obj(
        "result" -> "InProgress"
      )

      Json.toJson(identityVerificationProgress) mustBe expectedJson

    }

    "must deserialize from JSON correctly" in {

      val json = Json.obj(
        "result" -> "InProgress"
      )

      val expectedIdentityVerificationProgress = IdentityVerificationProgress("InProgress")

      json.validate[IdentityVerificationProgress] mustBe JsSuccess(expectedIdentityVerificationProgress)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[IdentityVerificationProgress] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "result" -> 12345
      )

      json.validate[IdentityVerificationProgress] mustBe a[JsError]
    }

    "must handle null data during deserialization" in {

      val json = Json.obj(
        "result" -> JsNull
      )

      json.validate[IdentityVerificationProgress] mustBe a[JsError]
    }
  }
}
