/*
 * Copyright 2022 HM Revenue & Customs
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

package models.registration

import models.domain.EuTaxIdentifierType
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsString, JsSuccess, Json}

class EuTaxIdentifierTypeSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks {

  "must serialise and deserialise to and from all valid values" in {

    EuTaxIdentifierType.values.foreach {
      value =>
        val json = Json.toJson(value)
        json.validate[EuTaxIdentifierType] mustEqual JsSuccess(value)
    }
  }

  "must not deserialise from any invalid values" in {

    forAll(arbitrary[String]) {
      value =>

        whenever (!EuTaxIdentifierType.values.map(_.toString).contains(value)) {
          JsString(value).validate[EuTaxIdentifierType] mustEqual JsError("error.invalid")
        }
    }
  }
}
