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

package models

import base.SpecBase
import org.scalatest.EitherValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsSuccess, Json}

class TotalVatToCountrySpec extends SpecBase
  with ScalaCheckPropertyChecks
  with EitherValues{

  "TotalVatToCountry" - {
    "must serialise and deserialise correctly" in {

      val country: Country = Country("AT", "Austria")
      val totalVat: BigDecimal = BigDecimal(100.10)

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "AT",
          "name" -> "Austria"
        ),
        "totalVat" -> BigDecimal(100.10)
      )

      val expectedResult = TotalVatToCountry(country, totalVat)

      Json.toJson(expectedResult) mustBe json
      json.validate[TotalVatToCountry] mustBe JsSuccess(expectedResult)
    }
  }
}
