/*
 * Copyright 2023 HM Revenue & Customs
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

import generators.Generators
import models.Country
import models.domain.EuTaxIdentifier
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsSuccess, Json}

class EuTaxRegistrationSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with Generators {

  "EU Tax Registration" - {

    "must serialise and deserialise from / to an EU VAT Registration" in {

      val euVatNumberGen = arbitrary[Int].map(_.toString)

      forAll(arbitrary[Country], euVatNumberGen) {
        case (country, vatNumber) =>

          val euVatRegistration = EuVatRegistration(country, vatNumber)

          val json = Json.toJson(euVatRegistration)
          json.validate[EuTaxRegistration] mustEqual JsSuccess(euVatRegistration)
      }
    }

    "must serialise and deserialise from / to a Registration with Fixed Establishment" in {

      forAll(arbitrary[Country], arbitrary[FixedEstablishment], arbitrary[EuTaxIdentifier]) {
        case (country, fixedEstablishment, taxRef) =>

          val euRegistration = RegistrationWithFixedEstablishment(country, taxRef, fixedEstablishment)

          val json = Json.toJson(euRegistration)
          json.validate[EuTaxRegistration] mustEqual JsSuccess(euRegistration)
      }
    }

    "must serialise and deserialise from / to a Registration without Fixed Establishment" in {

      forAll(arbitrary[Country]) {
        country =>
          val euRegistration = RegistrationWithoutFixedEstablishment(country)

          val json = Json.toJson(euRegistration)
          json.validate[EuTaxRegistration] mustEqual JsSuccess(euRegistration)
      }
    }
  }
}
