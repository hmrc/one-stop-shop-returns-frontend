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

import java.time.LocalDate

class PartialReturnPeriodSpec extends SpecBase
  with ScalaCheckPropertyChecks
  with EitherValues {

  "PartialReturnPeriod" - {
    "must serialise and deserialise correctly" in {

      val firstDay: LocalDate = LocalDate.of(2021,12,24)
      val lastDay: LocalDate = LocalDate.of(2021,12,25)
      val year: Int = 2021
      val quarter: Quarter = Quarter.Q4


      val json = Json.obj(
        "firstDay" -> LocalDate.of(2021,12,24),
        "lastDay" -> LocalDate.of(2021,12,25),
        "year" -> 2021,
        "quarter" -> Quarter.Q4.toString
      )

      val expectedResult = PartialReturnPeriod(firstDay, lastDay, year, quarter)

      Json.toJson(expectedResult) mustBe json
      json.validate[PartialReturnPeriod] mustBe JsSuccess(expectedResult)
    }
  }
}
