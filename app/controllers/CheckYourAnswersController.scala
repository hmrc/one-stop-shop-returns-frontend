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

import cats.data.Validated.{Invalid, Valid}
import com.google.inject.Inject
import connectors.VatReturnConnector
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.audit.{ReturnForDataEntryAuditModel, ReturnsAuditModel, SubmissionResult}
import models.domain.VatReturn
import models.emails.EmailSendingResult.EMAIL_ACCEPTED
import models.requests.corrections.CorrectionRequest
import models.requests.{DataRequest, VatReturnRequest, VatReturnWithCorrectionRequest}
import models.responses.ConflictFound
import models.{NormalMode, Period}
import pages.CheckYourAnswersPage
import pages.corrections.CorrectPreviousReturnPage
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.EmailConfirmationQuery
import queries.corrections.AllCorrectionPeriodsQuery
import services.corrections.CorrectionService
import services.{AuditService, EmailService, SalesAtVatRateService, VatReturnService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax._
import viewmodels.checkAnswers._
import viewmodels.checkAnswers.corrections.{CorrectPreviousReturnSummary, CorrectionReturnPeriodSummary}
import viewmodels.govuk.summarylist._
import views.html.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(
                                            cc: AuthenticatedControllerComponents,
                                            service: SalesAtVatRateService,
                                            view: CheckYourAnswersView,
                                            vatReturnService: VatReturnService,
                                            correctionService: CorrectionService,
                                            auditService: AuditService,
                                            emailService: EmailService,
                                            vatReturnConnector: VatReturnConnector,
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>

      val businessSummaryList = getBusinessSummaryList(request)

      val salesFromNiSummaryList = getSalesFromNiSummaryList(request)

      val salesFromEuSummaryList = getSalesFromEuSummaryList(request)

      val containsCorrections = request.userAnswers.get(AllCorrectionPeriodsQuery).isDefined

      val totalVatToCountries =
        service.getVatOwedToEuCountries(request.userAnswers).filter(vat => vat.totalVat > 0)
      val noPaymentDueCountries =
        service.getVatOwedToEuCountries(request.userAnswers).filter(vat => vat.totalVat <= 0)

      val totalVatOnSales =
        service.getTotalVatOwedAfterCorrections(request.userAnswers)

      val summaryLists = getAllSummaryLists(request, businessSummaryList, salesFromNiSummaryList, salesFromEuSummaryList)

      Ok(view(
        summaryLists,
        period,
        totalVatToCountries,
        totalVatOnSales,
        noPaymentDueCountries,
        containsCorrections
      ))
  }

  private def getAllSummaryLists(
                                  request: DataRequest[AnyContent],
                                  businessSummaryList: SummaryList,
                                  salesFromNiSummaryList: SummaryList,
                                  salesFromEuSummaryList: SummaryList
                                )(implicit messages: Messages) =
    if (request.userAnswers.get(CorrectPreviousReturnPage).isDefined) {
      val correctionsSummaryList = SummaryListViewModel(
        rows = Seq(
          CorrectPreviousReturnSummary.row(request.userAnswers),
          CorrectionReturnPeriodSummary.getAllRows(request.userAnswers)
        ).flatten
      ).withCssClass("govuk-!-margin-bottom-9")
      Seq(
        (None, businessSummaryList),
        (Some("checkYourAnswers.salesFromNi.heading"), salesFromNiSummaryList),
        (Some("checkYourAnswers.salesFromEU.heading"), salesFromEuSummaryList),
        (Some("checkYourAnswers.corrections.heading"), correctionsSummaryList)
      )
    } else {
      Seq(
        (None, businessSummaryList),
        (Some("checkYourAnswers.salesFromNi.heading"), salesFromNiSummaryList),
        (Some("checkYourAnswers.salesFromEU.heading"), salesFromEuSummaryList)
      )
    }

  private def getSalesFromEuSummaryList(request: DataRequest[AnyContent])(implicit messages: Messages) = {
    SummaryListViewModel(
      rows = Seq(
        SoldGoodsFromEuSummary.row(request.userAnswers),
        TotalEUNetValueOfSalesSummary.row(request.userAnswers, service.getEuTotalNetSales(request.userAnswers)),
        TotalEUVatOnSalesSummary.row(request.userAnswers, service.getEuTotalVatOnSales(request.userAnswers))
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")
  }

  private def getSalesFromNiSummaryList(request: DataRequest[AnyContent])(implicit messages: Messages) = {
    SummaryListViewModel(
      rows = Seq(
        SoldGoodsFromNiSummary.row(request.userAnswers),
        TotalNINetValueOfSalesSummary.row(request.userAnswers, service.getNiTotalNetSales(request.userAnswers)),
        TotalNIVatOnSalesSummary.row(request.userAnswers, service.getNiTotalVatOnSales(request.userAnswers))
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")
  }

  private def getBusinessSummaryList(request: DataRequest[AnyContent])(implicit messages: Messages) = {
    SummaryListViewModel(
      rows = Seq(
        BusinessNameSummary.row(request.registration),
        BusinessVRNSummary.row(request.registration),
        ReturnPeriodSummary.row(request.userAnswers)
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")
  }

  def onSubmit(period: Period): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>
      val validatedVatReturnRequest =
        vatReturnService.fromUserAnswers(request.userAnswers, request.vrn, period, request.registration)

      if (request.userAnswers.get(CorrectPreviousReturnPage).isDefined) {
        val validatedCorrectionRequest =
          correctionService.fromUserAnswers(request.userAnswers, request.vrn, period, request.registration.commencementDate)

        (validatedVatReturnRequest, validatedCorrectionRequest) match {
          case (Valid(vatReturnRequest), Valid(correctionRequest)) =>
            val vatReturnWithCorrectionRequest = VatReturnWithCorrectionRequest(vatReturnRequest, correctionRequest)
            vatReturnConnector.submitWithCorrection(vatReturnWithCorrectionRequest).flatMap {
              case Right((vatReturn: VatReturn, _)) =>
                auditEmailAndRedirect(vatReturnRequest, Some(correctionRequest), vatReturn, period)
              case Left(ConflictFound) =>
                auditService.audit(ReturnsAuditModel.build(
                  vatReturnRequest, Some(correctionRequest), SubmissionResult.Duplicate, None, None, request
                ))
                Redirect(routes.YourAccountController.onPageLoad()).toFuture
              case Left(e) =>
                logger.error(s"Unexpected result on submit: ${e.toString}")
                auditService.audit(ReturnsAuditModel.build(
                  vatReturnRequest, Some(correctionRequest), SubmissionResult.Failure, None, None, request
                ))
                Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture
            }

          case (Invalid(vatReturnErrors), Invalid(correctionErrors)) =>
            val errors = vatReturnErrors ++ correctionErrors
            val errorList = errors.toChain.toList
            val errorMessages = errorList.map(_.errorMessage).mkString("\n")
            logger.error(s"Unable to create a vat return and correction request from user answers: $errorMessages")

            Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture
          case (Invalid(errors), _) =>
            val errorList = errors.toChain.toList
            val errorMessages = errorList.map(_.errorMessage).mkString("\n")
            logger.error(s"Unable to create a vat return request from user answers: $errorMessages")

            Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture
          case (_, Invalid(errors)) =>
            val errorList = errors.toChain.toList
            val errorMessages = errorList.map(_.errorMessage).mkString("\n")
            logger.error(s"Unable to create a Corrections request from user answers: $errorMessages")

            Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture
        }
      } else {
        validatedVatReturnRequest match {
          case Valid(vatReturnRequest) =>
            vatReturnConnector.submit(vatReturnRequest).flatMap {
              case Right(vatReturn: VatReturn) =>
                auditEmailAndRedirect(vatReturnRequest, None, vatReturn, period)
              case Left(ConflictFound) =>
                auditService.audit(ReturnsAuditModel.build(
                  vatReturnRequest, None, SubmissionResult.Duplicate, None, None, request
                ))
                Redirect(routes.YourAccountController.onPageLoad()).toFuture
              case Left(e) =>
                logger.error(s"Unexpected result on submit: ${e.toString}")
                auditService.audit(ReturnsAuditModel.build(
                  vatReturnRequest, None, SubmissionResult.Failure, None, None, request
                ))
                Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture
            }
          case Invalid(errors) =>
            val errorList = errors.toChain.toList
            val errorMessages = errorList.map(_.errorMessage).mkString("\n")
            logger.error(s"Unable to create a VAT return request from user answers: $errorMessages")

            Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture
        }
      }

  }

  private def auditEmailAndRedirect(
                                     returnRequest: VatReturnRequest,
                                     correctionRequest: Option[CorrectionRequest],
                                     vatReturn: VatReturn,
                                     period: Period
                                   )(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[Result] = {
    auditService.audit(
      ReturnsAuditModel.build(
        returnRequest,
        correctionRequest,
        SubmissionResult.Success,
        Some(vatReturn.reference),
        Some(vatReturn.paymentReference),
        request
      )
    )

    auditService.audit(
      ReturnForDataEntryAuditModel(
        returnRequest,
        correctionRequest,
        vatReturn.reference,
        vatReturn.paymentReference
      )
    )

    emailService.sendConfirmationEmail(
      request.registration.contactDetails.fullName,
      request.registration.registeredCompanyName,
      request.registration.contactDetails.emailAddress,
      service.getTotalVatOwedAfterCorrections(request.userAnswers),
      period
    ) flatMap {
      emailConfirmationResult =>
        val emailSent = EMAIL_ACCEPTED == emailConfirmationResult

        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(EmailConfirmationQuery, emailSent))
          _ <- cc.sessionRepository.set(updatedAnswers)
        } yield {
          Redirect(CheckYourAnswersPage.navigate(NormalMode, request.userAnswers))
        }
    }
  }
}