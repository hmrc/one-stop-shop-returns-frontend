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
import play.api.libs.json.{JsString, JsSuccess, Json}
import models.iv.IdentityVerificationEvidenceSource.*

class IdentityVerificationEvidenceSourceSpec extends SpecBase with Matchers{

  "IdentityVerificationEvidenceSource" - {

    "serialize and deserialize PayslipService correctly" in {
      val json = Json.toJson(PayslipService.asString)
      json mustBe JsString("PayslipService")

      val deserialized = json.validate[IdentityVerificationEvidenceSource]
      deserialized mustBe JsSuccess(PayslipService)
    }

    "serialize and deserialize P60Service correctly" in {
      val json = Json.toJson(P60Service.asString)
      json mustBe JsString("P60Service")

      val deserialized = json.validate[IdentityVerificationEvidenceSource]
      deserialized mustBe JsSuccess(P60Service)
    }

    "serialize and deserialize NtcService correctly" in {
      val json = Json.toJson(NtcService.asString)
      json mustBe JsString("NtcService")

      val deserialized = json.validate[IdentityVerificationEvidenceSource]
      deserialized mustBe JsSuccess(NtcService)
    }

    "serialize and deserialize Passport correctly" in {
      val json = Json.toJson(Passport.asString)
      json mustBe JsString("passport")

      val deserialized = json.validate[IdentityVerificationEvidenceSource]
      deserialized mustBe JsSuccess(Passport)
    }

    "serialize and deserialize CallValidate correctly" in {
      val json = Json.toJson(CallValidate.asString)
      json mustBe JsString("call-validate")

      val deserialized = json.validate[IdentityVerificationEvidenceSource]
      deserialized mustBe JsSuccess(CallValidate)
    }

    "serialize and deserialize an UnrecognisedSource correctly" in {
      val unrecognized: IdentityVerificationEvidenceSource = UnrecognisedSource("custom-source")

      val json = Json.toJson(unrecognized)
      json mustBe JsString("custom-source")

      val deserialized = json.validate[IdentityVerificationEvidenceSource]
      deserialized mustBe JsSuccess(UnrecognisedSource("\"custom-source\""))
    }

    "deserialize unknown source strings as UnrecognisedSource" in {
      val json = JsString("unknown-source")

      // Deserialize into UnrecognisedSource
      val deserialized = json.validate[IdentityVerificationEvidenceSource]
      deserialized mustBe JsSuccess(UnrecognisedSource("\"unknown-source\""))
    }

    "handle invalid JSON gracefully" in {
      val invalidJson = Json.obj("invalid" -> "data")
      val deserialized = invalidJson.validate[IdentityVerificationEvidenceSource]
      deserialized.isSuccess mustBe true // Because all JsValue inputs are treated as UnrecognisedSource
      deserialized.get mustBe UnrecognisedSource("""{"invalid":"data"}""")
    }
  }

}
