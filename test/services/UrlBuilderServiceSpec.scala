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

package services

import config.FrontendAppConfig
import org.mockito.Mockito.when
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers.mustEqual
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl


class UrlBuilderServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar {

  ".loginContinueUrl" - {

    "must add an existing session Id as a querystring parameter" in {

      val config: FrontendAppConfig = mock[FrontendAppConfig]
      when(config.loginContinueUrl) thenReturn "http://localhost"

      val service = new UrlBuilderService(config)

      val result = service.loginContinueUrl(FakeRequest(GET, "/foo?k=session-id"))

      result mustEqual RedirectUrl("http://localhost/foo?k=session-id")
    }
  }

}
