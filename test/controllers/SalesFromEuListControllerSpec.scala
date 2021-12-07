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
import config.FrontendAppConfig
import forms.SalesFromEuListFormProvider
import models.{Country, NormalMode}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import pages.{CountryOfSaleFromEuPage, SalesFromEuListPage}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import viewmodels.checkAnswers.SalesFromEuSummary
import views.html.SalesFromEuListView

class SalesFromEuListControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new SalesFromEuListFormProvider()
  private val form         = formProvider()

  private lazy val salesFromEuListRoute = routes.SalesFromEuListController.onPageLoad(NormalMode, period).url

  private val country = arbitrary[Country].sample.value

  private val baseAnswers =
    emptyUserAnswers
      .set(CountryOfSaleFromEuPage(index), country).success.value

  "SalesFromEuList Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, salesFromEuListRoute)

        val result = route(application, request).value

        val view                    = application.injector.instanceOf[SalesFromEuListView]
        implicit val msgs: Messages = messages(application)
        val list                    = SalesFromEuSummary.addToListRows(baseAnswers, NormalMode)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, list, period, canAddCountries = true)(request, implicitly).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val application = applicationBuilder(Some(baseAnswers)).build()
      val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

      running(application) {
        val request   = FakeRequest(POST, salesFromEuListRoute).withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual SalesFromEuListPage.navigate(baseAnswers, NormalMode, addAnother = true).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(Some(baseAnswers)).build()

      running(application) {
        val request   = FakeRequest(POST, salesFromEuListRoute).withFormUrlEncodedBody("value" -> "")
        val boundForm = form.bind(Map("value" -> ""))

        val result = route(application, request).value

        val view                    = application.injector.instanceOf[SalesFromEuListView]
        implicit val msgs: Messages = messages(application)
        val list                    = SalesFromEuSummary.addToListRows(baseAnswers, NormalMode)

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, list, period, canAddCountries = true)(request, implicitly).toString
      }
    }
  }
}
