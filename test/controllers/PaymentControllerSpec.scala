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
import connectors.PaymentConnector
import models.requests.{PaymentPeriod, PaymentRequest, PaymentResponse}
import models.responses.InvalidJson
import org.mockito.ArgumentMatchers.{any, anyLong}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.PaymentService

import scala.concurrent.Future

class PaymentControllerSpec extends SpecBase with MockitoSugar {

  val paymentService: PaymentService     = mock[PaymentService]
  val paymentConnector: PaymentConnector = mock[PaymentConnector]

  "Payment Controller" - {

    "should make request to pay-api successfully" in {
      val amount = 20000000
      val paymentRequest = PaymentRequest(
        registration.vrn,
        PaymentPeriod(completeUserAnswers.period),
        amount
      )

      when(paymentService.buildPaymentRequest(any(), any(), anyLong()))
        .thenReturn(paymentRequest)
      when(paymentConnector.submit(any())(any()))
        .thenReturn(Future.successful(Right(PaymentResponse("journeyId", "nextUrl"))))

      val application =
        applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(bind[PaymentConnector].toInstance(paymentConnector))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.PaymentController.makePayment(period, amount).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must endWith("nextUrl")
      }
    }

    "should handle a failed request to pay-api" in {
      val amount = 20000000
      val paymentRequest = PaymentRequest(
        registration.vrn,
        PaymentPeriod(completeUserAnswers.period),
        amount
      )

      when(paymentService.buildPaymentRequest(any(), any(), anyLong()))
        .thenReturn(paymentRequest)
      when(paymentConnector.submit(any())(any()))
        .thenReturn(Future.successful(Left(InvalidJson)))

      val application =
        applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(bind[PaymentConnector].toInstance(paymentConnector))
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.PaymentController.makePayment(period, amount).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must endWith("/pay/service-unavailable")
      }
    }
  }
}