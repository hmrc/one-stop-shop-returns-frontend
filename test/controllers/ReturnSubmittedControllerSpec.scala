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
import connectors.VatReturnConnector
import models.ReturnReference
import models.domain.VatReturn
import models.responses.NotFound
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.VatReturnSalesService
import utils.CurrencyFormatter._
import views.html.ReturnSubmittedView

import scala.concurrent.Future

class ReturnSubmittedControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with ScalaCheckPropertyChecks {

  private val vatReturnConnector = mock[VatReturnConnector]
  private val vatReturnSalesService = mock[VatReturnSalesService]

  override def beforeEach(): Unit = {
    Mockito.reset(vatReturnConnector)
    Mockito.reset(vatReturnSalesService)
    super.beforeEach()
  }

  private val vatReturn = arbitrary[VatReturn].sample.value

  "ReturnSubmitted controller" - {

    "must return OK and the correct view with pay now button" in {
      val vatOnSalesGen = Gen.choose(1, 10000000)

      forAll(vatOnSalesGen) {
        vat =>
          val app = applicationBuilder(Some(emptyUserAnswers))
            .overrides(
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[VatReturnSalesService].toInstance(vatReturnSalesService)
            ).build()

          val vatOnSales = vat

          when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
          when(vatReturnSalesService.getTotalVatOnSales(any())) thenReturn vatOnSales

          running(app) {
            val request = FakeRequest(GET, routes.ReturnSubmittedController.onPageLoad(period).url)

              val result = route(app, request).value

              val view = app.injector.instanceOf[ReturnSubmittedView]
              val returnReference = ReturnReference(vrn, period)
              val vatOwed = currencyFormat(vatOnSales)

              status(result) mustEqual OK
              contentAsString(result) mustEqual view(
                period,
                returnReference,
                vatOwed,
                registration.contactDetails.emailAddress,
                true,
                vatOnSales.toLong
            )(request, messages(app)).toString
          }
      }
    }

    "must return OK and the correct view without pay now button" in {

      val app = applicationBuilder(Some(emptyUserAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService)
        ).build()

      val vatOnSales = 0

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
      when(vatReturnSalesService.getTotalVatOnSales(any())) thenReturn vatOnSales

      running(app) {
        val request = FakeRequest(GET, routes.ReturnSubmittedController.onPageLoad(period).url)

        val result = route(app, request).value

        val view = app.injector.instanceOf[ReturnSubmittedView]
        val returnReference = ReturnReference(vrn, period)
        val vatOwed = currencyFormat(vatOnSales)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          period,
          returnReference,
          vatOwed,
          registration.contactDetails.emailAddress,
          false,
          vatOnSales.toLong
        )(request, messages(app)).toString
      }
    }

    "must return redirect and the correct view" in {

      val app = applicationBuilder(Some(emptyUserAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService)
        ).build()

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))

      running(app) {
        val request = FakeRequest(GET, routes.ReturnSubmittedController.onPageLoad(period).url)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
      }
    }
  }
}
