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

package controllers

import base.SpecBase
import config.FrontendAppConfig
import models.NormalMode
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.CheckRegistrationUptoDateView

class CheckRegistrationUptoDateControllerSpec extends SpecBase {

  "CheckRegistrationUptoDate Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckRegistrationUptoDateController.onPageLoad(period).url)

        val result = route(application, request).value
        val config = application.injector.instanceOf[FrontendAppConfig]

        val view = application.injector.instanceOf[CheckRegistrationUptoDateView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(period, config.changeYourRegistrationUrl)(request, messages(application)).toString
      }
    }

    "must redirect to Want To Upload File page for a POST" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckRegistrationUptoDateController.onSubmit(period).url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.fileUpload.routes.WantToUploadFileController.onPageLoad(NormalMode, period).url
      }
    }
  }
}
