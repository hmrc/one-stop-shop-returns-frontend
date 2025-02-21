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

package models.requests.corrections

import base.SpecBase
import models.Quarter.Q4
import models.StandardPeriod
import models.corrections.PeriodWithCorrections
import org.scalatest.EitherValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.domain.Vrn

class CorrectionRequestSpec extends SpecBase
  with ScalaCheckPropertyChecks
  with EitherValues {

  "CorrectionRequest" - {
    "must serialise and deserialise correctly" in {

      val vrn = Vrn("vrn")
      val period = StandardPeriod(2021,Q4)
      val corrections = List(PeriodWithCorrections(StandardPeriod(2021,Q4),None))

      val json = Json.obj(
        "vrn" -> Vrn("vrn"),
        "period" -> StandardPeriod(2021,Q4),
        "corrections" -> List(PeriodWithCorrections(StandardPeriod(2021,Q4),None))
      )

      val expectedResult = CorrectionRequest(vrn, period, corrections)

      Json.toJson(expectedResult) mustBe json
      json.validate[CorrectionRequest] mustBe JsSuccess(expectedResult)
    }
  }

}
