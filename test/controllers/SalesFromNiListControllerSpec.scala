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
import forms.SalesFromNiListFormProvider
import models.{Country, NormalMode, VatOnSales, VatRate}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import pages.{CountryOfConsumptionFromNiPage, NetValueOfSalesFromNiPage, SalesFromNiListPage, VatOnSalesFromNiPage, VatRatesFromNiPage}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import viewmodels.checkAnswers.SalesFromNiSummary
import views.html.SalesFromNiListView

class SalesFromNiListControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new SalesFromNiListFormProvider()
  private val form         = formProvider()

  private lazy val salesFromNiListRoute = routes.SalesFromNiListController.onPageLoad(NormalMode, period).url
  def salesFromNiListRoutePost(prompt: Boolean) = routes.SalesFromNiListController.onSubmit(NormalMode, period, prompt).url

  private val country = arbitrary[Country].sample.value
  private val vatRate = arbitrary[VatRate].sample.value
  private val vat = arbitrary[VatOnSales].sample.value

  private val baseAnswers =
    emptyUserAnswers
      .set(CountryOfConsumptionFromNiPage(index), country).success.value

  private val answers = emptyUserAnswers.set(CountryOfConsumptionFromNiPage(index), country).success.value
    .set(VatRatesFromNiPage(index), List(vatRate)).success.value
    .set(NetValueOfSalesFromNiPage(index, index), BigDecimal(100)).success.value
    .set(VatOnSalesFromNiPage(index, index), vat).success.value

  "SalesFromNiList Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, salesFromNiListRoute)

        val result = route(application, request).value

        val view                    = application.injector.instanceOf[SalesFromNiListView]
        implicit val msgs: Messages = messages(application)
        val list                    = SalesFromNiSummary.addToListRows(answers, NormalMode)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, list, period, canAddCountries = true)(request, implicitly).toString
      }
    }

    "must return OK and show warning for a GET when previous answers are incomplete" in {

      val application = applicationBuilder(Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, salesFromNiListRoute)

        val result = route(application, request).value

        val view                    = application.injector.instanceOf[SalesFromNiListView]
        implicit val msgs: Messages = messages(application)
        val list                    = SalesFromNiSummary.addToListRows(baseAnswers, NormalMode)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, list, period, canAddCountries = true, List(country))(request, implicitly).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val application = applicationBuilder(Some(answers)).build()

      running(application) {
        val request   = FakeRequest(POST, salesFromNiListRoutePost(false)).withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual SalesFromNiListPage.navigate(answers, NormalMode, addAnother = true).url
      }
    }

    "must redirect to the CheckSales page for incomplete country sales and prompt was already shown" in {
      val incompleteAnswers = baseAnswers.set(VatRatesFromNiPage(index), List(vatRate)).success.value

      val application = applicationBuilder(Some(incompleteAnswers)).build()

      running(application) {
        val request   = FakeRequest(POST, salesFromNiListRoutePost(true))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.CheckSalesFromNiController.onPageLoad(NormalMode, period, index).url
      }
    }

    "must redirect to the Vat Rates page for incomplete country sales and prompt was already shown" in {
      val application = applicationBuilder(Some(baseAnswers)).build()

      running(application) {
        val request   = FakeRequest(POST, salesFromNiListRoutePost(true))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.VatRatesFromNiController.onPageLoad(NormalMode, period, index).url
      }
    }

    "must refresh the page for incomplete answers if prompt was not shown" in {
      val application = applicationBuilder(Some(baseAnswers)).build()

      running(application) {
        val request   = FakeRequest(POST, salesFromNiListRoutePost(false))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.SalesFromNiListController.onPageLoad(NormalMode, period).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(Some(answers)).build()

      running(application) {
        val request   = FakeRequest(POST, salesFromNiListRoutePost(false)).withFormUrlEncodedBody("value" -> "")
        val boundForm = form.bind(Map("value" -> ""))

        val result = route(application, request).value

        val view                    = application.injector.instanceOf[SalesFromNiListView]
        implicit val msgs: Messages = messages(application)
        val list                    = SalesFromNiSummary.addToListRows(answers, NormalMode)

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, list, period, canAddCountries = true)(request, implicitly).toString
      }
    }
  }
}
