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

import cats.data.Validated.{Invalid, Valid}
import com.google.inject.Inject
import connectors.{SaveForLaterConnector, SavedUserAnswers, VatReturnConnector}
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.audit.{ReturnForDataEntryAuditModel, ReturnsAuditModel, SubmissionResult}
import models.corrections.CorrectionPayload
import models.domain.VatReturn
import models.emails.EmailSendingResult.EMAIL_ACCEPTED
import models.requests.corrections.CorrectionRequest
import models.requests.{DataRequest, SaveForLaterRequest, VatReturnRequest, VatReturnWithCorrectionRequest}
import models.responses.{ConflictFound, ErrorResponse, ReceivedErrorFromCore, RegistrationNotFound}
import models.{NormalMode, Period, StandardPeriod}
import pages.corrections.CorrectPreviousReturnPage
import pages.{CheckYourAnswersPage, SavedProgressPage}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.*
import queries.*
import queries.corrections.AllCorrectionPeriodsQuery
import repositories.CachedVatReturnRepository
import services.*
import services.corrections.CorrectionService
import services.exclusions.ExclusionService
import uk.gov.hmrc.govukfrontend.views.Aliases.Card
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{CardTitle, SummaryList}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.*
import viewmodels.checkAnswers.*
import viewmodels.checkAnswers.corrections.{CorrectPreviousReturnSummary, CorrectionReturnPeriodSummary}
import viewmodels.govuk.summarylist.*
import views.html.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(
                                            cc: AuthenticatedControllerComponents,
                                            service: SalesAtVatRateService,
                                            exclusionService: ExclusionService,
                                            view: CheckYourAnswersView,
                                            vatReturnService: VatReturnService,
                                            vatReturnSalesService: VatReturnSalesService,
                                            redirectService: RedirectService,
                                            correctionService: CorrectionService,
                                            auditService: AuditService,
                                            emailService: EmailService,
                                            vatReturnConnector: VatReturnConnector,
                                            cachedVatReturnRepository: CachedVatReturnRepository,
                                            saveForLaterConnector: SaveForLaterConnector
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      val errors = redirectService.validate(period)

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

      for {
        currentReturnIsFinal <- exclusionService.currentReturnIsFinal(request.registration, request.userAnswers.period)
      } yield {
        Ok(view(
          summaryLists,
          request.userAnswers.period,
          totalVatToCountries,
          totalVatOnSales,
          noPaymentDueCountries,
          containsCorrections,
          errors.map(_.errorMessage),
          request.registration.excludedTrader,
          currentReturnIsFinal
        ))
      }
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
      ).withCard(
        card = Card(
          title = Some(CardTitle(content = HtmlContent(messages("checkYourAnswers.corrections.heading")))),
          actions = None
        )
      )
      Seq(
        (None, businessSummaryList),
        (None, salesFromNiSummaryList),
        (None, salesFromEuSummaryList),
        (None, correctionsSummaryList)
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
    ).withCard(
      card = Card(
        title = Some(CardTitle(content = HtmlContent(messages("checkYourAnswers.salesFromEU.heading")))),
        actions = None
      )
    )
  }

  private def getSalesFromNiSummaryList(request: DataRequest[AnyContent])(implicit messages: Messages) = {
    SummaryListViewModel(
      rows = Seq(
        SoldGoodsFromNiSummary.row(request.userAnswers),
        TotalNINetValueOfSalesSummary.row(request.userAnswers, service.getNiTotalNetSales(request.userAnswers)),
        TotalNIVatOnSalesSummary.row(request.userAnswers, service.getNiTotalVatOnSales(request.userAnswers))
      ).flatten
    ).withCard(
      card = Card(
        title = Some(CardTitle(content = HtmlContent(messages("checkYourAnswers.salesFromNi.heading")))),
        actions = None
      )
    )
  }

  private def getBusinessSummaryList(request: DataRequest[AnyContent])(implicit messages: Messages) = {
    SummaryListViewModel(
      rows = Seq(
        BusinessNameSummary.row(request.registration),
        BusinessVRNSummary.row(request.registration),
        ReturnPeriodSummary.row(request.userAnswers)
      ).flatten
    ).withCssClass("govuk-summary-card govuk-summary-card__content govuk-!-display-block width-auto")
  }

  def onSubmit(period: Period, incompletePromptShown: Boolean): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      val preferredPeriod = request.userAnswers.period

      val redirectToFirstError = redirectService.getRedirect(redirectService.validate(preferredPeriod), preferredPeriod).headOption

      (redirectToFirstError, incompletePromptShown) match {
        case (Some(redirect), true) => Future.successful(Redirect(redirect))
        case (Some(_), false) => Future.successful(Redirect(routes.CheckYourAnswersController.onPageLoad(preferredPeriod)))
        case _ =>
          val validatedVatReturnRequest =
            vatReturnService.fromUserAnswers(request.userAnswers, request.vrn, request.userAnswers.period, request.registration)

          val validatedCorrectionRequest = request.userAnswers.get(CorrectPreviousReturnPage).map(_ =>
            correctionService.fromUserAnswers(request.userAnswers, request.vrn, request.userAnswers.period, request.registration.commencementDate))

          (validatedVatReturnRequest, validatedCorrectionRequest) match {
            case (Valid(vatReturnRequest), Some(Valid(correctionRequest))) =>
              submitReturn(vatReturnRequest, Option(correctionRequest), preferredPeriod)
            case (Valid(vatReturnRequest), None) =>
              submitReturn(vatReturnRequest, None, preferredPeriod)
            case (Invalid(vatReturnErrors), Some(Invalid(correctionErrors))) =>
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
            case (_, Some(Invalid(errors))) =>
              val errorList = errors.toChain.toList
              val errorMessages = errorList.map(_.errorMessage).mkString("\n")
              logger.error(s"Unable to create a Corrections request from user answers: $errorMessages")
              Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture
          }
      }
  }

  private def submitReturn(
                            vatReturnRequest: VatReturnRequest,
                            correctionRequest: Option[CorrectionRequest],
                            period: Period
                          )
                          (implicit request: DataRequest[AnyContent]): Future[Result] = {

    val submission: Future[Either[ErrorResponse, Product]] = correctionRequest match {
      case Some(cr) => vatReturnConnector.submitWithCorrections(VatReturnWithCorrectionRequest(vatReturnRequest, cr))
      case _ => vatReturnConnector.submit(vatReturnRequest)
    }

    submission.flatMap {
      case Right(vatReturn: VatReturn) =>
        auditEmailAndRedirect(vatReturnRequest, correctionRequest, vatReturn, period, None)
      case Right((vatReturn: VatReturn, correctionPayload: CorrectionPayload)) =>
        auditEmailAndRedirect(vatReturnRequest, correctionRequest, vatReturn, period, Some(correctionPayload))
      case Left(RegistrationNotFound) =>
        auditService.audit(ReturnsAuditModel.build(
          vatReturnRequest, correctionRequest, SubmissionResult.NotYetRegistered, None, None, request
        ))
        saveUserAnswersOnCoreError(period, routes.NoRegistrationFoundInCoreController.onPageLoad())
      case Left(ReceivedErrorFromCore) =>
        auditService.audit(ReturnsAuditModel.build(
          vatReturnRequest, correctionRequest, SubmissionResult.Failure, None, None, request
        ))
        saveUserAnswersOnCoreError(period, routes.ReceivedErrorFromCoreController.onPageLoad())
      case Left(ConflictFound) =>
        auditService.audit(ReturnsAuditModel.build(
          vatReturnRequest, correctionRequest, SubmissionResult.Duplicate, None, None, request
        ))
        Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture
      case Left(e) =>
        logger.error(s"Unexpected result on submit: ${e.toString}")
        auditService.audit(ReturnsAuditModel.build(
          vatReturnRequest, correctionRequest, SubmissionResult.Failure, None, None, request
        ))
        Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture
      case Right(_) | Right((_, _)) =>
        auditService.audit(ReturnsAuditModel.build(
          vatReturnRequest, correctionRequest, SubmissionResult.Failure, None, None, request
        ))
        Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture
    }
  }

  private def auditEmailAndRedirect(
                                     returnRequest: VatReturnRequest,
                                     correctionRequest: Option[CorrectionRequest],
                                     vatReturn: VatReturn,
                                     period: Period,
                                     maybeCorrectionPayload: Option[CorrectionPayload]
                                   )(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[Result] = {
    
    val vatOwed: BigDecimal = vatReturnSalesService.getTotalVatOnSalesAfterCorrection(vatReturn, maybeCorrectionPayload)
    
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
          updatedAnswersWithVatOwed <- Future.fromTry(updatedAnswers.set(TotalAmountVatDueGBPQuery, vatOwed))
          _ <- cc.sessionRepository.set(updatedAnswersWithVatOwed)
          _ <- cachedVatReturnRepository.clear(request.userId, StandardPeriod.fromPeriod(period))
          _ <- saveForLaterConnector.delete(period)
        } yield {
          Redirect(CheckYourAnswersPage.navigate(NormalMode, updatedAnswersWithVatOwed))
        }
    }
  }

  private def saveUserAnswersOnCoreError(period: Period, redirectLocation: Call)(implicit request: DataRequest[AnyContent]): Future[Result] =
    Future.fromTry(request.userAnswers.set(SavedProgressPage, routes.CheckYourAnswersController.onPageLoad(period).url)).flatMap {
      updatedAnswers =>
        val s4LRequest = SaveForLaterRequest(updatedAnswers, request.vrn, StandardPeriod.fromPeriod(period))

        saveForLaterConnector.submit(s4LRequest).flatMap {
          case Right(Some(_: SavedUserAnswers)) =>
            for {
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield {
              Redirect(redirectLocation)
            }
          case Left(ConflictFound) =>
            Future.successful(Redirect(routes.YourAccountController.onPageLoad()))
          case Left(e) =>
            logger.error(s"Unexpected result on submit: ${e.toString}")
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          case Right(None) =>
            logger.error(s"Unexpected result on submit")
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
    }
}