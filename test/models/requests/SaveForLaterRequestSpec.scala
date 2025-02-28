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

package models.requests

import base.SpecBase
import models.Quarter.Q4
import models.StandardPeriod
import play.api.libs.json.{JsSuccess, JsValue, Json}
import uk.gov.hmrc.domain.Vrn


class SaveForLaterRequestSpec extends SpecBase {

  "SaveForLaterRequest" - {
    "must serialise and deserialise correctly" in {

      val vrn: Vrn = Vrn("vrn")
      val period: StandardPeriod = StandardPeriod(2021,Q4)
      val data: JsValue = Json.toJson("data")

      val json = Json.obj(
        "vrn" -> Vrn("vrn"),
        "period" -> StandardPeriod(2021,Q4),
        "data" -> Json.toJson("data")
      )

      val expectedResult = SaveForLaterRequest(vrn,period,data)

      Json.toJson(expectedResult) mustBe json
      json.validate[SaveForLaterRequest] mustBe JsSuccess(expectedResult)
    }
  }

}
