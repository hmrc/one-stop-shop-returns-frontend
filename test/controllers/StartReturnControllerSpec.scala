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

package controllers

import base.SpecBase
import forms.StartReturnFormProvider
import models.NormalMode
import org.scalatestplus.mockito.MockitoSugar
import pages.StartReturnPage
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.StartReturnView

class StartReturnControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new StartReturnFormProvider()
  private val form = formProvider()

  private lazy val startReturnRoute = routes.StartReturnController.onPageLoad(period).url

  "StartReturn Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, startReturnRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[StartReturnView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, period)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, startReturnRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(StartReturnPage, true).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual StartReturnPage.navigate(period, startReturn = true).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, startReturnRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[StartReturnView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, period)(request, messages(application)).toString
      }
    }
  }
}
