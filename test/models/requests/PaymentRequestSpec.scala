/*
 * Copyright 2024 HM Revenue & Customs
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

package models.requests

import base.SpecBase
import models.Quarter.*
import models.StandardPeriod
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.domain.Vrn

import java.time.LocalDate

class PaymentRequestSpec extends SpecBase {

  private val year = 2021

  "PaymentPeriod" - {
    "should return correct PaymentPeriod object" - {
      "for Q1" in {
        val period = StandardPeriod(year, Q1)
        val expected = PaymentPeriod(year, "JanuaryToMarch")

        PaymentPeriod(period) mustBe expected
      }

      "for Q2" in {
        val period = StandardPeriod(year, Q2)
        val expected = PaymentPeriod(year, "AprilToJune")

        PaymentPeriod(period) mustBe expected
      }

      "for Q3" in {
        val period = StandardPeriod(year, Q3)
        val expected = PaymentPeriod(year, "JulyToSeptember")

        PaymentPeriod(period) mustBe expected
      }

      "for Q4" in {
        val period = StandardPeriod(year, Q4)
        val expected = PaymentPeriod(year, "OctoberToDecember")

        PaymentPeriod(period) mustBe expected
      }
    }
  }

  "PaymentRequest" - {
    "must serialise and deserialise correctly" in {

      val vrn: Vrn = Vrn("vrn")
      val period: PaymentPeriod = PaymentPeriod(2024,"Q4")
      val amountInPence: Int = 102332
      val dueDate: Option[LocalDate] = Some(LocalDate.of(2024,12,9))

      val json = Json.obj(
        "vrn" -> Vrn("vrn"),
        "period" -> PaymentPeriod(2024,"Q4"),
        "amountInPence" -> 102332,
        "dueDate" -> Some(LocalDate.of(2024,12,9))
      )

      val expectedResult = PaymentRequest(vrn, period, amountInPence, dueDate)

      Json.toJson(expectedResult) mustBe json
      json.validate[PaymentRequest] mustBe JsSuccess(expectedResult)
    }
  }
}
