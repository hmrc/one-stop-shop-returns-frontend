/*
 * Copyright 2021 HM Revenue & Customs
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

package models

import generators.Generators
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsString, JsSuccess, Json}
import uk.gov.hmrc.domain.Vrn

class PaymentReferenceSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with Generators {

  ".fromString" - {

    "must read from a string in the format XI<vrn><quarter><two-digit-year>" in {

      val genYear = Gen.choose(10, 99)
      forAll(arbitrary[Vrn], genYear, arbitrary[Quarter]) {
        case (vrn, year, quarter) =>

          val string = s"XI$vrn${quarter.toString}$year"
          PaymentReference.fromString(string) mustBe defined
      }
    }

    "must not read from a string that isn't in the format XI<vrn><quarter><two-digit-year>" in {

      val pattern = """XI\d{9}Q[1-4]\d{2}""".r.anchored

      forAll(arbitrary[String]) {
        string =>

          whenever(pattern.findFirstIn(string).isEmpty) {
            PaymentReference.fromString(string) must not be defined
          }
      }
    }
  }

  "must serialise and deserialise to / from a valid payment reference" in {

    forAll(arbitrary[Period], arbitrary[Vrn]) {
      case (period, vrn) =>

        val paymentReference = PaymentReference(vrn, period)
        val json = Json.toJson(paymentReference)
        json.validate[PaymentReference] mustEqual JsSuccess(paymentReference)
    }
  }

  "must not read from an incorrectly formatted JSON string" in {

    forAll(arbitrary[String]) {
      string =>

        whenever(PaymentReference.fromString(string).isEmpty) {
          JsString(string).validate[PaymentReference] mustBe a[JsError]
        }
    }
  }
}
