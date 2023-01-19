/*
 * Copyright 2023 HM Revenue & Customs
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
import cats.data.NonEmptyChain
import cats.data.Validated.{Invalid, Valid}
import connectors.corrections.CorrectionConnector
import connectors.{SaveForLaterConnector, SavedUserAnswers, VatReturnConnector}
import controllers.corrections.{routes => correctionsRoutes}
import models.audit.{ReturnForDataEntryAuditModel, ReturnsAuditModel, SubmissionResult}
import models.corrections.CorrectionPayload
import models.domain.VatReturn
import models.emails.EmailSendingResult.EMAIL_ACCEPTED
import models.requests.{DataRequest, SaveForLaterRequest, VatReturnRequest, VatReturnWithCorrectionRequest}
import models.responses.{ConflictFound, ReceivedErrorFromCore, RegistrationNotFound, UnexpectedResponseStatus}
import models.{CheckMode, Country, DataMissingError, Index, NormalMode, Period, TotalVatToCountry, VatRate, VatRateType}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito
import org.mockito.Mockito.{doNothing, times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages._
import pages.corrections.{CorrectPreviousReturnPage, CorrectionCountryPage, CorrectionReturnPeriodPage, CountryVatCorrectionPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.EmailConfirmationQuery
import repositories.{CachedVatReturnRepository, UserAnswersRepository}
import services.corrections.CorrectionService
import services.{AuditService, EmailService, SalesAtVatRateService, VatReturnService}
import viewmodels.govuk.SummaryListFluency

import java.time.LocalDate
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with MockitoSugar with SummaryListFluency with BeforeAndAfterEach {

  private val vatReturnConnector = mock[VatReturnConnector]
  private val correctionConnector = mock[CorrectionConnector]
  private val vatReturnService = mock[VatReturnService]
  private val correctionService = mock[CorrectionService]
  private val auditService = mock[AuditService]
  private val salesAtVatRateService = mock[SalesAtVatRateService]
  private val emailService =  mock[EmailService]
  private val s4lConnector = mock[SaveForLaterConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(vatReturnConnector, correctionConnector, vatReturnService, auditService, salesAtVatRateService, emailService, s4lConnector)
    super.beforeEach()
  }

  "Check Your Answers Controller" - {

    "when correct previous return is false / empty" - {
      "must return OK and the correct view for a GET" in {
        val answers = completeUserAnswers

        val application = applicationBuilder(userAnswers = Some(answers))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(period).url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result).contains("Business name") mustBe true
          contentAsString(result).contains(registration.registeredCompanyName) mustBe true
          contentAsString(result).contains(registration.vrn.vrn) mustBe true
          contentAsString(result).contains("Sales from Northern Ireland to EU countries") mustBe true
          contentAsString(result).contains("Sales from EU countries to other EU countries") mustBe true
          contentAsString(result).contains("VAT owed to EU countries") mustBe true
          contentAsString(result).contains("VAT declared to EU countries after corrections") mustBe false
          contentAsString(result).contains("VAT declared where no payment is due") mustBe false
          contentAsString(result).contains("Corrections") mustBe false
        }
      }

      "must return OK and the correct view for a GET when the correction choice was NO " in {
        val application = applicationBuilder(userAnswers = Some(completeUserAnswers.set(CorrectPreviousReturnPage, false).success.value))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(period).url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result).contains("Business name") mustBe true
          contentAsString(result).contains(registration.registeredCompanyName) mustBe true
          contentAsString(result).contains(registration.vrn.vrn) mustBe true
          contentAsString(result).contains("Sales from Northern Ireland to EU countries") mustBe true
          contentAsString(result).contains("Sales from EU countries to other EU countries") mustBe true
          contentAsString(result).contains("VAT owed to EU countries") mustBe true
          contentAsString(result).contains("Corrections") mustBe true
          contentAsString(result).contains("VAT declared where no payment is due") mustBe false
          contentAsString(result).contains("VAT declared to EU countries after corrections") mustBe false
        }
      }

    }

    "when correct previous return is true" - {

      "must contain VAT declared to EU countries after corrections heading if there were corrections and all totals are positive" in {
        val answers = completeUserAnswersWithCorrections

        val application = applicationBuilder(userAnswers = Some(answers))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(period).url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result).contains("Business name") mustBe true
          contentAsString(result).contains(registration.registeredCompanyName) mustBe true
          contentAsString(result).contains(registration.vrn.vrn) mustBe true
          contentAsString(result).contains("Sales from Northern Ireland to EU countries") mustBe true
          contentAsString(result).contains("Sales from EU countries to other EU countries") mustBe true
          contentAsString(result).contains("VAT declared to EU countries after corrections") mustBe true
          contentAsString(result).contains("Corrections") mustBe true
          contentAsString(result).contains("VAT declared where no payment is due") mustBe false
          contentAsString(result).contains("VAT owed to EU countries") mustBe false
        }
      }

      "must contain VAT declared where no payment is due heading if there were negative totals after corrections" in {
        val answers = completeUserAnswersWithCorrections
          .set(CorrectPreviousReturnPage, true).success.value
          .set(CorrectionReturnPeriodPage(index), period).success.value
          .set(CorrectionCountryPage(index, index), Country("EE", "Estonia")).success.value
          .set(CountryVatCorrectionPage(index, index), BigDecimal(-1000)).success.value

        val application = applicationBuilder(userAnswers = Some(answers))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(period).url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result).contains("Business name") mustBe true
          contentAsString(result).contains(registration.registeredCompanyName) mustBe true
          contentAsString(result).contains(registration.vrn.vrn) mustBe true
          contentAsString(result).contains("Sales from Northern Ireland to EU countries") mustBe true
          contentAsString(result).contains("Sales from EU countries to other EU countries") mustBe true
          contentAsString(result).contains("VAT declared to EU countries after corrections") mustBe true
          contentAsString(result).contains("VAT declared where no payment is due") mustBe true
          contentAsString(result).contains("Corrections") mustBe true
          contentAsString(result).contains("VAT owed to EU countries") mustBe false
        }
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(period).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  "on submit" - {

    val vatReturn = arbitrary[VatReturn].sample.value
    val correctionPayload = arbitrary[CorrectionPayload].sample.value

    "when the user answered all necessary data and submission of the return succeeds" - {

      "and correct previous return is false / empty" - {

        "must redirect to the next page" in {

          val answers =
            emptyUserAnswers
              .set(SoldGoodsFromNiPage, false).success.value
              .set(SoldGoodsFromEuPage, false).success.value

          when(vatReturnConnector.submit(any[VatReturnRequest]())(any())) thenReturn Future.successful(Right(vatReturn))
          when(emailService.sendConfirmationEmail(any(), any(), any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(EMAIL_ACCEPTED))
          when(s4lConnector.delete(any())(any())) thenReturn Future.successful(Right(true))


          val app =
            applicationBuilder(Some(answers))
              .overrides(
                bind[VatReturnConnector].toInstance(vatReturnConnector),
                bind[CorrectionConnector].toInstance(correctionConnector),
                bind[EmailService].toInstance(emailService),
                bind[SaveForLaterConnector].toInstance(s4lConnector)
              )
              .build()

          running(app) {
            val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, false).url)
            val result = route(app, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.ReturnSubmittedController.onPageLoad(period).url
            verify(vatReturnConnector, times(1)).submit(any[VatReturnRequest]())(any())
          }
        }

        "must audit the event and redirect to the next page and successfully send email confirmation" in {
          val mockSessionRepository = mock[UserAnswersRepository]

          val answers = completeUserAnswers

          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
          when(vatReturnService.fromUserAnswers(any(), any(), any(), any())) thenReturn Valid(vatReturnRequest)
          when(vatReturnConnector.submit(any[VatReturnRequest]())(any())) thenReturn Future.successful(Right(vatReturn))
          when(emailService.sendConfirmationEmail(any(), any(), any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(EMAIL_ACCEPTED))
          when(s4lConnector.delete(any())(any())) thenReturn Future.successful(Right(true))

          val totalVatOnSales = BigDecimal(100)
          when(salesAtVatRateService.getTotalVatOwedAfterCorrections(any())) thenReturn totalVatOnSales
          doNothing().when(auditService).audit(any())(any(), any())

          val application = applicationBuilder(userAnswers = Some(answers))
            .overrides(
              bind[VatReturnService].toInstance(vatReturnService),
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[CorrectionConnector].toInstance(correctionConnector),
              bind[SalesAtVatRateService].toInstance(salesAtVatRateService),
              bind[AuditService].toInstance(auditService),
              bind[EmailService].toInstance(emailService),
              bind[UserAnswersRepository].toInstance(mockSessionRepository),
              bind[SaveForLaterConnector].toInstance(s4lConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(vatReturnRequest.period, false).url)
            val result = route(application, request).value
            val dataRequest = DataRequest(request, testCredentials, vrn, registration, answers)
            val expectedAuditEvent = ReturnsAuditModel.build(
              vatReturnRequest, None, SubmissionResult.Success, Some(vatReturn.reference), Some(vatReturn.paymentReference), dataRequest
            )
            val expectedAuditEventForDataEntry = ReturnForDataEntryAuditModel(vatReturnRequest, None, vatReturn.reference, vatReturn.paymentReference)

            val userAnswersWithEmailConfirmation = completeUserAnswers.copy().set(EmailConfirmationQuery, true).success.value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual CheckYourAnswersPage.navigate(NormalMode, userAnswersWithEmailConfirmation).url

            verify(auditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
            verify(auditService, times(1)).audit(eqTo(expectedAuditEventForDataEntry))(any(), any())
            verify(emailService, times(1))
              .sendConfirmationEmail(eqTo(registration.contactDetails.fullName),
                eqTo(registration.registeredCompanyName),
                eqTo(registration.contactDetails.emailAddress),
                eqTo(totalVatOnSales),
                eqTo(vatReturnRequest.period)
              )(any(), any())
            verify(mockSessionRepository, times(1)).set(eqTo(userAnswersWithEmailConfirmation))
          }
        }
      }

      "and correct previous return is true" - {

        "must redirect to the next page" in {

          val answers =
            completeUserAnswersWithCorrections
              .set(SoldGoodsFromNiPage, false).success.value
              .set(SoldGoodsFromEuPage, false).success.value

          when(vatReturnConnector.submitWithCorrections(any[VatReturnWithCorrectionRequest]())(any()))
            .thenReturn(Future.successful(Right((vatReturn, correctionPayload))))
          when(emailService.sendConfirmationEmail(any(), any(), any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(EMAIL_ACCEPTED))
          when(s4lConnector.delete(any())(any())) thenReturn Future.successful(Right(true))

          val app =
            applicationBuilder(Some(answers))
              .overrides(
                bind[VatReturnConnector].toInstance(vatReturnConnector),
                bind[CorrectionConnector].toInstance(correctionConnector),
                bind[EmailService].toInstance(emailService),
                bind[SaveForLaterConnector].toInstance(s4lConnector)
              ).build()

          running(app) {
            val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, false).url)
            val result = route(app, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.ReturnSubmittedController.onPageLoad(period).url
            verify(vatReturnConnector, times(1)).submitWithCorrections(any[VatReturnWithCorrectionRequest]())(any())
          }
        }

        "must audit the event and redirect to the next page and successfully send email confirmation" in {
          val mockSessionRepository = mock[UserAnswersRepository]

          val answers =
            completeUserAnswersWithCorrections

          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
          when(vatReturnService.fromUserAnswers(any(), any(), any(), any())) thenReturn Valid(vatReturnRequest)
          when(correctionService.fromUserAnswers(any(), any(), any(), any())) thenReturn Valid(correctionRequest)
          when(vatReturnConnector.submitWithCorrections(any[VatReturnWithCorrectionRequest]())(any()))
            .thenReturn(Future.successful(Right((vatReturn, correctionPayload))))
          when(emailService.sendConfirmationEmail(any(), any(), any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(EMAIL_ACCEPTED))
          when(s4lConnector.delete(any())(any())) thenReturn Future.successful(Right(true))

          val totalVatOnSales = BigDecimal(100)
          when(salesAtVatRateService.getTotalVatOwedAfterCorrections(any())) thenReturn totalVatOnSales
          doNothing().when(auditService).audit(any())(any(), any())

          val application = applicationBuilder(userAnswers = Some(answers))
            .overrides(
              bind[VatReturnService].toInstance(vatReturnService),
              bind[CorrectionService].toInstance(correctionService),
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[CorrectionConnector].toInstance(correctionConnector),
              bind[SalesAtVatRateService].toInstance(salesAtVatRateService),
              bind[AuditService].toInstance(auditService),
              bind[EmailService].toInstance(emailService),
              bind[UserAnswersRepository].toInstance(mockSessionRepository),
              bind[SaveForLaterConnector].toInstance(s4lConnector)
            )
            .build()

          running(application) {
            val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(vatReturnRequest.period, false).url)
            val result = route(application, request).value
            val dataRequest = DataRequest(request, testCredentials, vrn, registration, answers)
            val expectedAuditEvent = ReturnsAuditModel.build(
              vatReturnRequest, Some(correctionRequest), SubmissionResult.Success, Some(vatReturn.reference), Some(vatReturn.paymentReference), dataRequest
            )
            val expectedAuditEventForDataEntry = ReturnForDataEntryAuditModel(vatReturnRequest, Some(correctionRequest), vatReturn.reference, vatReturn.paymentReference)

            val userAnswersWithEmailConfirmation = answers.set(EmailConfirmationQuery, true).success.value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual CheckYourAnswersPage.navigate(NormalMode, userAnswersWithEmailConfirmation).url

            verify(auditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
            verify(auditService, times(1)).audit(eqTo(expectedAuditEventForDataEntry))(any(), any())
            verify(emailService, times(1))
              .sendConfirmationEmail(eqTo(registration.contactDetails.fullName),
                eqTo(registration.registeredCompanyName),
                eqTo(registration.contactDetails.emailAddress),
                eqTo(totalVatOnSales),
                eqTo(vatReturnRequest.period)
              )(any(), any())
            verify(mockSessionRepository, times(1)).set(eqTo(userAnswersWithEmailConfirmation))
          }
        }
      }
    }

    "when the user has already submitted a return for this period" - {

      "must redirect to Index" in {

        val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value
            .set(SoldGoodsFromEuPage, false).success.value

        val app =
          applicationBuilder(Some(answers))
            .overrides(
              bind[VatReturnService].toInstance(vatReturnService),
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[AuditService].toInstance(auditService)
            ).build()

        when(vatReturnService.fromUserAnswers(any(), any(), any(), any())) thenReturn Valid(vatReturnRequest)
        when(vatReturnConnector.submit(any[VatReturnRequest]())(any())) thenReturn Future.successful(Left(ConflictFound))
        doNothing().when(auditService).audit(any())(any(), any())

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, incompletePromptShown = false).url)
          val result = route(app, request).value
          val dataRequest = DataRequest(request, testCredentials, vrn, registration, completeUserAnswers)
          val expectedAuditEvent =
            ReturnsAuditModel.build(vatReturnRequest, None, SubmissionResult.Duplicate, None, None, dataRequest)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(auditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
        }
      }
    }

    "when the submission to the backend fails" - {

      "and the question on the Correct Previous Return Page has been answered" - {

        "must redirect to Journey Recovery when an Unexpected Status Result is returned from the connector" in {

          val answers =
            completeUserAnswers
              .set(CorrectPreviousReturnPage, false).success.value

          val app =
            applicationBuilder(Some(answers))
              .overrides(
                bind[VatReturnService].toInstance(vatReturnService),
                bind[VatReturnConnector].toInstance(vatReturnConnector),
                bind[AuditService].toInstance(auditService)
              ).build()

          when(vatReturnService.fromUserAnswers(any(), any(), any(), any())) thenReturn Valid(vatReturnRequest)
          when(vatReturnConnector.submitWithCorrections(any[VatReturnWithCorrectionRequest]())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "foo")))
          doNothing().when(auditService).audit(any())(any(), any())

          running(app) {
            val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, incompletePromptShown = false).url)
            val result = route(app, request).value
            val dataRequest = DataRequest(request, testCredentials, vrn, registration, completeUserAnswers)
            val expectedAuditEvent =
              ReturnsAuditModel.build(vatReturnRequest, Some(correctionRequest), SubmissionResult.Failure, None, None, dataRequest)

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            verify(auditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
          }
        }

        "must redirect to Journey Recovery when an Invalid Vat Return and Correction Request are returned" in {

          val answers =
            completeUserAnswers
              .set(CorrectPreviousReturnPage, false).success.value

          val app =
            applicationBuilder(Some(answers))
              .overrides(
                bind[VatReturnService].toInstance(vatReturnService),
                bind[VatReturnConnector].toInstance(vatReturnConnector),
                bind[CorrectionService].toInstance(correctionService)
              ).build()

          when(vatReturnService.fromUserAnswers(any(), any(), any(), any())) thenReturn Invalid(NonEmptyChain(DataMissingError(SoldGoodsFromNiPage)))
          when(vatReturnConnector.submit(any[VatReturnRequest]())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "foo")))
          when(correctionService.fromUserAnswers(any(), any(), any(), any())) thenReturn Invalid(NonEmptyChain(DataMissingError(CorrectPreviousReturnPage)))

          running(app) {
            val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, incompletePromptShown = false).url)
            val result = route(app, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "must redirect to Journey Recovery when an Invalid Vat Return is returned and a Valid Correction Request is returned" in {

          val answers =
            completeUserAnswers
              .set(CorrectPreviousReturnPage, false).success.value

          val app =
            applicationBuilder(Some(answers))
              .overrides(
                bind[VatReturnService].toInstance(vatReturnService),
                bind[VatReturnConnector].toInstance(vatReturnConnector),
                bind[CorrectionService].toInstance(correctionService)
              ).build()

          when(vatReturnService.fromUserAnswers(any(), any(), any(), any())) thenReturn Invalid(NonEmptyChain(DataMissingError(SoldGoodsFromNiPage)))
          when(vatReturnConnector.submit(any[VatReturnRequest]())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "foo")))
          when(correctionService.fromUserAnswers(any(), any(), any(), any())) thenReturn Valid(correctionRequest)

          running(app) {
            val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, incompletePromptShown = false).url)
            val result = route(app, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "must redirect to Journey Recovery when an Valid Vat Return is returned and a Invalid Correction Request is returned" in {

          val answers =
            completeUserAnswers
              .set(CorrectPreviousReturnPage, false).success.value

          val app =
            applicationBuilder(Some(answers))
              .overrides(
                bind[VatReturnService].toInstance(vatReturnService),
                bind[VatReturnConnector].toInstance(vatReturnConnector),
                bind[CorrectionService].toInstance(correctionService)
              ).build()

          when(vatReturnService.fromUserAnswers(any(), any(), any(), any())) thenReturn Valid(vatReturnRequest)
          when(vatReturnConnector.submit(any[VatReturnRequest]())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "foo")))
          when(correctionService.fromUserAnswers(any(), any(), any(), any())) thenReturn Invalid(NonEmptyChain(DataMissingError(CorrectPreviousReturnPage)))

          running(app) {
            val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, incompletePromptShown = false).url)
            val result = route(app, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "must redirect to Journey Recovery when connector returns Conflict Found" in {

          val answers =
            completeUserAnswers
              .set(CorrectPreviousReturnPage, false).success.value

          val app =
            applicationBuilder(Some(answers))
              .overrides(
                bind[VatReturnService].toInstance(vatReturnService),
                bind[VatReturnConnector].toInstance(vatReturnConnector),
                bind[AuditService].toInstance(auditService),
                bind[CorrectionService].toInstance(correctionService)
              ).build()

          when(vatReturnService.fromUserAnswers(any(), any(), any(), any())) thenReturn Valid(vatReturnRequest)
          when(vatReturnConnector.submitWithCorrections(any[VatReturnWithCorrectionRequest]())(any())) thenReturn Future.successful(Left(ConflictFound))
          when(correctionService.fromUserAnswers(any(), any(), any(), any())) thenReturn Valid(correctionRequest)
          doNothing().when(auditService).audit(any())(any(), any())

          running(app) {
            val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, incompletePromptShown = false).url)
            val result = route(app, request).value
            val dataRequest = DataRequest(request, testCredentials, vrn, registration, completeUserAnswers)
            val expectedAuditEvent =
              ReturnsAuditModel.build(vatReturnRequest, Some(correctionRequest), SubmissionResult.Duplicate, None, None, dataRequest)

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            verify(auditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
          }
        }
      }

      "and the question on the Correct Previous Return Page has not been answered" - {

        "must redirect to Journey Recovery when an Unexpected Status Result is returned from the connector" in {

          val app =
            applicationBuilder(Some(emptyUserAnswers))
              .overrides(
                bind[VatReturnService].toInstance(vatReturnService),
                bind[VatReturnConnector].toInstance(vatReturnConnector),
                bind[AuditService].toInstance(auditService)
              ).build()

        when(vatReturnService.fromUserAnswers(any(), any(), any(), any())) thenReturn Valid(vatReturnRequest)
        when(vatReturnConnector.submit(any[VatReturnRequest]())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "foo")))
        doNothing().when(auditService).audit(any())(any(), any())

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, incompletePromptShown = false).url)
          val result = route(app, request).value
          val dataRequest = DataRequest(request, testCredentials, vrn, registration, completeUserAnswers)
          val expectedAuditEvent =
            ReturnsAuditModel.build(vatReturnRequest, None, SubmissionResult.Failure, None, None, dataRequest)

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            verify(auditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
          }
        }
      }
    }

    "when the user has not answered any  questions" - {

      "must redirect to Journey Recovery" in {

        val app =
          applicationBuilder(Some(emptyUserAnswers))
            .overrides(bind[VatReturnConnector].toInstance(vatReturnConnector))
            .build()

        when(vatReturnConnector.submit(any[VatReturnRequest]())(any())) thenReturn Future.successful(Right(vatReturn))

        running(app) {

          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, incompletePromptShown = false).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "must display total sales sections from eu and ni" in {
      val salesAtVatRateService = mock[SalesAtVatRateService]
      val spain = Country("ES", "Spain")

      when(salesAtVatRateService.getEuTotalVatOnSales(any())).thenReturn(Some(BigDecimal(3333)))
      when(salesAtVatRateService.getEuTotalNetSales(any())).thenReturn(Some(BigDecimal(4444)))
      when(salesAtVatRateService.getNiTotalVatOnSales(any())).thenReturn(Some(BigDecimal(5555)))
      when(salesAtVatRateService.getNiTotalNetSales(any())).thenReturn(Some(BigDecimal(6666)))
      when(salesAtVatRateService.getTotalVatOwedAfterCorrections(any())).thenReturn(BigDecimal(8888))
      when(salesAtVatRateService.getVatOwedToEuCountries(any()))
        .thenReturn(List(TotalVatToCountry(spain, BigDecimal(7777))))

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(bind[SalesAtVatRateService].toInstance(salesAtVatRateService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(period).url)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result).contains("&pound;6,666") mustBe true
        contentAsString(result).contains("&pound;5,555") mustBe true
        contentAsString(result).contains("&pound;4,444") mustBe true
        contentAsString(result).contains("&pound;3,333") mustBe true
        contentAsString(result).contains("&pound;7,777") mustBe true
        contentAsString(result).contains("&pound;8,888") mustBe true
      }
    }

    "must clear cached vat return when return submitted" in {
      val mockCachedVatReturnRepository = mock[CachedVatReturnRepository]

      val answers = completeUserAnswers

      when(vatReturnConnector.submit(any[VatReturnRequest]())(any())) thenReturn Future.successful(Right(vatReturn))
      when(emailService.sendConfirmationEmail(any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(EMAIL_ACCEPTED))
      when(mockCachedVatReturnRepository.clear(any(), any())) thenReturn Future.successful(true)
      when(s4lConnector.delete(any())(any())) thenReturn Future.successful(Right(true))

      val app = applicationBuilder(userAnswers = Some(answers))
        .overrides(
          bind[CachedVatReturnRepository].toInstance(mockCachedVatReturnRepository),
          bind[VatReturnConnector].toInstance(vatReturnConnector),
          bind[EmailService].toInstance(emailService),
          bind[SaveForLaterConnector].toInstance(s4lConnector)
        ).build()

      running(app) {

        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, incompletePromptShown = false).url)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ReturnSubmittedController.onPageLoad(period).url
        verify(vatReturnConnector, times(1)).submit(any[VatReturnRequest]())(any())
        verify(mockCachedVatReturnRepository, times(1)).clear(eqTo(answers.userId), eqTo(period))
      }
    }

    "when the user has not answered" - {

      "a question but the missing data prompt has not been shown, must refresh page" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsFromNiPage, true).success.value
          .set(SoldGoodsFromEuPage, false).success.value

        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, false).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad(period).url
        }
      }

      "country of consumption from NI must redirect to CountryOfConsumptionFromNiController" in {

        val answers = emptyUserAnswers
            .set(SoldGoodsFromNiPage, true).success.value
            .set(SoldGoodsFromEuPage, false).success.value

        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, true).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CountryOfConsumptionFromNiController.onPageLoad(CheckMode, period, Index(0)).url
        }
      }

      "vat rates from NI, must redirect to VatRatesFromNiController" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsFromNiPage, true).success.value
          .set(CountryOfConsumptionFromNiPage(Index(0)), Country.euCountries.head).success.value
          .set(SoldGoodsFromEuPage, false).success.value

        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, true).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.VatRatesFromNiController.onPageLoad(CheckMode, period, Index(0)).url
        }
      }

      "net value of sales from NI must redirect to NetValueOfSalesFromNiController" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsFromNiPage, true).success.value
          .set(CountryOfConsumptionFromNiPage(Index(0)), Country.euCountries.head).success.value
          .set(VatRatesFromNiPage(Index(0)), List(VatRate(BigDecimal(20.0), VatRateType.Standard, LocalDate.now() ))).success.value
          .set(SoldGoodsFromEuPage, false).success.value

        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, true).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.NetValueOfSalesFromNiController.onPageLoad(CheckMode, period, Index(0), Index(0)).url
        }
      }

      "vat on sales from NI must redirect to VatOnSalesFromNiController" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsFromNiPage, true).success.value
          .set(CountryOfConsumptionFromNiPage(Index(0)), Country.euCountries.head).success.value
          .set(VatRatesFromNiPage(Index(0)), List(VatRate(BigDecimal(20.0), VatRateType.Standard, LocalDate.now() ))).success.value
          .set(NetValueOfSalesFromNiPage(Index(0), Index(0)), BigDecimal(2000.0)).success.value
          .set(SoldGoodsFromEuPage, false).success.value

        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, true).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.VatOnSalesFromNiController.onPageLoad(CheckMode, period, Index(0), Index(0)).url
        }
      }

      "country of sale from EU must redirect to CountryOfSaleFromEuController" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsFromNiPage, false).success.value
          .set(SoldGoodsFromEuPage, true).success.value

        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, true).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CountryOfSaleFromEuController.onPageLoad(CheckMode, period, Index(0)).url
        }
      }

      "country of consumption from EU must redirect to CountryOfConsumptionFromEuController" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsFromNiPage, false).success.value
          .set(SoldGoodsFromEuPage, true).success.value
          .set(CountryOfSaleFromEuPage(Index(0)), Country.euCountries.head).success.value
        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, true).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CountryOfConsumptionFromEuController.onPageLoad(CheckMode, period, Index(0), Index(0)).url
        }
      }

      "vat rates from EU must redirect to VatRatesFromEuController" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsFromNiPage, false).success.value
          .set(SoldGoodsFromEuPage, true).success.value
          .set(CountryOfSaleFromEuPage(Index(0)), Country.euCountries.head).success.value
          .set(CountryOfConsumptionFromEuPage(Index(0), Index(0)), Country.euCountries.tail.head).success.value
        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, true).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.VatRatesFromEuController.onPageLoad(CheckMode, period, Index(0), Index(0)).url
        }
      }

      "net value of sales from EU must redirect to NetValueOfSalesFromEuController" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsFromNiPage, false).success.value
          .set(SoldGoodsFromEuPage, true).success.value
          .set(CountryOfSaleFromEuPage(Index(0)), Country.euCountries.head).success.value
          .set(CountryOfConsumptionFromEuPage(Index(0), Index(0)), Country.euCountries.tail.head).success.value
          .set(VatRatesFromEuPage(Index(0), Index(0)), List(VatRate(BigDecimal(20.0), VatRateType.Standard, LocalDate.now()))).success.value
        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, true).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.NetValueOfSalesFromEuController.onPageLoad(CheckMode, period, Index(0), Index(0), Index(0)).url
        }
      }

      "vat charged on sales from EU must redirect to VatOnSalesFromEuController" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsFromNiPage, false).success.value
          .set(SoldGoodsFromEuPage, true).success.value
          .set(CountryOfSaleFromEuPage(Index(0)), Country.euCountries.head).success.value
          .set(CountryOfConsumptionFromEuPage(Index(0), Index(0)), Country.euCountries.tail.head).success.value
          .set(VatRatesFromEuPage(Index(0), Index(0)), List(VatRate(BigDecimal(20.0), VatRateType.Standard, LocalDate.now()))).success.value
          .set(NetValueOfSalesFromEuPage(Index(0), Index(0), Index(0)), BigDecimal(20000.0)).success.value

        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, true).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.VatOnSalesFromEuController.onPageLoad(CheckMode, period, Index(0), Index(0), Index(0)).url
        }
      }

      "period to correct must redirect to CorrectionReturnPeriodController" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsFromNiPage, false).success.value
          .set(SoldGoodsFromEuPage, false).success.value
          .set(CorrectPreviousReturnPage, true).success.value

        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, true).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual correctionsRoutes.CorrectionReturnPeriodController.onPageLoad(CheckMode, period, Index(0)).url
        }
      }

      "country of correction must redirect to CorrectionCountryController" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsFromNiPage, false).success.value
          .set(SoldGoodsFromEuPage, false).success.value
          .set(CorrectPreviousReturnPage, true).success.value
          .set(CorrectionReturnPeriodPage(Index(0)), Period("2022", "Q1").get).success.value

        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, true).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual correctionsRoutes.CorrectionCountryController.onPageLoad(CheckMode, period, Index(0), Index(0)).url
        }
      }

      "amount of correction must redirect to CountryVatCorrectionController" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsFromNiPage, false).success.value
          .set(SoldGoodsFromEuPage, false).success.value
          .set(CorrectPreviousReturnPage, true).success.value
          .set(CorrectionReturnPeriodPage(Index(0)), Period("2022", "Q1").get).success.value
          .set(CorrectionCountryPage(Index(0), Index(0)), Country.euCountries.head).success.value

        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, true).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual correctionsRoutes.CountryVatCorrectionController.onPageLoad(CheckMode, period, Index(0), Index(0), false).url
        }
      }
    }

    "when submission fails due to registration not being present in core" - {
      "must not save the return, save answers for later and redirect to the No Registration Found In Core page" in {

        val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value
            .set(SoldGoodsFromEuPage, false).success.value

        when(vatReturnConnector.submit(any[VatReturnRequest]())(any())) thenReturn Future.successful(Left(RegistrationNotFound))
        when(s4lConnector.submit(any())(any())) thenReturn Future.successful(Right(Some(SavedUserAnswers(vrn, period, answers.data, answers.lastUpdated))))


        val app =
          applicationBuilder(Some(answers))
            .overrides(
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[CorrectionConnector].toInstance(correctionConnector),
              bind[SaveForLaterConnector].toInstance(s4lConnector)
            )
            .build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, false).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.NoRegistrationFoundInCoreController.onPageLoad().url
          verify(s4lConnector, times(1)).submit(any[SaveForLaterRequest]())(any())
        }
      }
    }

    "when submission fails due to error received from core" - {
      "must not save the return, save answers for later and redirect to the Received Error From Core page" in {

        val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value
            .set(SoldGoodsFromEuPage, false).success.value

        when(vatReturnConnector.submit(any[VatReturnRequest]())(any())) thenReturn Future.successful(Left(ReceivedErrorFromCore))
        when(s4lConnector.submit(any())(any())) thenReturn Future.successful(Right(Some(SavedUserAnswers(vrn, period, answers.data, answers.lastUpdated))))


        val app =
          applicationBuilder(Some(answers))
            .overrides(
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[CorrectionConnector].toInstance(correctionConnector),
              bind[SaveForLaterConnector].toInstance(s4lConnector)
            )
            .build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period, false).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.ReceivedErrorFromCoreController.onPageLoad().url
          verify(s4lConnector, times(1)).submit(any[SaveForLaterRequest]())(any())
        }
      }
    }
  }
}
