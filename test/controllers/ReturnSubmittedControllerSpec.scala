/*
 * Copyright 2024 HM Revenue & Customs
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
import models.external.ExternalEntryUrl
import models.{ReturnReference, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{EmailConfirmationQuery, TotalAmountVatDueGBPQuery}
import repositories.UserAnswersRepository
import utils.CurrencyFormatter.*
import utils.FutureSyntax.FutureOps
import views.html.ReturnSubmittedView

import java.time.{Clock, Instant, ZoneId}

class ReturnSubmittedControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val vatReturnConnector = mock[VatReturnConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(vatReturnConnector)
    super.beforeEach()
  }

  private val vatOnSales: BigDecimal = arbitraryBigDecimal.arbitrary.sample.value

  "ReturnSubmitted controller" - {

    "must return OK and the correct view for a GET" in {

      val instant = Instant.parse("2021-10-30T00:00:00.00Z")
      val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

      val answers: UserAnswers = emptyUserAnswers
        .set(TotalAmountVatDueGBPQuery, vatOnSales).success.value

      val app = applicationBuilder(Some(answers), clock = Some(stubClock))
        .configure("urls.userResearch2" -> "https://test-url.com")
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector)
        ).build()

      val displayPayNow = vatOnSales > 0

      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(Some("example"))).toFuture

      running(app) {
        val request = FakeRequest(GET, routes.ReturnSubmittedController.onPageLoad(period).url)

        val result = route(app, request).value

        val view = app.injector.instanceOf[ReturnSubmittedView]
        val returnReference = ReturnReference(vrn, period)
        val vatOwed = currencyFormat(vatOnSales)

        status(result) mustBe OK

        contentAsString(result) mustBe
          view(
            period,
            returnReference,
            vatOwed,
            false,
            registration.contactDetails.emailAddress,
            displayPayNow,
            (vatOnSales * 100).toLong,
            false,
            Some("example"),
            "https://test-url.com"
          )(request, messages(app)).toString
      }
    }

    "must throw an Exception when there is no Total Amount Vat Due set in the user answers" in {

      val instant = Instant.parse("2021-10-30T00:00:00.00Z")
      val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

      val app = applicationBuilder(Some(emptyUserAnswers), clock = Some(stubClock))
        .configure("urls.userResearch2" -> "https://test-url.com")
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector)
        ).build()

      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture

      running(app) {
        val request = FakeRequest(GET, routes.ReturnSubmittedController.onPageLoad(period).url)

        val result = route(app, request).value

        whenReady(result.failed) { exp =>
          exp mustBe a[RuntimeException]
          exp.getMessage mustBe "VAT owed has not been set in answers"
        }
      }
    }

    "must clear user-answers on Page Load after return submitted" in {

      val mockSessionRepository = mock[UserAnswersRepository]

      val answers: UserAnswers = completeUserAnswers
        .copy()
        .set(TotalAmountVatDueGBPQuery, vatOnSales).success.value

      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture

      when(mockSessionRepository.clear(any())) thenReturn true.toFuture

      val app = applicationBuilder(userAnswers = Some(answers))
        .configure("urls.userResearch2" -> "https://test-url.com")
        .overrides(
          bind[UserAnswersRepository].toInstance(mockSessionRepository),
          bind[VatReturnConnector].toInstance(vatReturnConnector)
        ).build()

      running(app) {

        val request = FakeRequest(GET, routes.ReturnSubmittedController.onPageLoad(period).url)

        val result = route(app, request).value

        status(result) mustBe OK
        verify(mockSessionRepository, times(1)).clear(eqTo(answers.userId))
      }
    }

    "must add the external backToYourAccount url that has been saved" in {

      val instant = Instant.parse("2021-10-30T00:00:00.00Z")
      val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

      val vatOnSales: BigDecimal = BigDecimal(0)

      val userAnswersWithEmail = emptyUserAnswers
        .set(EmailConfirmationQuery, true).success.value
        .set(TotalAmountVatDueGBPQuery, vatOnSales).success.value

      val app = applicationBuilder(Some(userAnswersWithEmail), clock = Some(stubClock))
        .configure("urls.userResearch2" -> "https://test-url.com")
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector)
        ).build()

      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(Some("example"))).toFuture

      running(app) {
        val request = FakeRequest(GET, routes.ReturnSubmittedController.onPageLoad(period).url)

        val result = route(app, request).value

        val view = app.injector.instanceOf[ReturnSubmittedView]
        val returnReference = ReturnReference(vrn, period)
        val vatOwed = currencyFormat(vatOnSales)

        status(result) mustBe OK

        contentAsString(result) mustBe
          view(
            period,
            returnReference,
            vatOwed.toString(),
            true,
            registration.contactDetails.emailAddress,
            false,
            (vatOnSales * 100).toLong,
            false,
            Some("example"),
            "https://test-url.com"
          )(request, messages(app)).toString
      }
    }
  }
}
