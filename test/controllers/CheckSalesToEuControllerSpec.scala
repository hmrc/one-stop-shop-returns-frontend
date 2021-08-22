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
import models.{Country, NormalMode}
import org.scalacheck.Arbitrary.arbitrary
import pages.{CheckSalesToEuPage, CountryOfConsumptionFromEuPage, CountryOfSaleFromEuPage}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import viewmodels.govuk.SummaryListFluency
import views.html.CheckSalesToEuView

class CheckSalesToEuControllerSpec extends SpecBase with SummaryListFluency {

  val countryFrom: Country = arbitrary[Country].sample.value
  val countryTo: Country   = arbitrary[Country].sample.value
  private val baseAnswers =
    emptyUserAnswers
      .set(CountryOfSaleFromEuPage(index), countryFrom).success.value
      .set(CountryOfConsumptionFromEuPage(index, index), countryTo).success.value

  "Check Your Answers Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckSalesToEuController.onPageLoad(NormalMode, period, index, index).url)

        val result = route(application, request).value

        val view     = application.injector.instanceOf[CheckSalesToEuView]
        val mainList = SummaryListViewModel(Seq.empty)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          NormalMode,
          mainList,
          Seq.empty,
          period,
          index,
          index,
          countryFrom,
          countryTo
        )(request, messages(application)).toString
      }
    }

    "must redirect to the next page for a POST" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckSalesToEuController.onPageLoad(NormalMode, period, index, index).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CheckSalesToEuPage(index).navigate(NormalMode, baseAnswers).url
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
  }
}
