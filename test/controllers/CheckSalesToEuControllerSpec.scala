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
import models.{Country, Index, NormalMode, VatOnSales, VatRate}
import org.scalacheck.Arbitrary.arbitrary
import pages.{CheckSalesToEuPage, CountryOfConsumptionFromEuPage, CountryOfSaleFromEuPage, NetValueOfSalesFromEuPage, VatOnSalesFromEuPage, VatRatesFromEuPage}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import viewmodels.TitledSummaryList
import viewmodels.checkAnswers.{NetValueOfSalesFromEuSummary, VatOnSalesFromEuSummary, VatRatesFromEuSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.CheckSalesToEuView

class CheckSalesToEuControllerSpec extends SpecBase with SummaryListFluency {

  val countryFrom: Country = arbitrary[Country].sample.value
  val countryTo: Country   = arbitrary[Country].sample.value
  val vatRate: VatRate = arbitrary[VatRate].sample.value
  val vatOnSales: VatOnSales = arbitrary[VatOnSales].sample.value
  private val baseAnswers =
    emptyUserAnswers
      .set(CountryOfSaleFromEuPage(index), countryFrom).success.value
      .set(CountryOfConsumptionFromEuPage(index, index), countryTo).success.value

  private val completeAnswers = baseAnswers
    .set(VatRatesFromEuPage(index, index), List(vatRate)).success.value
    .set(NetValueOfSalesFromEuPage(index, index, index), BigDecimal(100)).success.value
    .set(VatOnSalesFromEuPage(index, index, index), vatOnSales).success.value

  "Check Your Answers Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(completeAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckSalesToEuController.onPageLoad(NormalMode, period, index, index).url)

        val result = route(application, request).value
        implicit val msgs: Messages = messages(application)
        val view     = application.injector.instanceOf[CheckSalesToEuView]
        val ratesList = Seq(
          TitledSummaryList(
            title = s"${vatRate.rateForDisplay} VAT rate",
            list = SummaryListViewModel(
              rows = Seq(
                NetValueOfSalesFromEuSummary.row(completeAnswers, index, index, index, vatRate, NormalMode),
                VatOnSalesFromEuSummary.row(completeAnswers, index, index, index, vatRate, NormalMode)
              ).flatten
            )
          )
        )

        val mainList = SummaryListViewModel(
          rows = Seq(VatRatesFromEuSummary.row(completeAnswers, index, index, NormalMode)).flatten
        )

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          NormalMode,
          mainList,
          ratesList,
          period,
          index,
          index,
          countryFrom,
          countryTo,
          List.empty
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when answers are incomplete" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckSalesToEuController.onPageLoad(NormalMode, period, index, index).url)

        val result = route(application, request).value
        val view     = application.injector.instanceOf[CheckSalesToEuView]


        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          NormalMode,
          SummaryListViewModel(List.empty),
          List.empty,
          period,
          index,
          index,
          countryFrom,
          countryTo,
          List(countryTo.name)
        )(request, messages(application)).toString
      }
    }

    "must redirect to the next page for a POST" in {

      val application = applicationBuilder(userAnswers = Some(completeAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckSalesToEuController.onSubmit(NormalMode, period, index, index, false).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CheckSalesToEuPage(index).navigate(NormalMode, completeAnswers).url
      }

    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckSalesToEuController.onPageLoad(NormalMode, period, index, index).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must refresh the page if the answers are incomplete and the prompt was not showing for a POST" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckSalesToEuController.onSubmit(NormalMode, period, index, index, false).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CheckSalesToEuController.onPageLoad(NormalMode, period, index, index).url
      }

    }

    "must redirect to VatRatesFromEu if the answers are incomplete and the prompt was showing for a POST" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckSalesToEuController.onSubmit(NormalMode, period, index, index, true).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.VatRatesFromEuController.onPageLoad(NormalMode, period, index, index).url
      }

    }
  }
}
