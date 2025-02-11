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
import config.FrontendAppConfig
import connectors.{RegistrationConnector, ReturnStatusConnector, SaveForLaterConnector, VatReturnConnector}
import connectors.financialdata.FinancialDataConnector
import generators.Generators
import models.{Country, Period, StandardPeriod, SubmissionStatus}
import models.Quarter._
import models.SubmissionStatus.{Due, Excluded, Next, Overdue}
import models.domain.{EuTaxIdentifier, EuTaxIdentifierType, VatReturn}
import models.exclusions.{ExcludedTrader, ExclusionLinkView, ExclusionReason}
import models.financialdata.{CurrentPayments, Payment, PaymentStatus}
import models.registration._
import models.requests.RegistrationRequest
import models.responses.{InvalidJson, NotFound, UnexpectedResponseStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.SavedProgressPage
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.UserAnswersRepository
import services.VatReturnSalesService
import utils.FutureSyntax.FutureOps
import viewmodels.yourAccount.{CurrentReturns, PaymentsViewModel, Return, ReturnsViewModel}
import views.html.IndexView

import java.time.{Clock, Instant, LocalDate, ZoneId}
import scala.concurrent.Future

class YourAccountControllerSpec extends SpecBase with MockitoSugar with Generators with BeforeAndAfterEach {

  private val returnStatusConnector = mock[ReturnStatusConnector]
  private val financialDataConnector = mock[FinancialDataConnector]
  private val vatReturnSalesService = mock[VatReturnSalesService]
  private val sessionRepository = mock[UserAnswersRepository]
  private val save4LaterConnector = mock[SaveForLaterConnector]
  private val vatReturnConnector = mock[VatReturnConnector]
  private val registrationConnector = mock[RegistrationConnector]

  private val periodQ2 = StandardPeriod(2022, Q2)

  private val vatReturn = arbitrary[VatReturn].sample.value
  private val excludedTraderHMRC: Option[ExcludedTrader] = Some(ExcludedTrader(
    registration.vrn, ExclusionReason.CeasedTrade, periodQ2.firstDay, quarantined = false))

  private val excludedTraderSelf: Option[ExcludedTrader] = Some(ExcludedTrader(
    registration.vrn, ExclusionReason.NoLongerMeetsConditions, periodQ2.firstDay, quarantined = false))

  private val excludedTraderQuarantined: Option[ExcludedTrader] = Some(ExcludedTrader(
    registration.vrn, ExclusionReason.FailsToComply, periodQ2.firstDay, quarantined = true))

  private val excludedTraderSelfRequestedToLeave: Option[ExcludedTrader] = Some(ExcludedTrader(
    registration.vrn, ExclusionReason.NoLongerSupplies, LocalDate.now().plusMonths(1), quarantined = false))

  private val amendRegistrationUrl = "http://localhost:10200/pay-vat-on-goods-sold-to-eu/northern-ireland-register/start-amend-journey"

  override def beforeEach(): Unit = {
    Mockito.reset(vatReturnConnector)
    Mockito.reset(registrationConnector)
  }

  private val instant: Instant = Instant.parse("2021-10-11T12:00:00Z")
  private val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  "Your Account Controller" - {

    "must return OK and the correct view with no saved answers" - {

      "when there are no returns due" in {

        val instant = Instant.parse("2021-10-11T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val nextPeriod = StandardPeriod(2021, Q4)

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(Return(
                nextPeriod,
                nextPeriod.firstDay,
                nextPeriod.lastDay,
                nextPeriod.paymentDeadline,
                SubmissionStatus.Next,
                inProgress = false,
                isOldest = false
              ))))
          )

        when(financialDataConnector.getFinancialData(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

        when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
        when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(vatCustomerInfo))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector),
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          )
          .build()

        running(application) {
          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
          
          val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
            displayText = msgs("index.details.leaveThisService"),
            id = "leave-this-service",
            href = config.leaveOneStopShopUrl
          )

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            ReturnsViewModel(
              Seq(
                Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
              ), Seq.empty
            )(messages(application), clock),
            PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
            paymentError = false,
            None,
            hasSubmittedFinalReturn = false,
            currentReturnIsFinal = false,
            config.amendRegistrationEnabled,
            amendRegistrationUrl,
            hasRequestedToLeave = false,
            Some(exclusionLinkView),
            hasDueReturnThreeYearsOld = false,
            hasDueReturnsLessThanThreeYearsOld = false,
            hasDeregisteredFromVat = false
          )(request, msgs).toString

          contentAsString(result).contains("leave-this-service") mustEqual true
        }
      }

      "when there is 1 return due" - {

        "only" in {
          val instant = Instant.parse("2021-10-11T12:00:00Z")
          val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
          val period = StandardPeriod(2021, Q3)
          when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
            Future.successful(
              Right(CurrentReturns(
                Seq(Return.fromPeriod(period, Due, inProgress = false, isOldest = true)))
              )
            )

          when(financialDataConnector.getFinancialData(any())(any())) thenReturn
            Future.successful(
              Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))
          when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
          when(sessionRepository.set(any())) thenReturn Future.successful(true)

          when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
          when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
          when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
          when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(vatCustomerInfo))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
            .overrides(
              bind[ReturnStatusConnector].toInstance(returnStatusConnector),
              bind[FinancialDataConnector].toInstance(financialDataConnector),
              bind[UserAnswersRepository].toInstance(sessionRepository),
              bind[SaveForLaterConnector].toInstance(save4LaterConnector),
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[RegistrationConnector].toInstance(registrationConnector)
            ).build()

          running(application) {
            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
            val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

            val result = route(application, request).value

            val view = application.injector.instanceOf[IndexView]

            val config = application.injector.instanceOf[FrontendAppConfig]

            val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
              displayText = msgs("index.details.leaveThisService"),
              id = "leave-this-service",
              href = config.leaveOneStopShopUrl
            )

            status(result) mustEqual OK

            contentAsString(result) mustEqual view(
              registration.registeredCompanyName,
              registration.vrn.vrn,
              ReturnsViewModel(
                Seq(
                  Return.fromPeriod(period, Due, inProgress = false, isOldest = true)
                ), Seq.empty
              )(messages(application), clock),
              PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
              paymentError = false,
              None,
              hasSubmittedFinalReturn = false,
              currentReturnIsFinal = false,
              config.amendRegistrationEnabled,
              amendRegistrationUrl,
              hasRequestedToLeave = false,
              Some(exclusionLinkView),
              hasDueReturnThreeYearsOld = false,
              hasDueReturnsLessThanThreeYearsOld = false,
              hasDeregisteredFromVat = false
            )(request, msgs).toString
          }
        }

        "and it is in progress" in {

          val instant = Instant.parse("2021-10-11T12:00:00Z")
          val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
          val period = StandardPeriod(2021, Q3)
          val userAnswers = emptyUserAnswers.set(SavedProgressPage, "test").success.value
          when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
            Future.successful(
              Right(CurrentReturns(
                Seq(Return.fromPeriod(period, Due, inProgress = true, isOldest = true)))
              )
            )

          when(financialDataConnector.getFinancialData(any())(any())) thenReturn
            Future.successful(
              Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))
          when(sessionRepository.get(any())) thenReturn Future.successful(Seq(userAnswers))
          when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
          when(sessionRepository.set(any())) thenReturn Future.successful(true)
          when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
          when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
          when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(vatCustomerInfo))

          val application = applicationBuilder(userAnswers = Some(userAnswers), clock = Some(clock))
            .overrides(
              bind[ReturnStatusConnector].toInstance(returnStatusConnector),
              bind[FinancialDataConnector].toInstance(financialDataConnector),
              bind[UserAnswersRepository].toInstance(sessionRepository),
              bind[SaveForLaterConnector].toInstance(save4LaterConnector),
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[RegistrationConnector].toInstance(registrationConnector)
            ).build()

          running(application) {
            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
            val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

            val result = route(application, request).value

            val view = application.injector.instanceOf[IndexView]

            val config = application.injector.instanceOf[FrontendAppConfig]

            val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
              displayText = msgs("index.details.leaveThisService"),
              id = "leave-this-service",
              href = config.leaveOneStopShopUrl
            )

            status(result) mustEqual OK

            contentAsString(result) mustEqual view(
              registration.registeredCompanyName,
              registration.vrn.vrn,
              ReturnsViewModel(
                Seq(
                  Return.fromPeriod(period, Due, inProgress = true, isOldest = true)
                ), Seq.empty
              )(messages(application), clock),
              PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
              paymentError = false,
              None,
              hasSubmittedFinalReturn = false,
              currentReturnIsFinal = false,
              config.amendRegistrationEnabled,
              amendRegistrationUrl,
              hasRequestedToLeave = false,
              Some(exclusionLinkView),
              hasDueReturnThreeYearsOld = false,
              hasDueReturnsLessThanThreeYearsOld = false,
              hasDeregisteredFromVat = false
            )(request, msgs).toString
          }
        }

        "and 1 return is completed" in {

          val instant = Instant.parse("2022-01-01T12:00:00Z")
          val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

          val secondPeriod = StandardPeriod(2021, Q4)
          when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
            Future.successful(
              Right(CurrentReturns(
                Seq(Return.fromPeriod(secondPeriod, Due, inProgress = false, isOldest = true)))
              )
            )

          when(financialDataConnector.getFinancialData(any())(any())) thenReturn
            Future.successful(
              Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

          when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
          when(sessionRepository.set(any())) thenReturn Future.successful(true)
          when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
          when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
          when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
          when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(vatCustomerInfo))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
            .overrides(
              bind[ReturnStatusConnector].toInstance(returnStatusConnector),
              bind[FinancialDataConnector].toInstance(financialDataConnector),
              bind[UserAnswersRepository].toInstance(sessionRepository),
              bind[SaveForLaterConnector].toInstance(save4LaterConnector),
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[RegistrationConnector].toInstance(registrationConnector)
            ).build()

          running(application) {
            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
            val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

            val result = route(application, request).value

            val view = application.injector.instanceOf[IndexView]

            val config = application.injector.instanceOf[FrontendAppConfig]

            val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
              displayText = msgs("index.details.leaveThisService"),
              id = "leave-this-service",
              href = config.leaveOneStopShopUrl
            )

            status(result) mustEqual OK

            contentAsString(result) mustEqual view(
              registration.registeredCompanyName,
              registration.vrn.vrn,
              ReturnsViewModel(
                Seq(
                  Return.fromPeriod(secondPeriod, Due, inProgress = false, isOldest = true)
                ), Seq.empty
              )(messages(application), clock),
              PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
              paymentError = false,
              None,
              hasSubmittedFinalReturn = false,
              currentReturnIsFinal = false,
              config.amendRegistrationEnabled,
              amendRegistrationUrl,
              hasRequestedToLeave = false,
              Some(exclusionLinkView),
              hasDueReturnThreeYearsOld = false,
              hasDueReturnsLessThanThreeYearsOld = false,
              hasDeregisteredFromVat = false
            )(request, msgs).toString
          }
        }

        "and 1 return overdue" in {

          val instant = Instant.parse("2022-01-01T12:00:00Z")
          val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

          val firstPeriod = StandardPeriod(2021, Q3)
          val secondPeriod = StandardPeriod(2021, Q4)
          when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
            Future.successful(
              Right(CurrentReturns(
                Seq(
                  Return.fromPeriod(secondPeriod, Due, inProgress = false, isOldest = false),
                  Return.fromPeriod(firstPeriod, Overdue, inProgress = false, isOldest = true)))
              )
            )

          when(financialDataConnector.getFinancialData(any())(any())) thenReturn
            Future.successful(
              Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

          when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
          when(sessionRepository.set(any())) thenReturn Future.successful(true)
          when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
          when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
          when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
          when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(vatCustomerInfo))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
            .overrides(
              bind[ReturnStatusConnector].toInstance(returnStatusConnector),
              bind[FinancialDataConnector].toInstance(financialDataConnector),
              bind[SaveForLaterConnector].toInstance(save4LaterConnector),
              bind[UserAnswersRepository].toInstance(sessionRepository),
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[RegistrationConnector].toInstance(registrationConnector)
            ).build()

          running(application) {
            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
            val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

            val result = route(application, request).value

            val view = application.injector.instanceOf[IndexView]

            val config = application.injector.instanceOf[FrontendAppConfig]

            val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
              displayText = msgs("index.details.leaveThisService"),
              id = "leave-this-service",
              href = config.leaveOneStopShopUrl
            )

            status(result) mustEqual OK

            contentAsString(result) mustEqual view(
              registration.registeredCompanyName,
              registration.vrn.vrn,
              ReturnsViewModel(
                Seq(
                  Return.fromPeriod(secondPeriod, Due, inProgress = false, isOldest = false),
                  Return.fromPeriod(firstPeriod, Overdue, inProgress = false, isOldest = true)
                ), Seq.empty
              )(messages(application), clock),
              PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
              paymentError = false,
              None,
              hasSubmittedFinalReturn = false,
              currentReturnIsFinal = false,
              config.amendRegistrationEnabled,
              amendRegistrationUrl,
              hasRequestedToLeave = false,
              Some(exclusionLinkView),
              hasDueReturnThreeYearsOld = false,
              hasDueReturnsLessThanThreeYearsOld = false,
              hasDeregisteredFromVat = false
            )(request, msgs).toString
          }
        }
      }

      "when there is 1 return overdue" in {

        val instant = Instant.parse("2021-11-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val period = StandardPeriod(2021, Q3)
        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(Return.fromPeriod(period, Overdue, inProgress = false, isOldest = true)))
            )
          )

        when(financialDataConnector.getFinancialData(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))
        when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
        when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(vatCustomerInfo))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector),
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          ).build()

        running(application) {
          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
          val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
            displayText = msgs("index.details.leaveThisService"),
            id = "leave-this-service",
            href = config.leaveOneStopShopUrl
          )

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            ReturnsViewModel(Seq(
              Return.fromPeriod(StandardPeriod(2021, Q3), Overdue, inProgress = false, isOldest = true)
            ), Seq.empty
            )(messages(application), clock),
            PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
            paymentError = false,
            None,
            hasSubmittedFinalReturn = false,
            currentReturnIsFinal = false,
            config.amendRegistrationEnabled,
            amendRegistrationUrl,
            hasRequestedToLeave = false,
            Some(exclusionLinkView),
            hasDueReturnThreeYearsOld = false,
            hasDueReturnsLessThanThreeYearsOld = false,
            hasDeregisteredFromVat = false
          )(request, msgs).toString
        }
      }

      "when there is 2 returns overdue" in {

        val instant = Instant.parse("2022-02-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val firstPeriod = StandardPeriod(2021, Q3)
        val secondPeriod = StandardPeriod(2021, Q4)

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(
                Return.fromPeriod(firstPeriod, Overdue, inProgress = false, isOldest = true),
                Return.fromPeriod(secondPeriod, Overdue, inProgress = false, isOldest = false)))
            )
          )

        when(financialDataConnector.getFinancialData(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

        when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
        when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(vatCustomerInfo))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          ).build()

        running(application) {
          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
          val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
            displayText = msgs("index.details.leaveThisService"),
            id = "leave-this-service",
            href = config.leaveOneStopShopUrl
          )

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            ReturnsViewModel(
              Seq(
              Return.fromPeriod(firstPeriod, Overdue, inProgress = false, isOldest = true),
              Return.fromPeriod(secondPeriod, Overdue, inProgress = false, isOldest = false)
              ), Seq.empty
            )(messages(application), clock),
            PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
            paymentError = false,
            None,
            hasSubmittedFinalReturn = false,
            currentReturnIsFinal = false,
            config.amendRegistrationEnabled,
            amendRegistrationUrl,
            hasRequestedToLeave = false,
            Some(exclusionLinkView),
            hasDueReturnThreeYearsOld = false,
            hasDueReturnsLessThanThreeYearsOld = false,
            hasDeregisteredFromVat = false
          )(request, msgs).toString
        }
      }

      "when there is 1 return completed" - {

        "and payment is outstanding" in {

          val instant = Instant.parse("2021-10-25T12:00:00Z")
          val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

          val firstPeriod = StandardPeriod(2021, Q3)
          val outstandingAmount = BigDecimal(1000)
          val payment = Payment(firstPeriod, outstandingAmount, firstPeriod.paymentDeadline, PaymentStatus.Unpaid)

          when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
            Future.successful(
              Right(CurrentReturns(
                Seq(Return.fromPeriod(period, Next, inProgress = false, isOldest = false)))
              )
            )

          when(financialDataConnector.getFinancialData(any())(any())) thenReturn
            Future.successful(
              Right(CurrentPayments(Seq(payment), Seq.empty, Seq.empty, Seq.empty, payment.amountOwed, BigDecimal(0)))
            )

          when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
          when(sessionRepository.set(any())) thenReturn Future.successful(true)
          when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
          when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
          when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
          when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(vatCustomerInfo))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
            .overrides(
              bind[ReturnStatusConnector].toInstance(returnStatusConnector),
              bind[FinancialDataConnector].toInstance(financialDataConnector),
              bind[UserAnswersRepository].toInstance(sessionRepository),
              bind[SaveForLaterConnector].toInstance(save4LaterConnector),
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[RegistrationConnector].toInstance(registrationConnector)
            ).build()


          running(application) {
            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
            val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

            val result = route(application, request).value

            val view = application.injector.instanceOf[IndexView]

            val config = application.injector.instanceOf[FrontendAppConfig]

            val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
              displayText = msgs("index.details.leaveThisService"),
              id = "leave-this-service",
              href = config.leaveOneStopShopUrl
            )

            status(result) mustEqual OK

            contentAsString(result) mustEqual view(
              registration.registeredCompanyName,
              registration.vrn.vrn,
              ReturnsViewModel(Seq(Return.fromPeriod(period, Next, inProgress = false, isOldest = false)), Seq.empty)(messages(application), clock),
              PaymentsViewModel(
                Seq(payment),
                Seq.empty,
                Seq.empty,
                hasDueReturnThreeYearsOld = false
              )(messages(application), clock, registrationRequest),
              paymentError = false,
              None,
              hasSubmittedFinalReturn = false,
              currentReturnIsFinal = false,
              config.amendRegistrationEnabled,
              amendRegistrationUrl,
              hasRequestedToLeave = false,
              Some(exclusionLinkView),
              hasDueReturnThreeYearsOld = false,
              hasDueReturnsLessThanThreeYearsOld = false,
              hasDeregisteredFromVat = false
            )(request, msgs).toString
          }
        }

        "and payment is outstanding with a correction" in {

          val instant = Instant.parse("2021-10-25T12:00:00Z")
          val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

          val firstPeriod = StandardPeriod(2021, Q3)

          val payment = Payment(firstPeriod, 0, firstPeriod.paymentDeadline, PaymentStatus.Unpaid)

          when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
            Future.successful(
              Right(CurrentReturns(
                Seq(Return.fromPeriod(period, Next, inProgress = false, isOldest = false)))
              )
            )

          when(financialDataConnector.getFinancialData(any())(any())) thenReturn
            Future.successful(
              Right(CurrentPayments(Seq(payment), Seq.empty, Seq.empty, Seq.empty, payment.amountOwed, BigDecimal(0)))
            )

          when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), any())) thenReturn BigDecimal(1000)

          when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
          when(sessionRepository.set(any())) thenReturn Future.successful(true)
          when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
          when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
          when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
          when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(vatCustomerInfo))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
            .overrides(
              bind[ReturnStatusConnector].toInstance(returnStatusConnector),
              bind[FinancialDataConnector].toInstance(financialDataConnector),
              bind[VatReturnSalesService].toInstance(vatReturnSalesService),
              bind[UserAnswersRepository].toInstance(sessionRepository),
              bind[SaveForLaterConnector].toInstance(save4LaterConnector),
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[RegistrationConnector].toInstance(registrationConnector)
            ).build()

          running(application) {
            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
            val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

            val result = route(application, request).value

            val view = application.injector.instanceOf[IndexView]

            val config = application.injector.instanceOf[FrontendAppConfig]

            val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
              displayText = msgs("index.details.leaveThisService"),
              id = "leave-this-service",
              href = config.leaveOneStopShopUrl
            )

            status(result) mustEqual OK

            contentAsString(result) mustEqual view(
              registration.registeredCompanyName,
              registration.vrn.vrn,
              ReturnsViewModel(Seq(Return.fromPeriod(period, Next, inProgress = false, isOldest = false)), Seq.empty)(messages(application), clock),
              PaymentsViewModel(
                Seq(payment),
                Seq.empty,
                Seq.empty,
                hasDueReturnThreeYearsOld = false
              )(messages(application), clock, registrationRequest),
              paymentError = false,
              None,
              hasSubmittedFinalReturn = false,
              currentReturnIsFinal = false,
              config.amendRegistrationEnabled,
              amendRegistrationUrl,
              hasRequestedToLeave = false,
              Some(exclusionLinkView),
              hasDueReturnThreeYearsOld = false,
              hasDueReturnsLessThanThreeYearsOld = false,
              hasDeregisteredFromVat = false
            )(request, msgs).toString

          }
        }

        "and payment is outstanding and overdue" in {

          val instant = Instant.parse("2022-01-01T12:00:00Z")
          val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

          val firstPeriod = StandardPeriod(2021, Q1)
          val outstandingAmount = BigDecimal(1000)
          val overduePayment = Payment(firstPeriod, outstandingAmount, firstPeriod.paymentDeadline, PaymentStatus.Unpaid)

          when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
            Future.successful(
              Right(CurrentReturns(
                Seq(Return.fromPeriod(period, Next, inProgress = false, isOldest = false)))
              )
            )

          when(financialDataConnector.getFinancialData(any())(any())) thenReturn
            Future.successful(
              Right(
                CurrentPayments(
                  Seq.empty,
                  Seq(overduePayment),
                  Seq.empty,
                  Seq.empty,
                  overduePayment.amountOwed,
                  overduePayment.amountOwed
                )
              )
            )

          when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
          when(sessionRepository.set(any())) thenReturn Future.successful(true)
          when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
          when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
          when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
          when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(vatCustomerInfo))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
            .overrides(
              bind[ReturnStatusConnector].toInstance(returnStatusConnector),
              bind[FinancialDataConnector].toInstance(financialDataConnector),
              bind[UserAnswersRepository].toInstance(sessionRepository),
              bind[SaveForLaterConnector].toInstance(save4LaterConnector),
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[RegistrationConnector].toInstance(registrationConnector)
            ).build()

          running(application) {
            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
            val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

            val result = route(application, request).value

            val view = application.injector.instanceOf[IndexView]

            val config = application.injector.instanceOf[FrontendAppConfig]

            val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
              displayText = msgs("index.details.leaveThisService"),
              id = "leave-this-service",
              href = config.leaveOneStopShopUrl
            )

            status(result) mustEqual OK

            contentAsString(result) mustEqual view(
              registration.registeredCompanyName,
              registration.vrn.vrn,
              ReturnsViewModel(Seq(Return.fromPeriod(period, Next, inProgress = false, isOldest = false)), Seq.empty)(messages(application), clock),
              PaymentsViewModel(Seq.empty, Seq(overduePayment), Seq.empty, hasDueReturnThreeYearsOld = false )
              (messages(application), clock, registrationRequest),
              paymentError = false,
              None,
              hasSubmittedFinalReturn = false,
              currentReturnIsFinal = false,

              config.amendRegistrationEnabled,
              amendRegistrationUrl,
              hasRequestedToLeave = false,
              Some(exclusionLinkView),
              hasDueReturnThreeYearsOld = false,
              hasDueReturnsLessThanThreeYearsOld = false,
              hasDeregisteredFromVat = false
            )(request, msgs).toString
          }
        }

        "and payment errors" in {

          val instant = Instant.parse("2022-01-01T12:00:00Z")
          val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

          when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
            Future.successful(
              Right(CurrentReturns(
                Seq(Return.fromPeriod(period, Next, inProgress = false, isOldest = false)))
              )
            )

          when(financialDataConnector.getFinancialData(any())(any())) thenReturn
            Future.successful(
              Left(
                InvalidJson
              )
            )

          when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
          when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
          when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
          when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
          when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(vatCustomerInfo))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
            .overrides(
              bind[ReturnStatusConnector].toInstance(returnStatusConnector),
              bind[FinancialDataConnector].toInstance(financialDataConnector),
              bind[UserAnswersRepository].toInstance(sessionRepository),
              bind[SaveForLaterConnector].toInstance(save4LaterConnector),
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[RegistrationConnector].toInstance(registrationConnector)
            ).build()

          running(application) {
            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
            val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

            val result = route(application, request).value

            val view = application.injector.instanceOf[IndexView]

            val config = application.injector.instanceOf[FrontendAppConfig]

            val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
              displayText = msgs("index.details.leaveThisService"),
              id = "leave-this-service",
              href = config.leaveOneStopShopUrl
            )

            status(result) mustEqual OK

            contentAsString(result) mustEqual view(
              registration.registeredCompanyName,
              registration.vrn.vrn,
              ReturnsViewModel(Seq(Return.fromPeriod(period, Next, inProgress = false, isOldest = false)), Seq.empty)(messages(application), clock),
              PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
              paymentError = true,
              None,
              hasSubmittedFinalReturn = false,
              currentReturnIsFinal = false,
              config.amendRegistrationEnabled,
              amendRegistrationUrl,
              hasRequestedToLeave = false,
              Some(exclusionLinkView),
              hasDueReturnThreeYearsOld = false,
              hasDueReturnsLessThanThreeYearsOld = false,
              hasDeregisteredFromVat = false
            )(request, msgs).toString
          }
        }

        "and charge is not in ETMP" in {

          val clock: Clock = Clock.fixed(Instant.parse("2021-10-25T12:00:00Z"), ZoneId.systemDefault)
          val vatOwed = BigDecimal(1563.49)
          val firstPeriod = StandardPeriod(2021, Q3)
          val payment = Payment(firstPeriod, vatOwed, firstPeriod.paymentDeadline, PaymentStatus.Unknown)
          when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
            Future.successful(
              Right(CurrentReturns(
                Seq(Return.fromPeriod(period, Next, inProgress = false, isOldest = false)))
              )
            )

          when(financialDataConnector.getFinancialData(any())(any())) thenReturn
            Future.successful(Right(CurrentPayments(Seq(payment), Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

          when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), any())) thenReturn
            vatOwed

          when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
          when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
          when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
          when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
          when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(vatCustomerInfo))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
            .overrides(
              bind[ReturnStatusConnector].toInstance(returnStatusConnector),
              bind[FinancialDataConnector].toInstance(financialDataConnector),
              bind[VatReturnSalesService].toInstance(vatReturnSalesService),
              bind[UserAnswersRepository].toInstance(sessionRepository),
              bind[SaveForLaterConnector].toInstance(save4LaterConnector),
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[RegistrationConnector].toInstance(registrationConnector)
            ).build()

          running(application) {
            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
            val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

            val result = route(application, request).value

            val view = application.injector.instanceOf[IndexView]

            val config = application.injector.instanceOf[FrontendAppConfig]

            val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
              displayText = msgs("index.details.leaveThisService"),
              id = "leave-this-service",
              href = config.leaveOneStopShopUrl
            )

            status(result) mustEqual OK

            contentAsString(result) mustEqual view(
              registration.registeredCompanyName,
              registration.vrn.vrn,
              ReturnsViewModel(Seq(Return.fromPeriod(period, Next, inProgress = false, isOldest = false)), Seq.empty)(messages(application), clock),
              PaymentsViewModel(
                Seq(payment),
                Seq.empty,
                Seq.empty,
                hasDueReturnThreeYearsOld = false
              )(messages(application), clock, registrationRequest),
              paymentError = true,
              None,
              hasSubmittedFinalReturn = false,
              currentReturnIsFinal = false,

              config.amendRegistrationEnabled,
              amendRegistrationUrl,
              hasRequestedToLeave = false,
              Some(exclusionLinkView),
              hasDueReturnThreeYearsOld = false,
              hasDueReturnsLessThanThreeYearsOld = false,
              hasDeregisteredFromVat = false
            )(request, msgs).toString
          }
        }
      }

      "when there is 1 nil return completed and payment is outstanding from a correction" in {

        val instant = Instant.parse("2021-10-25T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val firstPeriod = StandardPeriod(2021, Q3)
        val payment = Payment(firstPeriod, 1000, firstPeriod.paymentDeadline, PaymentStatus.Unknown)
        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(Return.fromPeriod(period, Next, inProgress = false, isOldest = false)))
            )
          )

        when(financialDataConnector.getFinancialData(any())(any()))
          .thenReturn(Future.successful(Right(CurrentPayments(Seq(payment), Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0)))))

        when(vatReturnSalesService.getTotalVatOnSalesAfterCorrection(any(), any())) thenReturn BigDecimal(1000)

        when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
        when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(vatCustomerInfo))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[VatReturnSalesService].toInstance(vatReturnSalesService),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector),
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          ).build()

        running(application) {
          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
          val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
            displayText = msgs("index.details.leaveThisService"),
            id = "leave-this-service",
            href = config.leaveOneStopShopUrl
          )

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            ReturnsViewModel(Seq(Return.fromPeriod(period, Next, inProgress = false, isOldest = false)), Seq.empty)(messages(application), clock),
            PaymentsViewModel(
              Seq(payment),
              Seq.empty,
              Seq.empty,
              hasDueReturnThreeYearsOld = false
            )(messages(application), clock, registrationRequest),
            paymentError = true,
            None,
            hasSubmittedFinalReturn = false,
            currentReturnIsFinal = false,

            config.amendRegistrationEnabled,
            amendRegistrationUrl,
            hasRequestedToLeave = false,
            Some(exclusionLinkView),
            hasDueReturnThreeYearsOld = false,
            hasDueReturnsLessThanThreeYearsOld = false,
            hasDeregisteredFromVat = false
          )(request, msgs).toString
        }
      }

      "when a user has previously saved their return progress and their session has renewed" in {

        val instant = Instant.parse("2021-11-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val period = StandardPeriod(2021, Q3)
        val answers = arbitrarySavedUserAnswers.arbitrary.sample.value.copy(period = period)
        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(Return.fromPeriod(period, Overdue, inProgress = true, isOldest = true)))
            )
          )
        when(financialDataConnector.getFinancialData(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0)))
          )
        when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(Some(answers)))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
        when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(vatCustomerInfo))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(clock))
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector),
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          ).build()

        running(application) {
          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
          val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
            displayText = msgs("index.details.leaveThisService"),
            id = "leave-this-service",
            href = config.leaveOneStopShopUrl
          )

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            ReturnsViewModel(Seq(Return.fromPeriod(period, Overdue, inProgress = true, isOldest = true)), Seq.empty)(messages(application), clock),
            PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
            paymentError = false,
            None,
            hasSubmittedFinalReturn = false,
            currentReturnIsFinal = false,

            config.amendRegistrationEnabled,
            amendRegistrationUrl,
            hasRequestedToLeave = false,
            Some(exclusionLinkView),
            hasDueReturnThreeYearsOld = false,
            hasDueReturnsLessThanThreeYearsOld = false,
            hasDeregisteredFromVat = false
          )(request, messages(application)).toString

        }
      }
    }

    "must throw an exception when an error is returned from both connectors" in {

      when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
        Future.successful(Left(UnexpectedResponseStatus(1, "error")))

      when(financialDataConnector.getFinancialData(any())(any())) thenReturn
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

    "must throw an exception when an error is returned from the Return Status connector" in {

      when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
        Future.successful(Left(UnexpectedResponseStatus(1, "error")))

      when(financialDataConnector.getFinancialData(any())(any())) thenReturn
        Future.successful(
          Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

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

    "trader is excluded by HMRC" - {

      "has not submitted final return" in {

        val instant = Instant.parse("2021-10-11T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val nextPeriod = StandardPeriod(2021, Q4)

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(Return(
                nextPeriod,
                nextPeriod.firstDay,
                nextPeriod.lastDay,
                nextPeriod.paymentDeadline,
                SubmissionStatus.Next,
                inProgress = false,
                isOldest = false
              ))))
          )

        when(financialDataConnector.getFinancialData(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

        when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
        when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
        when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(vatCustomerInfo))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers),
          clock = Some(clock),
          registration = registration.copy(excludedTrader = excludedTraderHMRC)
        )
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector),
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          )
          .build()

        running(application) {

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
          val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            ReturnsViewModel(
              Seq(
                Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
              ), Seq.empty
            )(messages(application), clock),
            PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
            paymentError = false,
            excludedTraderHMRC,
            hasSubmittedFinalReturn = false,
            currentReturnIsFinal = false,
            config.amendRegistrationEnabled,
            amendRegistrationUrl,
            hasRequestedToLeave = false,
            None,
            hasDueReturnThreeYearsOld = false,
            hasDueReturnsLessThanThreeYearsOld = true,
            hasDeregisteredFromVat = false
          )(request, messages(application)).toString

          contentAsString(result).contains("leave-this-service") mustEqual false
        }
      }

      "has submitted final return" in {

        val instant = Instant.parse("2021-10-11T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val nextPeriod = StandardPeriod(2021, Q4)

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(Return(
                nextPeriod,
                nextPeriod.firstDay,
                nextPeriod.lastDay,
                nextPeriod.paymentDeadline,
                SubmissionStatus.Next,
                inProgress = false,
                isOldest = false
              ))))
          )

        when(financialDataConnector.getFinancialData(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

        when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
        when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(vatCustomerInfo))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers),
          clock = Some(clock),
          registration = registration.copy(excludedTrader = excludedTraderHMRC)
        )
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector),
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          )
          .build()

        running(application) {
          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
          val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
            displayText = msgs("index.details.rejoinService"),
            id = "rejoin-this-service",
            href = s"${config.rejoinThisService}"
          )

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            ReturnsViewModel(
              Seq(
                Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
              ), Seq.empty
            )(messages(application), clock),
            PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
            paymentError = false,
            excludedTraderHMRC,
            hasSubmittedFinalReturn = true,
            currentReturnIsFinal = false,
            config.amendRegistrationEnabled,
            amendRegistrationUrl,
            hasRequestedToLeave = false,
            Some(exclusionLinkView),
            hasDueReturnThreeYearsOld = false,
            hasDueReturnsLessThanThreeYearsOld = false,
            hasDeregisteredFromVat = false
          )(request, msgs).toString
          contentAsString(result).contains("leave-this-service") mustEqual false
        }
      }
    }

    "trader is excluded by self" - {

      "has not submitted final return" in {

        val instant = Instant.parse("2024-10-11T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val nextPeriod = StandardPeriod(2024, Q4)
        val deregisteredFromVat: VatCustomerInfo = vatCustomerInfo
          .copy(deregistrationDecisionDate = Some(LocalDate.now(clock)))

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(Return(
                nextPeriod,
                nextPeriod.firstDay,
                nextPeriod.lastDay,
                nextPeriod.paymentDeadline,
                SubmissionStatus.Next,
                inProgress = false,
                isOldest = false
              ))))
          )

        when(financialDataConnector.getFinancialData(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

        when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
        when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
        when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(deregisteredFromVat))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers),
          clock = Some(clock),
          registration = registration.copy(excludedTrader = excludedTraderSelf)
        )
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector),
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          )
          .build()

        running(application) {
          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
          val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            ReturnsViewModel(
              Seq(//
                Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
              ), Seq.empty
            )(messages(application), clock),
            PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
            paymentError = false,
            excludedTraderSelf,
            hasSubmittedFinalReturn = false,
            currentReturnIsFinal = false,
            config.amendRegistrationEnabled,
            amendRegistrationUrl,
            hasRequestedToLeave = false,
            None,
            hasDueReturnThreeYearsOld = false,
            hasDueReturnsLessThanThreeYearsOld = true,
            hasDeregisteredFromVat = true
          )(request, msgs).toString
          contentAsString(result).contains("leave-this-service") mustEqual false
        }
      }

      "has not submitted final return and has 3 year old returns" in {

        val instant = Instant.parse("2024-10-11T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val nextPeriod = StandardPeriod(2024, Q4)
        val excludedPeriod = StandardPeriod(2019, Q2)
        
        val deregisteredFromVat: VatCustomerInfo = vatCustomerInfo
          .copy(deregistrationDecisionDate = Some(LocalDate.now(clock))) 

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(
                Return(
                  nextPeriod,
                  nextPeriod.firstDay,
                  nextPeriod.lastDay,
                  nextPeriod.paymentDeadline,
                  SubmissionStatus.Next,
                  inProgress = false,
                  isOldest = false
                )
              ),
              excludedReturns = Seq(Return(
                excludedPeriod,
                excludedPeriod.firstDay,
                excludedPeriod.lastDay,
                excludedPeriod.paymentDeadline,
                SubmissionStatus.Excluded,
                inProgress = false,
                isOldest = false
              ))
            ))
          )

        when(financialDataConnector.getFinancialData(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

        when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
        when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
        when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(deregisteredFromVat))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers),
          clock = Some(clock),
          registration = registration.copy(excludedTrader = excludedTraderSelf)
        )
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector),
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          )
          .build()

        running(application) {

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
          val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            ReturnsViewModel(
              returns = Seq(
                Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false),
              ), excludedReturns =  Seq(
                Return.fromPeriod(excludedPeriod, Excluded, inProgress = false, isOldest = false)
              )
            )(messages(application), clock),
            PaymentsViewModel(Seq.empty, Seq.empty, excludedPayments = Seq.empty, hasDueReturnThreeYearsOld = true)
            (messages(application), clock, registrationRequest),
            paymentError = false,
            excludedTraderSelf,
            hasSubmittedFinalReturn = false,
            currentReturnIsFinal = false,
            config.amendRegistrationEnabled,
            amendRegistrationUrl,
            hasRequestedToLeave = false,
            None,
            hasDueReturnThreeYearsOld = true,
            hasDueReturnsLessThanThreeYearsOld = true,
            hasDeregisteredFromVat = true
          )(request, messages(application)).toString
          contentAsString(result).contains("leave-this-service") mustEqual false
        }
      }

      "has not submitted final return and has only 3 year old returns" in {

        val instant = Instant.parse("2024-10-11T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val excludedPeriod = StandardPeriod(2019, Q2)

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              returns = Seq.empty,
              excludedReturns = Seq(Return(
                excludedPeriod,
                excludedPeriod.firstDay,
                excludedPeriod.lastDay,
                excludedPeriod.paymentDeadline,
                SubmissionStatus.Excluded,
                inProgress = false,
                isOldest = false
              ))
            ))
          )

        when(financialDataConnector.getFinancialData(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

        when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
        when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
        when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Left(NotFound))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers),
          clock = Some(clock),
          registration = registration.copy(excludedTrader = excludedTraderSelf)
        )
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector),
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          )
          .build()

        running(application) {

          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
          val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustEqual OK

          val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
            displayText = msgs("index.details.rejoinService"),
            id = "rejoin-this-service",
            href = s"${config.rejoinThisService}"
          )

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            ReturnsViewModel(
              returns = Seq.empty,
              excludedReturns = Seq(
                Return.fromPeriod(excludedPeriod, Excluded, inProgress = false, isOldest = false)
              )
            )(messages(application), clock),
            PaymentsViewModel(Seq.empty, Seq.empty, excludedPayments = Seq.empty, hasDueReturnThreeYearsOld = true)
            (messages(application), clock, registrationRequest),
            paymentError = false,
            excludedTraderSelf,
            hasSubmittedFinalReturn = false,
            currentReturnIsFinal = false,
            config.amendRegistrationEnabled,
            amendRegistrationUrl,
            hasRequestedToLeave = false,
            Some(exclusionLinkView),
            hasDueReturnThreeYearsOld = true,
            hasDueReturnsLessThanThreeYearsOld = false,
            hasDeregisteredFromVat = false
          )(request, messages(application)).toString
          contentAsString(result).contains("leave-this-service") mustEqual false
        }
      }

      "has submitted final return" in {

        val instant = Instant.parse("2024-10-11T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val nextPeriod = StandardPeriod(2024, Q4)

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(Return(
                nextPeriod,
                nextPeriod.firstDay,
                nextPeriod.lastDay,
                nextPeriod.paymentDeadline,
                SubmissionStatus.Next,
                inProgress = false,
                isOldest = false
              ))))
          )

        when(financialDataConnector.getFinancialData(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

        when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
        when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Left(NotFound))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers),
          clock = Some(clock),
          registration = registration.copy(excludedTrader = excludedTraderSelf)
        )
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector),
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          )
          .build()

        running(application) {
          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
          val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
            displayText = msgs("index.details.rejoinService"),
            id = "rejoin-this-service",
            href = s"${config.rejoinThisService}"
          )

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            ReturnsViewModel(
              Seq(
                Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
              ), Seq.empty
            )(messages(application), clock),
            PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
            paymentError = false,
            excludedTraderSelf,
            hasSubmittedFinalReturn = true,
            currentReturnIsFinal = false,
            amendRegistrationLinkEnabled = config.amendRegistrationEnabled,
            amendRegistrationUrl,
            hasRequestedToLeave = false,
            Some(exclusionLinkView),
            hasDueReturnThreeYearsOld = false,
            hasDueReturnsLessThanThreeYearsOld = false,
            hasDeregisteredFromVat = false
          )(request, msgs).toString
          contentAsString(result).contains("leave-this-service") mustEqual false
        }
      }
    }

    "trader has requested to leave by self" - {

      "has not submitted final return" in {

        val instant = Instant.parse("2021-10-11T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val nextPeriod = StandardPeriod(2021, Q4)

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(Return(
                nextPeriod,
                nextPeriod.firstDay,
                nextPeriod.lastDay,
                nextPeriod.paymentDeadline,
                SubmissionStatus.Next,
                inProgress = false,
                isOldest = false
              ))))
          )

        when(financialDataConnector.getFinancialData(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

        when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
        when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
        when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(vatCustomerInfo))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers),
          clock = Some(clock),
          registration = registration.copy(excludedTrader = excludedTraderSelfRequestedToLeave)
        )
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector),
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          )
          .build()

        running(application) {
          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
          val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
            displayText = msgs("index.details.cancelRequestToLeave"),
            id = "cancel-request-to-leave",
            href = s"${config.leaveOneStopShopUrl}/cancel-leave-scheme"
          )

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            ReturnsViewModel(
              Seq(
                Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
              ), Seq.empty
            )(messages(application), clock),
            PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
            paymentError = false,
            excludedTraderSelfRequestedToLeave,
            hasSubmittedFinalReturn = false,
            currentReturnIsFinal = false,
            config.amendRegistrationEnabled,
            amendRegistrationUrl,
            hasRequestedToLeave = true,
            Some(exclusionLinkView),
            hasDueReturnThreeYearsOld = false,
            hasDueReturnsLessThanThreeYearsOld = true,
            hasDeregisteredFromVat = false
          )(request, msgs).toString
          contentAsString(result).contains("cancel-request-to-leave") mustEqual true
        }
      }

      "has submitted final return" in {

        val instant = Instant.parse("2021-10-11T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val nextPeriod = StandardPeriod(2021, Q4)

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(Return(
                nextPeriod,
                nextPeriod.firstDay,
                nextPeriod.lastDay,
                nextPeriod.paymentDeadline,
                SubmissionStatus.Next,
                inProgress = false,
                isOldest = false
              ))))
          )

        when(financialDataConnector.getFinancialData(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

        when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
        when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(vatCustomerInfo))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers),
          clock = Some(clock),
          registration = registration.copy(excludedTrader = excludedTraderSelfRequestedToLeave)
        )
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector),
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          )
          .build()

        running(application) {
          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
          val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
            displayText = msgs("index.details.cancelRequestToLeave"),
            id = "cancel-request-to-leave",
            href = s"${config.leaveOneStopShopUrl}/cancel-leave-scheme"
          )

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            ReturnsViewModel(
              Seq(
                Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
              ), Seq.empty
            )(messages(application), clock),
            PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
            paymentError = false,
            excludedTraderSelfRequestedToLeave,
            hasSubmittedFinalReturn = true,
            currentReturnIsFinal = false,
            config.amendRegistrationEnabled,
            amendRegistrationUrl,
            hasRequestedToLeave = true,
            Some(exclusionLinkView),
            hasDueReturnThreeYearsOld = false,
            hasDueReturnsLessThanThreeYearsOld = false,
            hasDeregisteredFromVat = false
          )(request, msgs).toString
          contentAsString(result).contains("cancel-request-to-leave") mustEqual true
        }
      }

      "and trader is transferring country" - {

        "and we allow them to cancel their leave request" - {

          "when the 10th day of the following month of the effective date is today" in {

            val today: LocalDate = LocalDate.of(2024, 5, 10)

            val finalReturnPeriod: Period = StandardPeriod(2024, Q2)

            val newClock = Clock.fixed(today.atStartOfDay(ZoneId.systemDefault()).toInstant, ZoneId.systemDefault())

            val submittedVatReturnsWithoutEffectivePeriod: Seq[VatReturn] =
              Gen.listOfN(4, arbitraryVatReturn.arbitrary.suchThat(vr => vr.period != finalReturnPeriod)).sample.value

            val excludedTraderSelfRequestedToLeaveTransferringMSID: Option[ExcludedTrader] = Some(ExcludedTrader(
              registration.vrn, ExclusionReason.TransferringMSID, LocalDate.of(2024, 4, 10), quarantined = false))

            val nextPeriod = StandardPeriod(2024, Q3)

            when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
              Future.successful(
                Right(CurrentReturns(
                  Seq(Return(
                    nextPeriod,
                    nextPeriod.firstDay,
                    nextPeriod.lastDay,
                    nextPeriod.paymentDeadline,
                    SubmissionStatus.Next,
                    inProgress = false,
                    isOldest = false
                  ))))
              )

            when(financialDataConnector.getFinancialData(any())(any())) thenReturn
              Future.successful(
                Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

            when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
            when(sessionRepository.set(any())) thenReturn Future.successful(true)
            when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
            when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
            when(vatReturnConnector.getSubmittedVatReturns()(any())) thenReturn submittedVatReturnsWithoutEffectivePeriod.toFuture
            when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
            when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Left(NotFound))

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers),
              clock = Some(newClock),
              registration = registration.copy(excludedTrader = excludedTraderSelfRequestedToLeaveTransferringMSID)
            )
              .overrides(
                bind[ReturnStatusConnector].toInstance(returnStatusConnector),
                bind[FinancialDataConnector].toInstance(financialDataConnector),
                bind[UserAnswersRepository].toInstance(sessionRepository),
                bind[SaveForLaterConnector].toInstance(save4LaterConnector),
                bind[VatReturnConnector].toInstance(vatReturnConnector),
                bind[RegistrationConnector].toInstance(registrationConnector)
              )
              .build()

            running(application) {
              implicit val msgs: Messages = messages(application)

              val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
              val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

              val result = route(application, request).value

              val view = application.injector.instanceOf[IndexView]

              val config = application.injector.instanceOf[FrontendAppConfig]

              val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
                displayText = msgs("index.details.cancelRequestToLeave"),
                id = "cancel-request-to-leave",
                href = s"${config.leaveOneStopShopUrl}/cancel-leave-scheme"
              )

              status(result) mustEqual OK

              contentAsString(result) mustEqual view(
                registration.registeredCompanyName,
                registration.vrn.vrn,
                ReturnsViewModel(
                  Seq(
                    Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
                  ), Seq.empty
                )(messages(application), clock),
                PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
                paymentError = false,
                excludedTraderSelfRequestedToLeaveTransferringMSID,
                hasSubmittedFinalReturn = false,
                currentReturnIsFinal = false,
                config.amendRegistrationEnabled,
                amendRegistrationUrl,
                hasRequestedToLeave = false,
                Some(exclusionLinkView),
                hasDueReturnThreeYearsOld = false,
                hasDueReturnsLessThanThreeYearsOld = true,
                hasDeregisteredFromVat = false
              )(request, msgs).toString
              contentAsString(result).contains("cancel-request-to-leave") mustEqual true
            }
          }

          "when the 9th day of the following month of the effective date is today" in {

            val today: LocalDate = LocalDate.of(2024, 5, 9)

            val finalReturnPeriod: Period = StandardPeriod(2024, Q2)

            val newClock = Clock.fixed(today.atStartOfDay(ZoneId.systemDefault()).toInstant, ZoneId.systemDefault())

            val submittedVatReturnsWithoutEffectivePeriod: Seq[VatReturn] =
              Gen.listOfN(4, arbitraryVatReturn.arbitrary.suchThat(vr => vr.period != finalReturnPeriod)).sample.value

            val excludedTraderSelfRequestedToLeaveTransferringMSID: Option[ExcludedTrader] = Some(ExcludedTrader(
              registration.vrn, ExclusionReason.TransferringMSID, LocalDate.of(2024, 4, 10), quarantined = false))

            val nextPeriod = StandardPeriod(2024, Q3)

            when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
              Future.successful(
                Right(CurrentReturns(
                  Seq(Return(
                    nextPeriod,
                    nextPeriod.firstDay,
                    nextPeriod.lastDay,
                    nextPeriod.paymentDeadline,
                    SubmissionStatus.Next,
                    inProgress = false,
                    isOldest = false
                  ))))
              )

            when(financialDataConnector.getFinancialData(any())(any())) thenReturn
              Future.successful(
                Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

            when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
            when(sessionRepository.set(any())) thenReturn Future.successful(true)
            when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
            when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
            when(vatReturnConnector.getSubmittedVatReturns()(any())) thenReturn submittedVatReturnsWithoutEffectivePeriod.toFuture
            when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
            when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Left(NotFound))

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers),
              clock = Some(newClock),
              registration = registration.copy(excludedTrader = excludedTraderSelfRequestedToLeaveTransferringMSID)
            )
              .overrides(
                bind[ReturnStatusConnector].toInstance(returnStatusConnector),
                bind[FinancialDataConnector].toInstance(financialDataConnector),
                bind[UserAnswersRepository].toInstance(sessionRepository),
                bind[SaveForLaterConnector].toInstance(save4LaterConnector),
                bind[VatReturnConnector].toInstance(vatReturnConnector),
                bind[RegistrationConnector].toInstance(registrationConnector)
              )
              .build()

            running(application) {
              implicit val msgs: Messages = messages(application)

              val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
              val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

              val result = route(application, request).value

              val view = application.injector.instanceOf[IndexView]

              val config = application.injector.instanceOf[FrontendAppConfig]

              val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
                displayText = msgs("index.details.cancelRequestToLeave"),
                id = "cancel-request-to-leave",
                href = s"${config.leaveOneStopShopUrl}/cancel-leave-scheme"
              )

              status(result) mustEqual OK

              contentAsString(result) mustEqual view(
                registration.registeredCompanyName,
                registration.vrn.vrn,
                ReturnsViewModel(
                  Seq(
                    Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
                  ), Seq.empty
                )(messages(application), clock),
                PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
                paymentError = false,
                excludedTraderSelfRequestedToLeaveTransferringMSID,
                hasSubmittedFinalReturn = false,
                currentReturnIsFinal = false,
                config.amendRegistrationEnabled,
                amendRegistrationUrl,
                hasRequestedToLeave = false,
                Some(exclusionLinkView),
                hasDueReturnThreeYearsOld = false,
                hasDueReturnsLessThanThreeYearsOld = true,
                hasDeregisteredFromVat = false
              )(request, msgs).toString
              contentAsString(result).contains("cancel-request-to-leave") mustEqual true
            }
          }
        }

        "and we do not allow them to cancel their leave request" - {

          "when the 11th day of the following month of the effective date is today" in {

            val today: LocalDate = LocalDate.of(2024, 5, 11)

            val finalReturnPeriod: Period = StandardPeriod(2024, Q2)

            val newClock = Clock.fixed(today.atStartOfDay(ZoneId.systemDefault()).toInstant, ZoneId.systemDefault())

            val submittedVatReturnsWithoutEffectivePeriod: Seq[VatReturn] =
              Gen.listOfN(4, arbitraryVatReturn.arbitrary.suchThat(vr => vr.period != finalReturnPeriod)).sample.value

            val excludedTraderSelfRequestedToLeaveTransferringMSID: Option[ExcludedTrader] = Some(ExcludedTrader(
              registration.vrn, ExclusionReason.TransferringMSID, LocalDate.of(2024, 4, 10), quarantined = false))

            val nextPeriod = StandardPeriod(2024, Q3)

            when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
              Future.successful(
                Right(CurrentReturns(
                  Seq(Return(
                    nextPeriod,
                    nextPeriod.firstDay,
                    nextPeriod.lastDay,
                    nextPeriod.paymentDeadline,
                    SubmissionStatus.Next,
                    inProgress = false,
                    isOldest = false
                  ))))
              )

            when(financialDataConnector.getFinancialData(any())(any())) thenReturn
              Future.successful(
                Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

            when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
            when(sessionRepository.set(any())) thenReturn Future.successful(true)
            when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
            when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
            when(vatReturnConnector.getSubmittedVatReturns()(any())) thenReturn submittedVatReturnsWithoutEffectivePeriod.toFuture
            when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
            when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Left(NotFound))

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers),
              clock = Some(newClock),
              registration = registration.copy(excludedTrader = excludedTraderSelfRequestedToLeaveTransferringMSID)
            )
              .overrides(
                bind[ReturnStatusConnector].toInstance(returnStatusConnector),
                bind[FinancialDataConnector].toInstance(financialDataConnector),
                bind[UserAnswersRepository].toInstance(sessionRepository),
                bind[SaveForLaterConnector].toInstance(save4LaterConnector),
                bind[VatReturnConnector].toInstance(vatReturnConnector),
                bind[RegistrationConnector].toInstance(registrationConnector)
              )
              .build()

            running(application) {
              implicit val msgs: Messages = messages(application)

              val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
              val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

              val result = route(application, request).value

              val view = application.injector.instanceOf[IndexView]

              val config = application.injector.instanceOf[FrontendAppConfig]

              status(result) mustEqual OK

              contentAsString(result) mustEqual view(
                registration.registeredCompanyName,
                registration.vrn.vrn,
                ReturnsViewModel(
                  Seq(
                    Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
                  ), Seq.empty
                )(messages(application), clock),
                PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
                paymentError = false,
                excludedTraderSelfRequestedToLeaveTransferringMSID,
                hasSubmittedFinalReturn = false,
                currentReturnIsFinal = false,
                amendRegistrationLinkEnabled = config.amendRegistrationEnabled,
                amendRegistrationUrl,
                hasRequestedToLeave = false,
                None,
                hasDueReturnThreeYearsOld = false,
                hasDueReturnsLessThanThreeYearsOld = true,
                hasDeregisteredFromVat = false
              )(request, msgs).toString
              contentAsString(result).contains("cancel-request-to-leave") mustEqual false
            }
          }
        }

        "has not submitted final return" in {

          val today: LocalDate = LocalDate.of(2024, 5, 28)

          val finalReturnPeriod: Period = StandardPeriod(2024, Q2)

          val newClock = Clock.fixed(today.atStartOfDay(ZoneId.systemDefault()).toInstant, ZoneId.systemDefault())

          val submittedVatReturnsWithoutEffectivePeriod: Seq[VatReturn] =
            Gen.listOfN(4, arbitraryVatReturn.arbitrary.suchThat(vr => vr.period != finalReturnPeriod)).sample.value

          val excludedTraderSelfRequestedToLeaveTransferringMSID: Option[ExcludedTrader] = Some(ExcludedTrader(
            registration.vrn, ExclusionReason.TransferringMSID, LocalDate.of(2024, 6, 9), quarantined = false))

          val nextPeriod = StandardPeriod(2024, Q3)

          when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
            Future.successful(
              Right(CurrentReturns(
                Seq(Return(
                  nextPeriod,
                  nextPeriod.firstDay,
                  nextPeriod.lastDay,
                  nextPeriod.paymentDeadline,
                  SubmissionStatus.Next,
                  inProgress = false,
                  isOldest = false
                ))))
            )

          when(financialDataConnector.getFinancialData(any())(any())) thenReturn Future.
              successful(Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

          when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
          when(sessionRepository.set(any())) thenReturn Future.successful(true)
          when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
          when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
          when(vatReturnConnector.getSubmittedVatReturns()(any())) thenReturn submittedVatReturnsWithoutEffectivePeriod.toFuture
          when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
          when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Left(NotFound))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers),
            clock = Some(newClock),
            registration = registration.copy(excludedTrader = excludedTraderSelfRequestedToLeaveTransferringMSID)
          )
            .overrides(
              bind[ReturnStatusConnector].toInstance(returnStatusConnector),
              bind[FinancialDataConnector].toInstance(financialDataConnector),
              bind[UserAnswersRepository].toInstance(sessionRepository),
              bind[SaveForLaterConnector].toInstance(save4LaterConnector),
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[RegistrationConnector].toInstance(registrationConnector)
            )
            .build()

          running(application) {
            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
            val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

            val result = route(application, request).value

            val view = application.injector.instanceOf[IndexView]

            val config = application.injector.instanceOf[FrontendAppConfig]

            val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
              displayText = msgs("index.details.cancelRequestToLeave"),
              id = "cancel-request-to-leave",
              href = s"${config.leaveOneStopShopUrl}/cancel-leave-scheme"
            )

            status(result) mustEqual OK

            contentAsString(result) mustEqual view(
              registration.registeredCompanyName,
              registration.vrn.vrn,
              ReturnsViewModel(
                Seq(
                  Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
                ), Seq.empty
              )(messages(application), clock),
              PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
              paymentError = false,
              excludedTraderSelfRequestedToLeaveTransferringMSID,
              hasSubmittedFinalReturn = false,
              currentReturnIsFinal = false,
              config.amendRegistrationEnabled,
              amendRegistrationUrl,
              hasRequestedToLeave = true,
              Some(exclusionLinkView),
              hasDueReturnThreeYearsOld = false,
              hasDueReturnsLessThanThreeYearsOld = true,
              hasDeregisteredFromVat = false
            )(request, msgs).toString
            contentAsString(result).contains("cancel-request-to-leave") mustEqual true
          }
        }

        "has submitted final return" in {

          val today: LocalDate = LocalDate.of(2024, 5, 28)

          val finalReturnPeriod: Period = StandardPeriod(2024, Q2)

          val newClock = Clock.fixed(today.atStartOfDay(ZoneId.systemDefault()).toInstant, ZoneId.systemDefault())

          val finalVatReturn: VatReturn = arbitraryVatReturn.arbitrary.sample.value.copy(period = finalReturnPeriod)
          val submittedVatReturnsWithoutEffectivePeriod: Seq[VatReturn] =
            Gen.listOfN(4, arbitraryVatReturn.arbitrary).sample.value ++ Seq(finalVatReturn)

          val excludedTraderSelfRequestedToLeaveTransferringMSID: Option[ExcludedTrader] = Some(ExcludedTrader(
            registration.vrn, ExclusionReason.TransferringMSID, LocalDate.of(2024, 6, 9), quarantined = false))

          val nextPeriod = StandardPeriod(2024, Q3)

          when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
            Future.successful(
              Right(CurrentReturns(
                Seq(Return(
                  nextPeriod,
                  nextPeriod.firstDay,
                  nextPeriod.lastDay,
                  nextPeriod.paymentDeadline,
                  SubmissionStatus.Next,
                  inProgress = false,
                  isOldest = false
                ))))
            )

          when(financialDataConnector.getFinancialData(any())(any())) thenReturn
            Future.successful(
              Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

          when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
          when(sessionRepository.set(any())) thenReturn Future.successful(true)
          when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
          when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(finalVatReturn))
          when(vatReturnConnector.getSubmittedVatReturns()(any())) thenReturn submittedVatReturnsWithoutEffectivePeriod.toFuture
          when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
          when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Left(NotFound))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers),
            clock = Some(newClock),
            registration = registration.copy(excludedTrader = excludedTraderSelfRequestedToLeaveTransferringMSID)
          )
            .overrides(
              bind[ReturnStatusConnector].toInstance(returnStatusConnector),
              bind[FinancialDataConnector].toInstance(financialDataConnector),
              bind[UserAnswersRepository].toInstance(sessionRepository),
              bind[SaveForLaterConnector].toInstance(save4LaterConnector),
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[RegistrationConnector].toInstance(registrationConnector)
            )
            .build()

          running(application) {
            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
            val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

            val result = route(application, request).value

            val view = application.injector.instanceOf[IndexView]

            val config = application.injector.instanceOf[FrontendAppConfig]

            val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
              displayText = msgs("index.details.cancelRequestToLeave"),
              id = "cancel-request-to-leave",
              href = s"${config.leaveOneStopShopUrl}/cancel-leave-scheme"
            )

            status(result) mustEqual OK

            contentAsString(result) mustEqual view(
              registration.registeredCompanyName,
              registration.vrn.vrn,
              ReturnsViewModel(
                Seq(
                  Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
                ), Seq.empty
              )(messages(application), clock),
              PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
              paymentError = false,
              excludedTraderSelfRequestedToLeaveTransferringMSID,
              hasSubmittedFinalReturn = true,
              currentReturnIsFinal = true,
              config.amendRegistrationEnabled,
              amendRegistrationUrl,
              hasRequestedToLeave = true,
              Some(exclusionLinkView),
              hasDueReturnThreeYearsOld = false,
              hasDueReturnsLessThanThreeYearsOld = false,
              hasDeregisteredFromVat = false
            )(request, msgs).toString
            contentAsString(result).contains("cancel-request-to-leave") mustEqual true
          }
        }
      }
    }

    "trader is excluded and quarantined" - {

      "has not submitted final return" in {

        val instant = Instant.parse("2021-10-11T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val nextPeriod = StandardPeriod(2021, Q4)

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(Return(
                nextPeriod,
                nextPeriod.firstDay,
                nextPeriod.lastDay,
                nextPeriod.paymentDeadline,
                SubmissionStatus.Next,
                inProgress = false,
                isOldest = false
              ))))
          )

        when(financialDataConnector.getFinancialData(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

        when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
        when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
        when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Right(vatCustomerInfo))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers),
          clock = Some(clock),
          registration = registration.copy(excludedTrader = excludedTraderQuarantined)
        )
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector),
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
          val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            ReturnsViewModel(
              Seq(
                Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
              ), Seq.empty
            )(messages(application), clock),
            PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
            paymentError = false,
            excludedTraderQuarantined,
            hasSubmittedFinalReturn = false,
            currentReturnIsFinal = false,
            config.amendRegistrationEnabled,
            amendRegistrationUrl,
            hasRequestedToLeave = false,
            None,
            hasDueReturnThreeYearsOld = false,
            hasDueReturnsLessThanThreeYearsOld = true,
            hasDeregisteredFromVat = false
          )(request, messages(application)).toString
          contentAsString(result).contains("leave-this-service") mustEqual false
        }
      }

      "has submitted final return and eligible to rejoin" in {

        val instant = Instant.parse("2024-04-01T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val nextPeriod = StandardPeriod(2021, Q3)

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(Return(
                nextPeriod,
                nextPeriod.firstDay,
                nextPeriod.lastDay,
                nextPeriod.paymentDeadline,
                SubmissionStatus.Next,
                inProgress = false,
                isOldest = false
              ))))
          )

        when(financialDataConnector.getFinancialData(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

        when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
        when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Left(NotFound))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers),
          clock = Some(clock),
          registration = registration.copy(excludedTrader = excludedTraderQuarantined)
        )
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector),
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          )
          .build()

        running(application) {
          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
          val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          val exclusionLinkView: ExclusionLinkView = ExclusionLinkView(
            displayText = msgs("index.details.rejoinService"),
            id = "rejoin-this-service",
            href = s"${config.rejoinThisService}"
          )

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            ReturnsViewModel(
              Seq(
                Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
              ), Seq.empty
            )(messages(application), clock),
            PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
            paymentError = false,
            excludedTraderQuarantined,
            hasSubmittedFinalReturn = true,
            currentReturnIsFinal = false,
            config.amendRegistrationEnabled,
            amendRegistrationUrl,
            hasRequestedToLeave = false,
            Some(exclusionLinkView),
            hasDueReturnThreeYearsOld = false,
            hasDueReturnsLessThanThreeYearsOld = false,
            hasDeregisteredFromVat = false
          )(request, msgs).toString
          contentAsString(result).contains("leave-this-service") mustEqual false
        }
      }

      "has submitted final return and not eligible to rejoin when today is before rejoin date" in {

        val instant = Instant.parse("2024-03-31T12:00:00Z")
        val clock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

        val nextPeriod = StandardPeriod(2021, Q4)

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(Return(
                nextPeriod,
                nextPeriod.firstDay,
                nextPeriod.lastDay,
                nextPeriod.paymentDeadline,
                SubmissionStatus.Next,
                inProgress = false,
                isOldest = false
              ))))
          )

        when(financialDataConnector.getFinancialData(any())(any())) thenReturn
          Future.successful(
            Right(CurrentPayments(Seq.empty, Seq.empty, Seq.empty, Seq.empty, BigDecimal(0), BigDecimal(0))))

        when(sessionRepository.get(any())) thenReturn Future.successful(Seq())
        when(sessionRepository.set(any())) thenReturn Future.successful(true)
        when(save4LaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(registrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
        when(registrationConnector.getVatCustomerInfo()(any())) thenReturn Future.successful(Left(NotFound))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers),
          clock = Some(clock),
          registration = registration.copy(excludedTrader = excludedTraderQuarantined)
        )
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[FinancialDataConnector].toInstance(financialDataConnector),
            bind[UserAnswersRepository].toInstance(sessionRepository),
            bind[SaveForLaterConnector].toInstance(save4LaterConnector),
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          )
          .build()

        running(application) {
          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)
          val registrationRequest = RegistrationRequest(request, credentials = testCredentials, vrn = vrn, registration = registration)

          val result = route(application, request).value

          val view = application.injector.instanceOf[IndexView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(
            registration.registeredCompanyName,
            registration.vrn.vrn,
            ReturnsViewModel(
              Seq(
                Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
              ), Seq.empty
            )(messages(application), clock),
            PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, hasDueReturnThreeYearsOld = false)(messages(application), clock, registrationRequest),
            paymentError = false,
            excludedTraderQuarantined,
            hasSubmittedFinalReturn = true,
            currentReturnIsFinal = false,
            config.amendRegistrationEnabled,
            amendRegistrationUrl,
            hasRequestedToLeave = false,
            None,
            hasDueReturnThreeYearsOld = false,
            hasDueReturnsLessThanThreeYearsOld = false,
            hasDeregisteredFromVat = false
          )(request, msgs).toString
          contentAsString(result).contains("leave-this-service") mustEqual false
        }
      }
    }

    "when part of vat group is true and has fixed establishment" in {

      val newRegistration: Registration = Registration(
        vrn = vrn,
        registeredCompanyName = arbitrary[String].sample.value,
        vatDetails = VatDetails(LocalDate.of(2000, 1, 1), address, partOfVatGroup = true, VatDetailSource.Mixed),
        euRegistrations = Seq(RegistrationWithFixedEstablishment(
          Country("ES", "Spain"),
          EuTaxIdentifier(EuTaxIdentifierType.Vat, "ES123456789"),
          TradeDetails("Spanish trading name", InternationalAddress("Line 1", None, "Town", None, None, Country("ES", "Spain")))
        )),
        contactDetails = ContactDetails("name", "0123 456789", "email@example.com"),
        commencementDate = LocalDate.now,
        isOnlineMarketplace = false,
        None,
        None
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = None, registration = newRegistration)
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.YourAccountController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

      }
    }
  }
}