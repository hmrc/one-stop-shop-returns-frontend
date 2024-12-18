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
import connectors.corrections.CorrectionConnector
import models.ReturnReference
import models.corrections.CorrectionPayload
import models.domain.VatReturn
import models.external.ExternalEntryUrl
import models.responses.NotFound
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.corrections.CorrectPreviousReturnPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.EmailConfirmationQuery
import repositories.UserAnswersRepository
import services.VatReturnSalesService
import utils.CurrencyFormatter._
import views.html.ReturnSubmittedView

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class ReturnSubmittedControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with ScalaCheckPropertyChecks {

  private val vatReturnConnector = mock[VatReturnConnector]
  private val vatReturnSalesService = mock[VatReturnSalesService]
  private val correctionConnector = mock[CorrectionConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(vatReturnConnector)
    Mockito.reset(vatReturnSalesService)
    Mockito.reset(correctionConnector)
    super.beforeEach()
  }

  private val vatReturn = arbitrary[VatReturn].sample.value
  private val correctionPayload = arbitrary[CorrectionPayload].sample.value

  "ReturnSubmitted controller" - {

    "when correct previous return is false / empty" - {

      "must return OK and the correct view for a GET with email confirmation" in {
        val instant = Instant.parse("2021-10-30T00:00:00.00Z")
        val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val userAnswersWithEmail =
          emptyUserAnswers.copy().set(EmailConfirmationQuery, true).success.value

        val app = applicationBuilder(Some(userAnswersWithEmail), clock = Some(stubClock))
          .overrides(

            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[CorrectionConnector].toInstance(correctionConnector)
          )
          .build()

        val vatOnSales = BigDecimal(0)

        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(None))) thenReturn vatOnSales
        when(correctionConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
        when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

        running(app) {
          val request = FakeRequest(GET, routes.ReturnSubmittedController.onPageLoad(period).url)

          val result = route(app, request).value

          val view = app.injector.instanceOf[ReturnSubmittedView]
          val returnReference = ReturnReference(vrn, period)
          val vatOwed = currencyFormat(vatOnSales)

          status(result) mustEqual OK

          contentAsString(result) mustEqual
            view(
              period,
              returnReference,
              vatOwed,
              true,
              registration.contactDetails.emailAddress,
              false,
              (vatOnSales * 100).toLong,
              false
            )(request, messages(app)).toString
        }
      }

      "must return OK and the correct view for a GET without email confirmation" in {
        val instant = Instant.parse("2021-10-30T00:00:00.00Z")
        val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val userAnswersWithoutEmail =
          emptyUserAnswers.copy().set(EmailConfirmationQuery, false).success.value

        val app = applicationBuilder(Some(userAnswersWithoutEmail), clock = Some(stubClock))
          .overrides(
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[CorrectionConnector].toInstance(correctionConnector)
          )
          .build()

        val vatOnSales = arbitrary[BigDecimal].sample.value

        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(None))) thenReturn vatOnSales
        when(correctionConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
        when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

        running(app) {
          val request = FakeRequest(GET, routes.ReturnSubmittedController.onPageLoad(period).url)

          val result = route(app, request).value

          val view = app.injector.instanceOf[ReturnSubmittedView]
          val returnReference = ReturnReference(vrn, period)
          val vatOwed = currencyFormat(vatOnSales)
          val displayPayNow = vatOnSales > 0

          status(result) mustEqual OK

          contentAsString(result) mustEqual
            view(
              period,
              returnReference,
              vatOwed,
              false,
              registration.contactDetails.emailAddress,
              displayPayNow,
              (vatOnSales * 100).toLong,
              false
            )(request, messages(app)).toString
        }
      }

      "must return OK and the correct view for a GET with email confirmation and overdue return" in {
        val instant = Instant.parse("2021-11-02T00:00:00.00Z")
        val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val userAnswersWithEmail =
          emptyUserAnswers.copy().set(EmailConfirmationQuery, true).success.value

        val app = applicationBuilder(Some(userAnswersWithEmail), clock = Some(stubClock))
          .overrides(
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[CorrectionConnector].toInstance(correctionConnector)
          )
          .build()

        val vatOnSales = BigDecimal(100)

        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(None))) thenReturn vatOnSales
        when(correctionConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
        when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

        running(app) {
          val request = FakeRequest(GET, routes.ReturnSubmittedController.onPageLoad(period).url)

          val result = route(app, request).value

          val view = app.injector.instanceOf[ReturnSubmittedView]
          val returnReference = ReturnReference(vrn, period)
          val vatOwed = currencyFormat(vatOnSales)

          status(result) mustEqual OK
          contentAsString(result) mustEqual
            view(
              period,
              returnReference,
              vatOwed,
              true,
              registration.contactDetails.emailAddress,
              true,
              (vatOnSales * 100).toLong,
              true
            )(request, messages(app)).toString
        }
      }

      "must return redirect and the correct view" in {

        val app = applicationBuilder(Some(emptyUserAnswers))
          .overrides(
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[CorrectionConnector].toInstance(correctionConnector)
          )
          .build()

        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
        when(correctionConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
        when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

        running(app) {
          val request = FakeRequest(GET, routes.ReturnSubmittedController.onPageLoad(period).url)

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
        }
      }
    }

    "when correct previous return is true" - {

      "must return OK and the correct view for a GET with email confirmation" in {
        val instant = Instant.parse("2021-10-30T00:00:00.00Z")
        val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val userAnswersWithEmail =
          emptyUserAnswers
            .set(EmailConfirmationQuery, true).success.value
            .set(CorrectPreviousReturnPage, true).success.value

        val app = applicationBuilder(Some(userAnswersWithEmail), clock = Some(stubClock))
          .overrides(
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[CorrectionConnector].toInstance(correctionConnector)
          )
          .build()

        val vatOnSales = BigDecimal(0)

        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(Some(correctionPayload)))) thenReturn vatOnSales
        when(correctionConnector.get(any())(any())) thenReturn Future.successful(Right(correctionPayload))
        when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

        running(app) {
          val request = FakeRequest(GET, routes.ReturnSubmittedController.onPageLoad(period).url)

          val result = route(app, request).value

          val view = app.injector.instanceOf[ReturnSubmittedView]
          val returnReference = ReturnReference(vrn, period)
          val vatOwed = currencyFormat(vatOnSales)

          status(result) mustEqual OK

          contentAsString(result) mustEqual
            view(
              period,
              returnReference,
              vatOwed,
              true,
              registration.contactDetails.emailAddress,
              false,
              (vatOnSales * 100).toLong,
              false
            )(request, messages(app)).toString
        }
      }

      "must return OK and the correct view for a GET without email confirmation" in {
        val instant = Instant.parse("2021-10-30T00:00:00.00Z")
        val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val userAnswersWithoutEmail =
          emptyUserAnswers
            .set(EmailConfirmationQuery, false).success.value
            .set(CorrectPreviousReturnPage, true).success.value

        val app = applicationBuilder(Some(userAnswersWithoutEmail), clock = Some(stubClock))
          .overrides(
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[CorrectionConnector].toInstance(correctionConnector)
          )
          .build()

        val vatOnSales = arbitrary[BigDecimal].sample.value

        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(Some(correctionPayload)))) thenReturn vatOnSales
        when(correctionConnector.get(any())(any())) thenReturn Future.successful(Right(correctionPayload))
        when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

        running(app) {
          val request = FakeRequest(GET, routes.ReturnSubmittedController.onPageLoad(period).url)

          val result = route(app, request).value

          val view = app.injector.instanceOf[ReturnSubmittedView]
          val returnReference = ReturnReference(vrn, period)
          val vatOwed = currencyFormat(vatOnSales)
          val displayPayNow = vatOnSales > 0

          status(result) mustEqual OK

          contentAsString(result) mustEqual
            view(
              period,
              returnReference,
              vatOwed,
              false,
              registration.contactDetails.emailAddress,
              displayPayNow,
              (vatOnSales * 100).toLong,
              false
            )(request, messages(app)).toString
        }
      }

      "must return OK and the correct view for a GET with email confirmation and overdue return" in {
        val instant = Instant.parse("2021-11-02T00:00:00.00Z")
        val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val userAnswersWithEmail =
          emptyUserAnswers
            .set(EmailConfirmationQuery, true).success.value
            .set(CorrectPreviousReturnPage, true).success.value

        val app = applicationBuilder(Some(userAnswersWithEmail), clock = Some(stubClock))
          .overrides(
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[CorrectionConnector].toInstance(correctionConnector)
          )
          .build()

        val vatOnSales = BigDecimal(100)

        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(Some(correctionPayload)))) thenReturn vatOnSales
        when(correctionConnector.get(any())(any())) thenReturn Future.successful(Right(correctionPayload))
        when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

        running(app) {
          val request = FakeRequest(GET, routes.ReturnSubmittedController.onPageLoad(period).url)

          val result = route(app, request).value

          val view = app.injector.instanceOf[ReturnSubmittedView]
          val returnReference = ReturnReference(vrn, period)
          val vatOwed = currencyFormat(vatOnSales)

          status(result) mustEqual OK
          contentAsString(result) mustEqual
            view(
              period,
              returnReference,
              vatOwed,
              true,
              registration.contactDetails.emailAddress,
              true,
              (vatOnSales * 100).toLong,
              true
            )(request, messages(app)).toString
        }
      }

      "must return redirect and the correct view" in {

        val userAnswers = emptyUserAnswers
          .set(CorrectPreviousReturnPage, true).success.value

        val app = applicationBuilder(Some(userAnswers))
          .overrides(
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[CorrectionConnector].toInstance(correctionConnector)
          )
          .build()

        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
        when(correctionConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
        when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

        running(app) {
          val request = FakeRequest(GET, routes.ReturnSubmittedController.onPageLoad(period).url)

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
        }
      }
    }

    "must clear user-answers on Page Load after return submitted" in {
      val mockSessionRepository = mock[UserAnswersRepository]

      val answers = completeUserAnswers

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
      when(correctionConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

      when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)

      val app = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[UserAnswersRepository].toInstance(mockSessionRepository),
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[CorrectionConnector].toInstance(correctionConnector)
        ).build()

      running(app) {

        val request = FakeRequest(GET, routes.ReturnSubmittedController.onPageLoad(period).url)

        val result = route(app, request).value

        status(result) mustEqual OK
        verify(mockSessionRepository, times(1)).clear(eqTo(answers.userId))
      }
    }

    "must add the external backToYourAccount url that has been saved" in {
      val instant = Instant.parse("2021-10-30T00:00:00.00Z")
      val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

      val userAnswersWithEmail =
        emptyUserAnswers.copy().set(EmailConfirmationQuery, true).success.value

      val app = applicationBuilder(Some(userAnswersWithEmail), clock = Some(stubClock))
        .overrides(

          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[VatReturnSalesService].toInstance(vatReturnSalesService),
          bind[CorrectionConnector].toInstance(correctionConnector)
        )
        .build()

      val vatOnSales = BigDecimal(0)

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
      when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), eqTo(None))) thenReturn vatOnSales
      when(correctionConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(Some("example"))))

      running(app) {
        val request = FakeRequest(GET, routes.ReturnSubmittedController.onPageLoad(period).url)

        val result = route(app, request).value

        val view = app.injector.instanceOf[ReturnSubmittedView]
        val returnReference = ReturnReference(vrn, period)
        val vatOwed = currencyFormat(vatOnSales)

        status(result) mustEqual OK

        contentAsString(result) mustEqual
          view(
            period,
            returnReference,
            vatOwed,
            true,
            registration.contactDetails.emailAddress,
            false,
            (vatOnSales * 100).toLong,
            false,
            Some("example")
          )(request, messages(app)).toString
      }
    }
  }
}
