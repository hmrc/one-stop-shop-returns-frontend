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

import cats.data.Validated.{Invalid, Valid}
import com.google.inject.Inject
import connectors.{SaveForLaterConnector, SavedUserAnswers, VatReturnConnector}
import controllers.actions.AuthenticatedControllerComponents
import controllers.corrections.{routes => correctionsRoutes}
import logging.Logging
import models.audit.{ReturnForDataEntryAuditModel, ReturnsAuditModel, SubmissionResult}
import models.domain.VatReturn
import models.emails.EmailSendingResult.EMAIL_ACCEPTED
import models.requests.corrections.CorrectionRequest
import models.requests.{DataRequest, SaveForLaterRequest, VatReturnRequest, VatReturnWithCorrectionRequest}
import models.responses.{ConflictFound, ErrorResponse, ReceivedErrorFromCore, RegistrationNotFound}
import models.{CheckMode, DataMissingError, Index, NormalMode, Period, ValidationError}
import pages.corrections.CorrectPreviousReturnPage
import pages.{CheckYourAnswersPage, SavedProgressPage, VatRatesFromEuPage, VatRatesFromNiPage}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import queries._
import queries.corrections.{AllCorrectionCountriesQuery, AllCorrectionPeriodsQuery, CorrectionToCountryQuery}
import repositories.CachedVatReturnRepository
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
                                            cachedVatReturnRepository: CachedVatReturnRepository,
                                            saveForLaterConnector: SaveForLaterConnector
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      val errors = validate(period)

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

      Future.successful(Ok(view(
        summaryLists,
        request.userAnswers.period,
        totalVatToCountries,
        totalVatOnSales,
        noPaymentDueCountries,
        containsCorrections,
        errors.map(_.errorMessage)
      )))
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

  def validate(period: Period)( implicit request: DataRequest[AnyContent]): List[ValidationError] = {

    val validatedVatReturnRequest =
      vatReturnService.fromUserAnswers(request.userAnswers, request.vrn, period, request.registration)

    val validatedCorrectionRequest = request.userAnswers.get(CorrectPreviousReturnPage).map(_ =>
      correctionService.fromUserAnswers(request.userAnswers, request.vrn, period, request.registration.commencementDate))

    (validatedVatReturnRequest, validatedCorrectionRequest) match {
      case (Invalid(vatReturnErrors), Some(Invalid(correctionErrors))) =>
        (vatReturnErrors ++ correctionErrors).toChain.toList
      case (Invalid(errors), _) =>
        errors.toChain.toList
      case (_, Some(Invalid(errors))) =>
        errors.toChain.toList
      case _ => List.empty[ValidationError]
    }
  }

  def onSubmit(period: Period, incompletePromptShown: Boolean): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      val redirectToFirstError = getRedirect(validate(period), period).headOption

      (redirectToFirstError, incompletePromptShown) match {
        case (Some(redirect), true) => Future.successful(Redirect(redirect))
        case (Some(_), false) => Future.successful(Redirect(routes.CheckYourAnswersController.onPageLoad(period)))
        case _ =>
          val validatedVatReturnRequest =
            vatReturnService.fromUserAnswers(request.userAnswers, request.vrn, period, request.registration)

          val validatedCorrectionRequest = request.userAnswers.get(CorrectPreviousReturnPage).map(_ =>
            correctionService.fromUserAnswers(request.userAnswers, request.vrn, period, request.registration.commencementDate))

          (validatedVatReturnRequest, validatedCorrectionRequest) match {
            case (Valid(vatReturnRequest), Some(Valid(correctionRequest))) =>
              submitReturn(vatReturnRequest, Option(correctionRequest), period)
            case (Valid(vatReturnRequest), None) =>
              submitReturn(vatReturnRequest, None, period)
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

  def getRedirect(errors: List[ValidationError], period: Period): List[Call] = {
    errors.flatMap {
      case DataMissingError(AllSalesFromNiQuery) =>
        logger.error(s"Data missing - no data provided for NI sales")
        Some(routes.CountryOfConsumptionFromNiController.onPageLoad(CheckMode, period, Index(0)))
      case DataMissingError(VatRatesFromNiPage(index)) =>
        logger.error(s"Data missing - vat rates with index ${index.position}")
        Some(routes.VatRatesFromNiController.onPageLoad(CheckMode, period, index))
      case DataMissingError(NiSalesAtVatRateQuery(countryIndex, vatRateIndex)) =>
        logger.error(s"Data missing - net value of sales at vat rate ${vatRateIndex.position} for country ${countryIndex.position}")
        Some(routes.NetValueOfSalesFromNiController.onPageLoad(CheckMode, period, countryIndex, vatRateIndex))
      case DataMissingError(VatOnSalesFromNiQuery(countryIndex, vatRateIndex)) =>
        logger.error(s"Data missing - vat charged on sales at vat rate ${vatRateIndex.position} for country ${countryIndex.position}")
        Some(routes.VatOnSalesFromNiController.onPageLoad(CheckMode, period, countryIndex, vatRateIndex))

      case DataMissingError(AllSalesFromEuQuery) =>
        logger.error(s"Data missing - no data provided for EU sales")
        Some(routes.CountryOfSaleFromEuController.onPageLoad(CheckMode, period, Index(0)))
      case DataMissingError(AllSalesToEuQuery(countryFromIndex)) =>
        logger.error(s"Data missing - country of consumption from country ${countryFromIndex.position}")
        Some(routes.CountryOfConsumptionFromEuController.onPageLoad(CheckMode, period, countryFromIndex, Index(0)))
      case DataMissingError(VatRatesFromEuPage(countryFromIndex, countryToIndex)) =>
        logger.error(s"Data missing - vat rates for sales from country ${countryFromIndex.position} to country ${countryToIndex.position}")
        Some(routes.VatRatesFromEuController.onPageLoad(CheckMode, period, countryFromIndex, countryToIndex))
      case DataMissingError(EuSalesAtVatRateQuery(countryFromIndex, countryToIndex, vatRateIndex)) =>
        logger.error(s"Data missing - net value of sales from country ${countryFromIndex.position} to country " +
          s"${countryToIndex.position} at vat rate ${vatRateIndex.position} ")
        Some(routes.NetValueOfSalesFromEuController.onPageLoad(CheckMode, period, countryFromIndex, countryToIndex, vatRateIndex))
      case DataMissingError(VatOnSalesFromEuQuery(countryFromIndex, countryToIndex, vatRateIndex)) =>
        logger.error(s"Data missing - vat charged on sales from country ${countryFromIndex.position} to country " +
          s"${countryToIndex.position} at vat rate ${vatRateIndex.position} ")
        Some(routes.VatOnSalesFromEuController.onPageLoad(CheckMode, period, countryFromIndex, countryToIndex, vatRateIndex))

      case DataMissingError(AllCorrectionPeriodsQuery) =>
        logger.error(s"Data missing - no data provided for corrections")
        Some(correctionsRoutes.CorrectionReturnPeriodController.onPageLoad(CheckMode, period, Index(0)))
      case DataMissingError(AllCorrectionCountriesQuery(periodIndex)) =>
        logger.error(s"Data missing - no countries found for corrections to period ${periodIndex.position}")
        Some(correctionsRoutes.CorrectionCountryController.onPageLoad(CheckMode, period, periodIndex, Index(0)))
      case DataMissingError(CorrectionToCountryQuery(periodIndex, countryIndex)) =>
        logger.error(s"Data missing - correction to country ${countryIndex.position} in period ${periodIndex.position}")
        Some(correctionsRoutes.CountryVatCorrectionController.onPageLoad(CheckMode, period, periodIndex, countryIndex, false))

      case DataMissingError(_) =>
        logger.error(s"Unhandled DataMissingError")
        None
      case _ =>
        logger.error(s"Unhandled ValidationError")
        None
    }
  }

  def submitReturn(vatReturnRequest: VatReturnRequest, correctionRequest: Option[CorrectionRequest], period: Period)
                  (implicit request: DataRequest[AnyContent]): Future[Result] = {

    val submission: Future[Either[ErrorResponse, Product]] = correctionRequest match {
      case Some(cr) => vatReturnConnector.submitWithCorrections(VatReturnWithCorrectionRequest(vatReturnRequest, cr))
      case _ => vatReturnConnector.submit(vatReturnRequest)
    }

    submission.flatMap {
      case Right(vatReturn: VatReturn) =>
        auditEmailAndRedirect(vatReturnRequest, correctionRequest, vatReturn, period)
      case Right((vatReturn: VatReturn, _)) =>
        auditEmailAndRedirect(vatReturnRequest, correctionRequest, vatReturn, period)
      case Left(RegistrationNotFound) =>
        auditService.audit(ReturnsAuditModel.build(
          vatReturnRequest, correctionRequest, SubmissionResult.NotYetRegistered, None, None, request
        ))
        saveUserAnswersOnCoreError(period, routes.NoRegistrationFoundInCoreController.onPageLoad())
      case Left(ReceivedErrorFromCore) =>
        auditService.audit(ReturnsAuditModel.build(
          vatReturnRequest, correctionRequest, SubmissionResult.NotYetRegistered, None, None, request
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
          _ <- cachedVatReturnRepository.clear(request.userId, period)
          _ <- saveForLaterConnector.delete(period)
        } yield {
          Redirect(CheckYourAnswersPage.navigate(NormalMode, request.userAnswers))
        }
    }
  }

  private def saveUserAnswersOnCoreError(period: Period, redirectLocation: Call)(implicit request: DataRequest[AnyContent]): Future[Result] =
    Future.fromTry(request.userAnswers.set(SavedProgressPage, routes.CheckYourAnswersController.onPageLoad(period).url)).flatMap {
    updatedAnswers =>
      val s4LRequest = SaveForLaterRequest(updatedAnswers, request.vrn, period)

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