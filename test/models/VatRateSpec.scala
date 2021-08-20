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

import models.VatRateType.Standard
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}

import java.time.LocalDate

class VatRateSpec extends AnyFreeSpec with Matchers {

  "VAT Rate" - {

    "must deserialise when the rate is a JsNumber" in {

      val json = Json.obj(
        "rate" -> 1.0,
        "rateType" -> Standard.toString,
        "validFrom" -> "2021-07-01"
      )

      val expectedVatRate = VatRate(BigDecimal(1.0), Standard, LocalDate.of(2021, 7, 1))
      json.validate[VatRate] mustEqual JsSuccess(expectedVatRate)
    }

    "must deserialise when the rate is a JsString" in {

      val json = Json.obj(
        "rate" -> "1.0",
        "rateType" -> Standard.toString,
        "validFrom" -> "2021-07-01"
      )

      val expectedVatRate = VatRate(BigDecimal(1.0), Standard, LocalDate.of(2021, 7, 1))
      json.validate[VatRate] mustEqual JsSuccess(expectedVatRate)
    }

    "must serialise with the rate as a string" in {

      val vatRate = VatRate(BigDecimal(1.0), Standard, LocalDate.of(2021, 7, 1))

      val expectedJson = Json.obj(
        "rate" -> "1.0",
        "rateType" -> Standard.toString,
        "validFrom" -> "2021-07-01"
      )

      Json.toJson(vatRate) mustEqual expectedJson
    }

    "must serialise and deserialise when validUntil is present" in {

      val vatRate =  VatRate(BigDecimal(1.0), Standard, LocalDate.of(2021, 7, 1), Some(LocalDate.of(2022, 1, 1)))

      Json.toJson(vatRate).validate[VatRate] mustEqual JsSuccess(vatRate)
    }
  }
}
