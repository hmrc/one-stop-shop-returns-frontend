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

package controllers.external

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.NoMoreWelshTranslationsView
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

class NoMoreWelshControllerSpec extends SpecBase with MockitoSugar{


  ".onPageLoad" - {
      "must return OK and correct view" in {
        val application = applicationBuilder()
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.NoMoreWelshController.onPageLoad().url)
          val view = application.injector.instanceOf[NoMoreWelshTranslationsView]

          val result = route(application, request).value
          status(result) mustBe OK
          contentAsString(result) mustBe view()(request, messages(application)).toString
        }
      }
  }

  "NoMoreWelsh Controller" - {

    "must return OK and the correct view for a GET with no RedirectUrl" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.NoMoreWelshController.onPageLoad().url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[NoMoreWelshTranslationsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(None)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET with a valid redirectUrl" in {
      val redirectUrl = RedirectUrl("/relative-url")
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.NoMoreWelshController.onPageLoad(Some(redirectUrl)).url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[NoMoreWelshTranslationsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(Some("/relative-url"))(request, messages(application)).toString
      }
    }

    "must return OK and the correct view when given an invalid redirectUrl" in {
      val invalidRedirectUrl = RedirectUrl("http://malicious-site.com")
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.NoMoreWelshController.onPageLoad(Some(invalidRedirectUrl)).url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[NoMoreWelshTranslationsView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(None)(request, messages(application)).toString
      }
    }
  }
}
