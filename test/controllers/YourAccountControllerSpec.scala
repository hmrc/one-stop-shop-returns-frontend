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
import connectors.ReturnStatusConnector
import connectors.financialdata.FinancialDataConnector
import generators.Generators
import models.{Period, PeriodWithStatus, SubmissionStatus}
import models.Quarter._
import models.financialdata.{Charge, PeriodWithOutstandingAmount, VatReturnWithFinancialData}
import models.responses.InvalidJson
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.IndexView

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class YourAccountControllerSpec extends SpecBase with MockitoSugar with Generators with BeforeAndAfterEach {

  private val returnStatusConnector = mock[ReturnStatusConnector]
  private val financialDataConnector = mock[FinancialDataConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(returnStatusConnector)
    Mockito.reset(financialDataConnector)
    super.beforeEach()
  }

  "Your Account Controller" - {

    "must return OK and the correct view" - {

      "when there are no returns due" in {

        val instant = Instant.parse("2021-10-11T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector)
          ).build()

        when(returnStatusConnector.listStatuses(any())(any())) thenReturn
          Future.successful(
            Right(Seq.empty))

        when(financialDataConnector.getPeriodsAndOutstandingAmounts()(any())) thenReturn
          Future.successful(
            Right(Seq.empty))

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            Seq.empty,
            None,
            Seq.empty,
            Seq.empty,
            paymentError = false
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return due" in {

        val instant = Instant.parse("2021-10-11T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector)
          ).build()

        val period = Period(2021, Q3)

        when(returnStatusConnector.listStatuses(any())(any())) thenReturn
          Future.successful(Right(Seq(PeriodWithStatus(period, SubmissionStatus.Due))))

        when(financialDataConnector.getPeriodsAndOutstandingAmounts()(any())) thenReturn
          Future.successful(
            Right(Seq.empty))

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            Seq.empty,
            Some(Period(2021, Q3)),
            Seq.empty,
            Seq.empty,
            paymentError = false
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return overdue" in {

        val instant = Instant.parse("2021-11-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector)
          ).build()

        val period = Period(2021, Q3)

        when(returnStatusConnector.listStatuses(any())(any())) thenReturn Future.successful(Right(Seq(PeriodWithStatus(period, SubmissionStatus.Overdue))))

        when(financialDataConnector.getPeriodsAndOutstandingAmounts()(any())) thenReturn
          Future.successful(
            Right(Seq.empty))

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            Seq(Period(2021, Q3)),
            None,
            Seq.empty,
            Seq.empty,
            paymentError = false
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return due, 1 return overdue" in {

        val instant = Instant.parse("2022-01-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector)
          ).build()

        val firstPeriod = Period(2021, Q3)
        val secondPeriod = Period(2021, Q4)

        when(returnStatusConnector.listStatuses(any())(any())) thenReturn
          Future.successful(Right(Seq(
            PeriodWithStatus(firstPeriod, SubmissionStatus.Overdue),
            PeriodWithStatus(secondPeriod, SubmissionStatus.Due)
          )))

        when(financialDataConnector.getPeriodsAndOutstandingAmounts()(any())) thenReturn
          Future.successful(
            Right(Seq.empty))

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            Seq(firstPeriod),
            Some(secondPeriod),
            Seq.empty,
            Seq.empty,
            paymentError = false
          )(request, messages(application)).toString
        }
      }

      "when there is 2 returns overdue" in {

        val instant = Instant.parse("2022-02-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector)
          ).build()
        val firstPeriod = Period(2021, Q3)
        val secondPeriod = Period(2021, Q4)

        when(returnStatusConnector.listStatuses(any())(any())) thenReturn Future.successful(Right(Seq(
          PeriodWithStatus(firstPeriod, SubmissionStatus.Overdue),
          PeriodWithStatus(secondPeriod, SubmissionStatus.Overdue)
        )))

        when(financialDataConnector.getPeriodsAndOutstandingAmounts()(any())) thenReturn
          Future.successful(
            Right(Seq.empty))

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            Seq(firstPeriod, secondPeriod),
            None,
            Seq.empty,
            Seq.empty,
            paymentError = false
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return is completed 1 return is due" in {

        val instant = Instant.parse("2022-01-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector)
          ).build()

        val firstPeriod = Period(2021, Q3)
        val secondPeriod = Period(2021, Q4)

        when(returnStatusConnector.listStatuses(any())(any())) thenReturn
          Future.successful(
            Right(Seq(
              PeriodWithStatus(firstPeriod, SubmissionStatus.Complete),
              PeriodWithStatus(secondPeriod, SubmissionStatus.Due)
            )))

        when(financialDataConnector.getPeriodsAndOutstandingAmounts()(any())) thenReturn
          Future.successful(
            Right(Seq.empty))

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            Seq.empty,
            Some(secondPeriod),
            Seq.empty,
            Seq.empty,
            paymentError = false
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return is completed and payment is outstanding" in {

        val instant = Instant.parse("2022-01-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector)
          ).build()

        val firstPeriod = Period(2021, Q3)
        val secondPeriod = Period(2021, Q4)
        val outstandingAmount = BigDecimal(1000)
        val vatReturn = completeVatReturn.copy(period = firstPeriod)
        val vatReturnWithFinancialData = VatReturnWithFinancialData(
          vatReturn = vatReturn,
          charge = Some(Charge(
            period = firstPeriod,
            outstandingAmount = outstandingAmount,
            originalAmount = outstandingAmount,
            clearedAmount = BigDecimal(0)
          )),
          vatOwed = Some(outstandingAmount.toLong)
        )

        when(returnStatusConnector.listStatuses(any())(any())) thenReturn
          Future.successful(
            Right(Seq(
              PeriodWithStatus(firstPeriod, SubmissionStatus.Complete),
              PeriodWithStatus(secondPeriod, SubmissionStatus.Due)
            )))


        when(financialDataConnector.getVatReturnWithFinancialData(any())(any())) thenReturn
          Future.successful(
            Right(
              Seq(vatReturnWithFinancialData)
            )
          )

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            Seq.empty,
            Some(secondPeriod),
            Seq(vatReturnWithFinancialData),
            Seq.empty,
            paymentError = false
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return is completed and payment is outstanding and overdue" in {

        val instant = Instant.parse("2022-01-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector)
          ).build()

        val firstPeriod = Period(2021, Q1)
        val secondPeriod = Period(2021, Q2)
        val outstandingAmount = BigDecimal(1000)
        val vatReturn = completeVatReturn.copy(period = firstPeriod)
        val vatReturnWithFinancialData = VatReturnWithFinancialData(
          vatReturn = vatReturn,
          charge = Some(Charge(
            period = firstPeriod,
            outstandingAmount = outstandingAmount,
            originalAmount = outstandingAmount,
            clearedAmount = BigDecimal(0)
          )),
          vatOwed = Some(outstandingAmount.toLong)
        )

        when(returnStatusConnector.listStatuses(any())(any())) thenReturn
          Future.successful(
            Right(Seq(
              PeriodWithStatus(firstPeriod, SubmissionStatus.Complete),
              PeriodWithStatus(secondPeriod, SubmissionStatus.Overdue)
            )))

        when(financialDataConnector.getVatReturnWithFinancialData(any())(any())) thenReturn
          Future.successful(
            Right(
              Seq(vatReturnWithFinancialData)
            )
          )

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            Seq(secondPeriod),
            None,
            Seq.empty,
            Seq(vatReturnWithFinancialData),
            paymentError = false
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return is completed and payment errors" in {

        val instant = Instant.parse("2022-01-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector)
          ).build()

        val firstPeriod = Period(2021, Q1)
        val secondPeriod = Period(2021, Q2)

        when(returnStatusConnector.listStatuses(any())(any())) thenReturn
          Future.successful(
            Right(Seq(
              PeriodWithStatus(firstPeriod, SubmissionStatus.Complete),
              PeriodWithStatus(secondPeriod, SubmissionStatus.Overdue)
            )))

        when(financialDataConnector.getPeriodsAndOutstandingAmounts()(any())) thenReturn
          Future.successful(
            Left(
              InvalidJson
            )
          )

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            Seq(secondPeriod),
            None,
            Seq.empty,
            Seq.empty,
            paymentError = true
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return is completed and charge is not in ETMP" in {
        val clock: Clock = Clock.fixed(Instant.parse("2022-01-01T12:00:00Z"), ZoneId.systemDefault)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector)
          ).build()

        val firstPeriod = Period(2021, Q3)

        when(returnStatusConnector.listStatuses(any())(any())) thenReturn
          Future.successful(
            Right(Seq(
              PeriodWithStatus(firstPeriod, SubmissionStatus.Complete)
            )))

        when(financialDataConnector.getVatReturnWithFinancialData(any())(any())) thenReturn
          Future.successful(Right(Seq.empty))

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            Seq.empty,
            None,
            Seq.empty,
            Seq.empty,
            paymentError = true
          )(request, messages(application)).toString
        }
      }
    }
  }
}