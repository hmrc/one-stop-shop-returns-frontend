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

package controllers.corrections

import base.SpecBase
import forms.corrections.VatCorrectionsListFormProvider
import models.{Country, NormalMode}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, CountryVatCorrectionPage, VatCorrectionsListPage, VatPayableForCountryPage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import viewmodels.checkAnswers.corrections.VatCorrectionsListSummary
import views.html.corrections.VatCorrectionsListView

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VatCorrectionsListControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new VatCorrectionsListFormProvider()
  private val form = formProvider()

  private lazy val vatCorrectionsListRoute = controllers.corrections.routes.VatCorrectionsListController.onPageLoad(NormalMode, period, index).url

  private val country = arbitrary[Country].sample.value

  private val baseAnswers =
    emptyUserAnswers
      .set(CorrectionCountryPage(index, index), country).success.value
      .set(CorrectionReturnPeriodPage(index), period).success.value
      .set(CountryVatCorrectionPage(index, index), BigDecimal(100.0)).success.value

  "VatCorrectionsList Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, vatCorrectionsListRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[VatCorrectionsListView]
        implicit val msgs: Messages = messages(application)
        val list                    = VatCorrectionsListSummary.addToListRows(baseAnswers, NormalMode, index)


        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form = form,
          mode = NormalMode,
          list = list,
          period = period,
          correctionPeriod = period,
          canAddCountries = true,
          periodIndex = index,
          allCorrectionsComplete = true
        )(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val application = applicationBuilder(Some(baseAnswers)).build()

      running(application) {
        val request   = FakeRequest(POST, vatCorrectionsListRoute).withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual VatCorrectionsListPage(index).navigate(baseAnswers, NormalMode, addAnother = true).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(Some(baseAnswers)).build()

      running(application) {
        val request   = FakeRequest(POST, vatCorrectionsListRoute).withFormUrlEncodedBody("value" -> "")
        val boundForm = form.bind(Map("value" -> ""))

        val result = route(application, request).value

        val view                    = application.injector.instanceOf[VatCorrectionsListView]
        implicit val msgs: Messages = messages(application)
        val list                    = VatCorrectionsListSummary.addToListRows(baseAnswers, NormalMode, index)

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          form = boundForm,
          mode = NormalMode,
          list = list,
          period = period,
          correctionPeriod = period,
          canAddCountries = true,
          periodIndex = index,
          allCorrectionsComplete = true
        )(request, implicitly).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, vatCorrectionsListRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, vatCorrectionsListRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
