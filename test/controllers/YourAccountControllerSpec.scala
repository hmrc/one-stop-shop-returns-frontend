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

package controllers

import base.SpecBase
import connectors.financialdata.FinancialDataConnector
import connectors.{ReturnStatusConnector, SaveForLaterConnector}
import generators.Generators
import models.{Period, SubmissionStatus}
import models.Quarter._
import models.SubmissionStatus.{Due, Next, Overdue}
import models.financialdata.{CurrentPayments, Payment, PaymentStatus}
import models.responses.{InvalidJson, UnexpectedResponseStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.SavedProgressPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.UserAnswersRepository
import services.VatReturnSalesService
import viewmodels.yourAccount.{OpenReturns, Return}
import views.html.IndexView

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class YourAccountControllerSpec extends SpecBase with MockitoSugar with Generators with BeforeAndAfterEach {

  private val returnStatusConnector = mock[ReturnStatusConnector]
  private val financialDataConnector = mock[FinancialDataConnector]
  private val vatReturnSalesService = mock[VatReturnSalesService]
  private val sessionRepository = mock[UserAnswersRepository]
  private val save4LaterConnector = mock[SaveForLaterConnector]

  "Your Account Controller" - {

    "must return OK and the correct view with no saved answers" - {

      "when there are no returns due" in {

        val instant = Instant.parse("2021-10-11T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val nextPeriod = Period(2021, Q4)

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(Seq(Return(
              nextPeriod,
              nextPeriod.firstDay,
              nextPeriod.lastDay,
              nextPeriod.paymentDeadline,
              SubmissionStatus.Next,
              false,
              false
            ))))

        when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty)))

        when(sessionRepository.get(any())) thenReturn(Future.successful(Seq()))
        when(sessionRepository.set(any())) thenReturn(Future.successful(true))
        when(save4LaterConnector.get()(any())) thenReturn(Future.successful(Right(None)))
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector),
          ).build()

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            OpenReturns(
              None,
              None,
              Seq.empty,
              Some(Return.fromPeriod(nextPeriod, Next, false, false))
            ),
            CurrentPayments(Seq.empty, Seq.empty),
            paymentError = false
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return due" in {

        val instant = Instant.parse("2021-10-11T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
        val period = Period(2021, Q3)
        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(Seq(Return.fromPeriod(period, Due, false, true))))

        when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty)))
        when(sessionRepository.get(any())) thenReturn(Future.successful(Seq()))
        when(sessionRepository.set(any())) thenReturn(Future.successful(true))

        when(save4LaterConnector.get()(any())) thenReturn(Future.successful(Right(None)))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector)
          ).build()

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            OpenReturns(
              None,
              Some(Return.fromPeriod(period, Due, false, true)),
              Seq.empty,
              None
            ),
            CurrentPayments(Seq.empty, Seq.empty),
            paymentError = false
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return overdue" in {

        val instant = Instant.parse("2021-11-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val period = Period(2021, Q3)
        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(Seq(Return.fromPeriod(period, Overdue, false, true))))

        when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty)))
        when(sessionRepository.get(any())) thenReturn(Future.successful(Seq()))
        when(sessionRepository.set(any())) thenReturn(Future.successful(true))
        when(save4LaterConnector.get()(any())) thenReturn(Future.successful(Right(None)))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector)
          ).build()

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            OpenReturns(
              None,
              None,
              Seq(
                Return.fromPeriod(Period(2021, Q3), Overdue, false, true)),
              None
            ),
            CurrentPayments(Seq.empty, Seq.empty),
            paymentError = false
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return due, 1 return overdue" in {

        val instant = Instant.parse("2022-01-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val firstPeriod = Period(2021, Q3)
        val secondPeriod = Period(2021, Q4)
        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(Seq(
              Return.fromPeriod(secondPeriod, Due, false, false),
              Return.fromPeriod(firstPeriod, Overdue, false, true))))

        when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty)))

        when(sessionRepository.get(any())) thenReturn(Future.successful(Seq()))
        when(sessionRepository.set(any())) thenReturn(Future.successful(true))
        when(save4LaterConnector.get()(any())) thenReturn(Future.successful(Right(None)))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository)
          ).build()

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            OpenReturns(
              None,
              Some(Return.fromPeriod(secondPeriod, Due, false, false)),
              Seq(
                Return.fromPeriod(firstPeriod, Overdue, false, true)),
              None
            ),
            CurrentPayments(Seq.empty, Seq.empty),
            paymentError = false
          )(request, messages(application)).toString
        }
      }

      "when there is 2 returns overdue" in {

        val instant = Instant.parse("2022-02-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val firstPeriod = Period(2021, Q3)
        val secondPeriod = Period(2021, Q4)

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(Seq(
              Return.fromPeriod(firstPeriod, Overdue, false, true),
              Return.fromPeriod(secondPeriod, Overdue, false, false))))

        when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty)))

        when(sessionRepository.get(any())) thenReturn(Future.successful(Seq()))
        when(sessionRepository.set(any())) thenReturn(Future.successful(true))
        when(save4LaterConnector.get()(any())) thenReturn(Future.successful(Right(None)))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
          ).build()

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            OpenReturns(
              None,
              None,
              Seq(
                Return.fromPeriod(firstPeriod, Overdue, false, true),
                Return.fromPeriod(secondPeriod, Overdue, false, false)),
              None
            ),
            CurrentPayments(Seq.empty, Seq.empty),
            paymentError = false
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return completed 1 return is due" in {

        val instant = Instant.parse("2022-01-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val secondPeriod = Period(2021, Q4)
        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(Seq(Return.fromPeriod(secondPeriod, Due, false, true))))

        when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty)))

        when(sessionRepository.get(any())) thenReturn(Future.successful(Seq()))
        when(sessionRepository.set(any())) thenReturn(Future.successful(true))
        when(save4LaterConnector.get()(any())) thenReturn(Future.successful(Right(None)))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector)
          ).build()

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            OpenReturns(
              None,
              Some(Return.fromPeriod(secondPeriod, Due, false, true)),
              Seq.empty,
              None
            ),
            CurrentPayments(Seq.empty, Seq.empty),
            paymentError = false
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return completed and payment is outstanding" in {

        val instant = Instant.parse("2021-10-25T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val firstPeriod = Period(2021, Q3)
        val outstandingAmount = BigDecimal(1000)
        val payment = Payment(firstPeriod, outstandingAmount, firstPeriod.paymentDeadline, PaymentStatus.Unpaid)

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(Seq.empty))

        when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq(payment), Seq.empty))
          )

        when(sessionRepository.get(any())) thenReturn(Future.successful(Seq()))
        when(sessionRepository.set(any())) thenReturn(Future.successful(true))
        when(save4LaterConnector.get()(any())) thenReturn(Future.successful(Right(None)))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector)
          ).build()


        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            OpenReturns(
              None,
              None,
              Seq.empty,
              None
            ),
            CurrentPayments(
              Seq(payment),
              Seq.empty),
            paymentError = false
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return completed and payment is outstanding with a correction" in {

        val instant = Instant.parse("2021-10-25T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val firstPeriod = Period(2021, Q3)

        val payment = Payment(firstPeriod, 0, firstPeriod.paymentDeadline, PaymentStatus.Unpaid)

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(Seq.empty))

        when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq(payment), Seq.empty))
          )

        when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), any())) thenReturn BigDecimal(1000)

        when(sessionRepository.get(any())) thenReturn(Future.successful(Seq()))
        when(sessionRepository.set(any())) thenReturn(Future.successful(true))
        when(save4LaterConnector.get()(any())) thenReturn(Future.successful(Right(None)))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector)
          ).build()

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            OpenReturns(
              None,
              None,
              Seq.empty,
              None
            ),
            CurrentPayments(Seq(payment),
              Seq.empty),
            paymentError = false
          )(request, messages(application)).toString

        }
      }

      "when there is 1 nil return completed and payment is outstanding from a correction" in {

        val instant = Instant.parse("2021-10-25T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val firstPeriod = Period(2021, Q3)
        val payment = Payment(firstPeriod, 1000, firstPeriod.paymentDeadline, PaymentStatus.Unknown)
        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(Seq.empty))

        when(financialDataConnector.getCurrentPayments(any())(any()))
          .thenReturn(Future.successful(Right(CurrentPayments(Seq(payment), Seq.empty))))

        when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), any())) thenReturn BigDecimal(1000)

        when(sessionRepository.get(any())) thenReturn(Future.successful(Seq()))
        when(sessionRepository.set(any())) thenReturn(Future.successful(true))
        when(save4LaterConnector.get()(any())) thenReturn(Future.successful(Right(None)))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector)
          ).build()

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            OpenReturns(
              None,
              None,
              Seq.empty,
              None
            ),
            CurrentPayments(
              Seq(payment),
              Seq.empty),
            paymentError = true
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return completed and payment is outstanding and overdue" in {

        val instant = Instant.parse("2022-01-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val firstPeriod = Period(2021, Q1)
        val outstandingAmount = BigDecimal(1000)
        val overduePayment = Payment(firstPeriod, outstandingAmount, firstPeriod.paymentDeadline, PaymentStatus.Unpaid)

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(Seq.empty))

        when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
          Future.successful(
            Right(
              CurrentPayments(
                Seq.empty,
                Seq(overduePayment)
              )
            )
          )

        when(sessionRepository.get(any())) thenReturn(Future.successful(Seq()))
        when(sessionRepository.set(any())) thenReturn(Future.successful(true))
        when(save4LaterConnector.get()(any())) thenReturn(Future.successful(Right(None)))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector)
          ).build()

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            OpenReturns(
              None,
              None,
              Seq.empty,
              None
            ),
            CurrentPayments(Seq.empty, Seq(overduePayment)),
            paymentError = false
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return completed and payment errors" in {

        val instant = Instant.parse("2022-01-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(Seq.empty))

        when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
          Future.successful(
            Left(
              InvalidJson
            )
          )

        when(sessionRepository.get(any())) thenReturn(Future.successful(Seq()))
        when(save4LaterConnector.get()(any())) thenReturn(Future.successful(Right(None)))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector)
          ).build()

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            OpenReturns(
              None,
              None,
              Seq.empty,
              None
            ),
            CurrentPayments(Seq.empty, Seq.empty),
            paymentError = true
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return completed and charge is not in ETMP" in {
        val clock: Clock = Clock.fixed(Instant.parse("2021-10-25T12:00:00Z"), ZoneId.systemDefault)
        val vatOwed = BigDecimal(1563.49)
        val firstPeriod = Period(2021, Q3)
        val payment = Payment(firstPeriod, vatOwed, firstPeriod.paymentDeadline, PaymentStatus.Unknown)
        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(Seq.empty))

        when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
          Future.successful(Right(CurrentPayments(Seq(payment), Seq.empty)))

        when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), any())) thenReturn
          vatOwed

        when(sessionRepository.get(any())) thenReturn(Future.successful(Seq()))
        when(save4LaterConnector.get()(any())) thenReturn(Future.successful(Right(None)))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector)
          ).build()

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            OpenReturns(
              None,
              None,
              Seq.empty,
              None
            ),
            CurrentPayments(
              Seq(payment),
              Seq.empty
            ),
            paymentError = true
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return due and it is in progress" in {

        val instant = Instant.parse("2021-10-11T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
        val period = Period(2021, Q3)
        val userAnswers = emptyUserAnswers.set(SavedProgressPage, "test").success.value
        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(Seq(Return.fromPeriod(period, Due, true, true))))

        when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty)))
        when(sessionRepository.get(any())) thenReturn(Future.successful(Seq(userAnswers)))
        when(save4LaterConnector.get()(any())) thenReturn(Future.successful(Right(None)))
        when(sessionRepository.set(any())) thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(userAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector)
          ).build()

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            OpenReturns(
              Some(Return.fromPeriod(period, Due, true, true)),
              Some(Return.fromPeriod(period, Due, true, true)),
              Seq.empty,
              None
            ),
            CurrentPayments(Seq.empty, Seq.empty),
            paymentError = false
          )(request, messages(application)).toString
        }
      }

      "when a user has previously saved their return progress and their session has renewed" in {

        val instant = Instant.parse("2021-11-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val period = Period(2021, Q3)
        val answers = arbitrarySavedUserAnswers.arbitrary.sample.value.copy(period = period)
        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(Seq(Return.fromPeriod(period, Overdue, true, true))))
        when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty)))
        when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(Some(answers)))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector)
          ).build()

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            OpenReturns(
              Some(Return.fromPeriod(period, Overdue, true, true)),
              None,
              Seq(Return.fromPeriod(Period(2021, Q3), Overdue, true, true)),
              None
            ),
            CurrentPayments(Seq.empty, Seq.empty),
            paymentError = false
          )(request, messages(application)).toString

        }
      }

    }

    "must throw and exception when an error is returned from both connectors" in {

      when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
        Future.successful(Left(UnexpectedResponseStatus(1, "error")))

      when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
        Future.successful(
          Left(UnexpectedResponseStatus(1, "error")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[ReturnStatusConnector].toInstance(returnStatusConnector),
          bind[FinancialDataConnector].toInstance(financialDataConnector)
        ).build()

      running(application) {
        val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

        val result = route(application, request).value

        whenReady(result.failed) { exp => exp mustBe a[Exception] }
      }
    }

    "must throw and exception when an error is returned from the Return Status connector" in {

      when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
        Future.successful(Left(UnexpectedResponseStatus(1, "error")))

      when(financialDataConnector.getCurrentPayments(any())(any())) thenReturn
        Future.successful(
          Right(CurrentPayments(Seq.empty, Seq.empty)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[ReturnStatusConnector].toInstance(returnStatusConnector),
          bind[FinancialDataConnector].toInstance(financialDataConnector)
        ).build()

      running(application) {
        val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

        val result = route(application, request).value

        whenReady(result.failed) { exp => exp mustBe a[Exception] }
      }
    }
  }
}