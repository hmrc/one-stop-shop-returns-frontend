/*
 * Copyright 2022 HM Revenue & Customs
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
import models.{Country, NormalMode}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, CountryVatCorrectionPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.VatReturnService
import views.html.corrections.CheckVatPayableAmountView

import scala.concurrent.Future

class CheckVatPayableAmountControllerSpec extends SpecBase {

  "CheckVatPayableAmount Controller" - {

    "must return OK and the correct view for a GET" in {
      val mockService = mock[VatReturnService]
      when(mockService.getLatestVatAmountForPeriodAndCountry(any(), any())(any(), any())) thenReturn Future.successful(BigDecimal(20))
      val userAnswers = emptyUserAnswers.set(CorrectionCountryPage(index, index), Country("DE", "Germany")).success.value
        .set(CorrectionReturnPeriodPage(index), period).success.value
        .set(CountryVatCorrectionPage(index, index), BigDecimal(10)).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[VatReturnService].toInstance(mockService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.corrections.routes.CheckVatPayableAmountController.onPageLoad(NormalMode, period, index, index).url)
        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckVatPayableAmountView]


        status(result) mustEqual OK
        contentAsString(result).contains("Correction amount") mustBe true
        contentAsString(result).contains("Previous VAT total declared") mustBe true
        contentAsString(result).contains("New VAT total") mustBe true
      }
    }

    "must redirect to Journey Recovery if no correction period or country found in user answers" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.corrections.routes.CheckVatPayableAmountController.onPageLoad(NormalMode, period, index, index).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
