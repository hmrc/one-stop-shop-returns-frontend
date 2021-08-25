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
import models.ReturnReference
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.CurrencyFormatter._
import views.html.ReturnSubmittedView

class ReturnSubmittedControllerSpec extends SpecBase {

  "ReturnSubmitted controller" - {

    "must return OK and the correct view" in {

      val app = applicationBuilder(Some(emptyUserAnswers)).build()

      running(app) {
        val request = FakeRequest(GET, routes.ReturnSubmittedController.onPageLoad(period).url)

        val result = route(app, request).value

        val view = app.injector.instanceOf[ReturnSubmittedView]
        val returnReference = ReturnReference(vrn, period)
        val vatOwed = currencyFormat(1)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(period, returnReference, vatOwed, registration.contactDetails.emailAddress)(request, messages(app)).toString
      }
    }
  }
}
