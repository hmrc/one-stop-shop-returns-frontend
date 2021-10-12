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
import generators.Generators
import models.Period
import models.Quarter._
import models.domain.VatReturn
import models.responses.NotFound
import org.mockito.{ArgumentMatchers, Mockito}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.PeriodService
import views.html.IndexView

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class YourAccountControllerSpec extends SpecBase with MockitoSugar with Generators with BeforeAndAfterEach {

  private val vatReturnConnector = mock[VatReturnConnector]
  private val periodService = mock[PeriodService]

  override def beforeEach(): Unit = {
    Mockito.reset(vatReturnConnector)
    Mockito.reset(periodService)
    super.beforeEach()
  }

  "Your Account Controller" - {

    "must return OK and the correct view" - {

      "when there are no returns due" in {

        val instant = Instant.parse("2021-10-11T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[PeriodService].toInstance(periodService)
          ).build()

        val vatReturn = arbitrary[VatReturn].sample.value

        when(periodService.getReturnPeriods(any())) thenReturn Seq(Period(2021, Q3))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            Seq(),
            None
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return due" in {

        val instant = Instant.parse("2021-10-11T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[PeriodService].toInstance(periodService)
          ).build()

        when(periodService.getReturnPeriods(any())) thenReturn Seq(Period(2021, Q3))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            Seq(),
            Some(Period(2021, Q3))
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return overdue" in {

        val instant = Instant.parse("2021-11-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[PeriodService].toInstance(periodService)
          ).build()

        when(periodService.getReturnPeriods(any())) thenReturn Seq(Period(2021, Q3))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            Seq(Period(2021, Q3)),
            None
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return due, 1 return overdue" in {

        val instant = Instant.parse("2022-01-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[PeriodService].toInstance(periodService)
          ).build()

        when(periodService.getReturnPeriods(any())) thenReturn Seq(Period(2021, Q3), Period(2021, Q4))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            Seq(Period(2021, Q3)),
            Some(Period(2021, Q4))
          )(request, messages(application)).toString
        }
      }

      "when there is 2 returns overdue" in {

        val instant = Instant.parse("2022-02-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[PeriodService].toInstance(periodService)
          ).build()

        when(periodService.getReturnPeriods(any())) thenReturn Seq(Period(2021, Q3), Period(2021, Q4))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            Seq(Period(2021, Q3), Period(2021, Q4)),
            None
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return is completed 1 return is due" in {

        val instant = Instant.parse("2022-01-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[PeriodService].toInstance(periodService)
          ).build()

        val firstPeriod = Period(2021, Q3)
        val secondPeriod = Period(2021, Q4)

        val vatReturn = arbitrary[VatReturn].sample.value

        when(periodService.getReturnPeriods(any())) thenReturn Seq(firstPeriod, secondPeriod)
        when(vatReturnConnector.get(ArgumentMatchers.eq(firstPeriod))(any())) thenReturn Future.successful(Right(vatReturn))
        when(vatReturnConnector.get(ArgumentMatchers.eq(secondPeriod))(any())) thenReturn Future.successful(Left(NotFound))

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            Seq(),
            Some(secondPeriod)
          )(request, messages(application)).toString
        }
      }
    }
  }
}