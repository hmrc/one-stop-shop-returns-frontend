/*
 * Copyright 2026 HM Revenue & Customs
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

package models.upscan

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class UpscanRedirectErrorSpec extends SpecBase {

  "UpscanRedirectError" - {

    "must return an UpscanRedirectError when errorCode is present" in {

      val request = FakeRequest(GET, "/callback?errorCode=EntityTooSmall")

      val result = UpscanRedirectError.fromQuery(request)

      result mustBe Some(UpscanRedirectError(code = "EntityTooSmall", message = None))
    }

    "must return an UpscanRedirectError with message when errorCode and errorMessage are present" in {

      val request = FakeRequest(GET, "/callback?errorCode=EntityTooLarge&errorMessage=File%20is%20too%20large")

      val result = UpscanRedirectError.fromQuery(request)

      result mustBe Some(UpscanRedirectError(code = "EntityTooLarge", message = Some("File is too large")))
    }

    "must return None when errorCode is missing" in {

      val request = FakeRequest(GET, "/callback?errorMessage=File%20is%20too%20large")

      val result = UpscanRedirectError.fromQuery(request)

      result mustBe None
    }

    "must return None when no query parameters are present" in {

      val request = FakeRequest(GET, "/callback")

      val result = UpscanRedirectError.fromQuery(request)

      result mustBe None
    }
  }

}
