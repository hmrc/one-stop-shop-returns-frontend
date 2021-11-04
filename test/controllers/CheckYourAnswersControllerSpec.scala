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
import cats.data.Validated.Valid
import connectors.VatReturnConnector
import models.audit.{ReturnForDataEntryAuditModel, ReturnsAuditModel, SubmissionResult}
import models.domain.VatReturn
import models.emails.EmailSendingResult.EMAIL_ACCEPTED
import models.requests.DataRequest
import models.responses.{ConflictFound, UnexpectedResponseStatus}
import models.{Country, NormalMode, TotalVatToCountry}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito
import org.mockito.Mockito.{doNothing, times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, CountryVatCorrectionPage}
import pages.{CheckYourAnswersPage, SoldGoodsFromEuPage, SoldGoodsFromNiPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.EmailConfirmationQuery
import repositories.SessionRepository
import services.{AuditService, EmailService, SalesAtVatRateService, VatReturnService}
import viewmodels.govuk.SummaryListFluency

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with MockitoSugar with SummaryListFluency with BeforeAndAfterEach {

  private val vatReturnConnector = mock[VatReturnConnector]
  private val vatReturnService = mock[VatReturnService]
  private val auditService = mock[AuditService]
  private val salesAtVatRateService = mock[SalesAtVatRateService]
  private val emailService =  mock[EmailService]

  override def beforeEach(): Unit = {
    Mockito.reset(vatReturnConnector, vatReturnService, auditService, salesAtVatRateService, emailService)
    super.beforeEach()
  }

  "Check Your Answers Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers)).build()

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
      }
    }

    "must contain VAT declared to EU countries after corrections heading if there were corrections" in {
      val answers = completeUserAnswers
        .set(CorrectionReturnPeriodPage(index), period).success.value
        .set(CorrectionCountryPage(index, index), Country("EE", "Estonia")).success.value
        .set(CountryVatCorrectionPage(index, index), BigDecimal(1000)).success.value

      val application = applicationBuilder(userAnswers = Some(answers)).build()

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
      }
    }

    "must contain VAT declared where no payment is due heading if there were corrections" in {
      val answers = completeUserAnswers
        .set(CorrectionReturnPeriodPage(index), period).success.value
        .set(CorrectionCountryPage(index, index), Country("EE", "Estonia")).success.value
        .set(CountryVatCorrectionPage(index, index), BigDecimal(-1000)).success.value

      val application = applicationBuilder(userAnswers = Some(answers)).build()

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

    "when the user answered all necessary data and submission of the return succeeds" - {

      "must redirect to the next page" in {

        val answers =
          emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value
            .set(SoldGoodsFromEuPage, false).success.value

        when(vatReturnConnector.submit(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(emailService.sendConfirmationEmail(any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(EMAIL_ACCEPTED))

        val app =
          applicationBuilder(Some(answers))
            .overrides(
              bind[VatReturnConnector].toInstance(vatReturnConnector),
              bind[EmailService].toInstance(emailService)
            ).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(period).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.ReturnSubmittedController.onPageLoad(period).url
        }
      }

      "must audit the event and redirect to the next page and successfully send email confirmation" in {
        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        when(vatReturnService.fromUserAnswers(any(), any(), any(), any())) thenReturn Valid(vatReturnRequest)
        when(vatReturnConnector.submit(any())(any())) thenReturn Future.successful(Right(vatReturn))
        when(emailService.sendConfirmationEmail(any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(EMAIL_ACCEPTED))

        val totalVatOnSales = BigDecimal(100)
        when(salesAtVatRateService.getTotalVatOwedAfterCorrections(any())) thenReturn totalVatOnSales
        doNothing().when(auditService).audit(any())(any(), any())

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(
            bind[VatReturnService].toInstance(vatReturnService),
            bind[VatReturnConnector].toInstance(vatReturnConnector),
            bind[SalesAtVatRateService].toInstance(salesAtVatRateService),
            bind[AuditService].toInstance(auditService),
            bind[EmailService].toInstance(emailService),
            bind[SessionRepository].toInstance(mockSessionRepository),
          ).build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(vatReturnRequest.period).url)
          val result = route(application, request).value
          val dataRequest = DataRequest(request, testCredentials, vrn, registration, completeUserAnswers)
          val expectedAuditEvent = ReturnsAuditModel.build(
            vatReturnRequest, SubmissionResult.Success, Some(vatReturn.reference), Some(vatReturn.paymentReference), dataRequest
          )
          val expectedAuditEventForDataEntry = ReturnForDataEntryAuditModel(vatReturnRequest, vatReturn.reference, vatReturn.paymentReference)

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
        when(vatReturnConnector.submit(any())(any())) thenReturn Future.successful(Left(ConflictFound))
        doNothing().when(auditService).audit(any())(any(), any())

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(period).url)
          val result = route(app, request).value
          val dataRequest = DataRequest(request, testCredentials, vrn, registration, completeUserAnswers)
          val expectedAuditEvent =
            ReturnsAuditModel.build(vatReturnRequest, SubmissionResult.Duplicate, None, None, dataRequest)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.YourAccountController.onPageLoad().url
          verify(auditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
        }
      }
    }

    "when the submission to the backend fails" - {

      "must redirect to Journey Recovery" in {

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
        when(vatReturnConnector.submit(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "foo")))
        doNothing().when(auditService).audit(any())(any(), any())

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(period).url)
          val result = route(app, request).value
          val dataRequest = DataRequest(request, testCredentials, vrn, registration, completeUserAnswers)
          val expectedAuditEvent =
            ReturnsAuditModel.build(vatReturnRequest, SubmissionResult.Failure, None, None, dataRequest)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(auditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
        }
      }
    }

    "when the user has not answered all necessary questions" - {

      "must redirect to Journey Recovery" in {

        val app =
          applicationBuilder(Some(emptyUserAnswers))
            .overrides(bind[VatReturnConnector].toInstance(vatReturnConnector))
            .build()

        when(vatReturnConnector.submit(any())(any())) thenReturn Future.successful(Right((vatReturn)))

        running(app) {

          val request = FakeRequest(POST, routes.CheckYourAnswersController.onPageLoad(period).url)
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
  }
}
