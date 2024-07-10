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

import config.Constants.{exclusionCodeSixFollowingMonth, exclusionCodeSixTenthOfMonth}
import config.FrontendAppConfig
import connectors.financialdata.FinancialDataConnector
import connectors.{RegistrationConnector, ReturnStatusConnector, SaveForLaterConnector, VatReturnConnector}
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.Period.getPeriod
import models.exclusions.ExcludedTrader
import models.exclusions.ExcludedTrader._
import models.exclusions.ExclusionReason.{NoLongerSupplies, TransferringMSID, VoluntarilyLeaves}
import models.financialdata.{CurrentPayments, Payment, PaymentStatus}
import models.registration.RegistrationWithFixedEstablishment
import models.requests.RegistrationRequest
import models.{Period, UserAnswers}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.UserAnswersRepository
import services.exclusions.ExclusionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import utils.ReturnsUtils
import viewmodels.yourAccount._
import views.html.IndexView

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YourAccountController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       exclusionService: ExclusionService,
                                       returnStatusConnector: ReturnStatusConnector,
                                       financialDataConnector: FinancialDataConnector,
                                       saveForLaterConnector: SaveForLaterConnector,
                                       vatReturnConnector: VatReturnConnector,
                                       registrationConnector: RegistrationConnector,
                                       view: IndexView,
                                       sessionRepository: UserAnswersRepository,
                                       frontendAppConfig: FrontendAppConfig,
                                       clock: Clock
                                     )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  implicit val c: Clock = clock

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>

      if (frontendAppConfig.amendRegistrationEnabled) {
        if (request.registration.vatDetails.partOfVatGroup && hasFixedEstablishment()) {
          Redirect(frontendAppConfig.deleteAllFixedEstablishmentUrl).toFuture
        } else {
          normalView()
        }
      } else {
        normalView()
      }
  }

  private def normalView()(implicit request: RegistrationRequest[AnyContent]): Future[Result] = {
    val results = getCurrentReturnsAndFinancialDataAndUserAnswers()

    results.flatMap {
      case (Right(availablePeriodsWithStatus), Right(vatReturnsWithFinancialData), answers) =>
        prepareViewWithFinancialData(
          availablePeriodsWithStatus.returns, availablePeriodsWithStatus.excludedReturns,
          vatReturnsWithFinancialData, answers.map(_.period)
        )
      case (Right(availablePeriodsWithStatus), Left(error), answers) =>
        logger.warn(s"There was an error with getting payment information $error")
        prepareViewWithNoFinancialData(availablePeriodsWithStatus.returns, availablePeriodsWithStatus.excludedReturns,
          answers.map(_.period))
      case (Left(error), Left(error2), _) =>
        logger.error(s"there was an error with period with status $error and getting periods with outstanding amounts $error2")
        throw new Exception(error.toString)
      case (Left(error), _, _) =>
        logger.error(s"there was an error during period with status $error")
        throw new Exception(error.toString)
    }
  }

  private def hasFixedEstablishment()(implicit request: RegistrationRequest[AnyContent]): Boolean = {
    request.registration.euRegistrations.exists {
      case _: RegistrationWithFixedEstablishment => true
      case _ => false
    }
  }

  private def getCurrentReturnsAndFinancialDataAndUserAnswers()(implicit request: RegistrationRequest[AnyContent]) = {
    for {
      currentReturns <- returnStatusConnector.getCurrentReturns(request.vrn)
      currentPayments <- financialDataConnector.getCurrentPayments(request.vrn)
      userAnswers <- getSavedAnswers()
    } yield {
      userAnswers.map(answers => sessionRepository.set(answers))
      (currentReturns, currentPayments, userAnswers)
    }
  }

  private def getSavedAnswers()(implicit request: RegistrationRequest[AnyContent]): Future[Option[UserAnswers]] = {
    for {
      answersInSession <- sessionRepository.get(request.userId)
      savedForLater <- saveForLaterConnector.get()
    } yield {
      val latestInSession = answersInSession.sortBy(_.lastUpdated).headOption
      val answers = if (latestInSession.isEmpty) {
        savedForLater match {
          case Right(Some(answers)) => Some(UserAnswers(request.userId, answers.period, answers.data, answers.lastUpdated))
          case _ => None
        }
      } else {
        latestInSession
      }
      answers
    }
  }

  private def prepareViewWithFinancialData(nonExcludedReturns: Seq[Return],
                                           excludedReturns: Seq[Return],
                                           currentPayments: CurrentPayments,
                                           periodInProgress: Option[Period])(implicit request: RegistrationRequest[AnyContent]): Future[Result] = {

    val excludedTraderOpt = request.registration.excludedTrader
    for {
      hasSubmittedFinalReturn <- exclusionService.hasSubmittedFinalReturn(request.registration)
      currentReturnIsFinal <- checkCurrentReturn(nonExcludedReturns)
      canCancel <- canCancelRequestToLeave(request.registration.excludedTrader)
      hasDeregisteredFromVat <- checkDeregisteredTrader(registrationConnector)
      returns = nonExcludedReturns.map(currentReturn => if (periodInProgress.contains(currentReturn.period)) {
        currentReturn.copy(inProgress = true)
      } else {
        currentReturn
      })
    } yield {
      val hasDueReturnThreeYearsOld = ReturnsUtils.hasReturnThreeYearsOld(excludedReturns)
      val hasDueReturnsLessThanThreeYearsOld = ReturnsUtils.hasDueReturnsLessThanThreeYearsOld(returns)

      Ok(view(
        request.registration.registeredCompanyName,
        request.vrn.vrn,
        ReturnsViewModel(returns, excludedReturns),
        PaymentsViewModel(
          currentPayments.duePayments, currentPayments.overduePayments, currentPayments.excludedPayments, hasDueReturnThreeYearsOld
        ),
        (currentPayments.overduePayments ++ currentPayments.duePayments).exists(_.paymentStatus == PaymentStatus.Unknown),
        excludedTraderOpt,
        hasSubmittedFinalReturn,
        currentReturnIsFinal,
        frontendAppConfig.amendRegistrationEnabled,
        frontendAppConfig.changeYourRegistrationUrl,
        request.registration.excludedTrader.fold(false)(_.hasRequestedToLeave),
        exclusionService.getLink(
          exclusionService.calculateExclusionViewType(
            request.registration.excludedTrader, canCancel, hasSubmittedFinalReturn,
            hasDueReturnsLessThanThreeYearsOld = hasDueReturnsLessThanThreeYearsOld,
            hasDueReturnThreeYearsOld = hasDueReturnThreeYearsOld,
            hasDeregisteredFromVat
          ),
        ),
        hasDueReturnThreeYearsOld,
        hasDueReturnsLessThanThreeYearsOld,
        hasDeregisteredFromVat
      ))
    }
  }

  private def prepareViewWithNoFinancialData(returnsViewModel: Seq[Return],
                                             excludedReturns: Seq[Return],
                                             periodInProgress: Option[Period])
                                            (implicit request: RegistrationRequest[AnyContent]): Future[Result] = {

    val excludedTraderOpt = request.registration.excludedTrader
    for {
      hasSubmittedFinalReturn <- exclusionService.hasSubmittedFinalReturn(request.registration)
      currentReturnIsFinal <- checkCurrentReturn(returnsViewModel)
      canCancel <- canCancelRequestToLeave(request.registration.excludedTrader)
      hasDeregisteredFromVat <- checkDeregisteredTrader(registrationConnector)
      returns = returnsViewModel.map { currentReturn =>
          if (periodInProgress.contains(currentReturn.period)) {
            currentReturn.copy(inProgress = true)
          } else {
            currentReturn
          }
        }
    } yield {
      val hasDueReturnThreeYearsOld = ReturnsUtils.hasReturnThreeYearsOld(excludedReturns)
      val hasDueReturnsLessThanThreeYearsOld = ReturnsUtils.hasDueReturnsLessThanThreeYearsOld(returns)

      Ok(view(
        request.registration.registeredCompanyName,
        request.vrn.vrn,
        ReturnsViewModel(returns, excludedReturns),
        PaymentsViewModel(Seq.empty[Payment], Seq.empty[Payment], Seq.empty[Payment], hasDueReturnThreeYearsOld = true), //true = no effect
        paymentError = true,
        excludedTraderOpt,
        hasSubmittedFinalReturn,
        currentReturnIsFinal,
        frontendAppConfig.amendRegistrationEnabled,
        frontendAppConfig.changeYourRegistrationUrl,
        request.registration.excludedTrader.fold(false)(_.hasRequestedToLeave),
        exclusionService.getLink(
          exclusionService.calculateExclusionViewType(
            request.registration.excludedTrader, canCancel, hasSubmittedFinalReturn,
            hasDueReturnsLessThanThreeYearsOld = hasDueReturnsLessThanThreeYearsOld,
            hasDueReturnThreeYearsOld = hasDueReturnThreeYearsOld,
            hasDeregisteredFromVat
          ),
        ),
        hasDueReturnThreeYearsOld,
        hasDueReturnsLessThanThreeYearsOld,
        hasDeregisteredFromVat
      ))
    }
  }

  private def checkCurrentReturn(returnsViewModel: Seq[Return])(implicit request: RegistrationRequest[AnyContent]): Future[Boolean] = {
    if (returnsViewModel.isEmpty) {
      Future.successful(false)
    } else {
      exclusionService.currentReturnIsFinal(request.registration, returnsViewModel.minBy(_.period.firstDay.toEpochDay).period)
    }
  }

  private def canCancelRequestToLeave(maybeExcludedTrader: Option[ExcludedTrader])(implicit hc: HeaderCarrier): Future[Boolean] = {
    val now: LocalDate = LocalDate.now(c)

    maybeExcludedTrader match {
      case Some(excludedTrader) if TransferringMSID == excludedTrader.exclusionReason &&
        todayIsEqualToOrBeforeTenthOfFollowingMonth(excludedTrader.effectiveDate, now) =>

        val currentPeriod: Period = getPeriod(now)

        if (excludedTrader.finalReturnPeriod == currentPeriod) {
          true.toFuture
        } else {
          checkVatReturnSubmissionStatus(excludedTrader)
        }

      case Some(excludedTrader) if Seq(NoLongerSupplies, VoluntarilyLeaves).contains(excludedTrader.exclusionReason) &&
        now.isBefore(excludedTrader.effectiveDate) =>
        true.toFuture

      case _ =>
        false.toFuture
    }
  }

  private def todayIsEqualToOrBeforeTenthOfFollowingMonth(effectiveDate: LocalDate, now: LocalDate): Boolean = {

    val tenthOfFollowingMonth = effectiveDate
      .plusMonths(exclusionCodeSixFollowingMonth)
      .withDayOfMonth(exclusionCodeSixTenthOfMonth)
    now.isBefore(tenthOfFollowingMonth) || now.isEqual(tenthOfFollowingMonth)
  }

  private def checkVatReturnSubmissionStatus(excludedTrader: ExcludedTrader)(implicit hc: HeaderCarrier): Future[Boolean] = {
    vatReturnConnector.getSubmittedVatReturns().map { submittedVatReturnsResponse =>
      val periods = submittedVatReturnsResponse.map(_.period)

      !periods.contains(excludedTrader.finalReturnPeriod)
    }
  }

  private def checkDeregisteredTrader(connector: RegistrationConnector)(implicit hc: HeaderCarrier): Future[Boolean] = {
    connector.getVatCustomerInfo().map {
      case Right(vatInfo) if vatInfo.deregistrationDecisionDate.exists(!_.isAfter(LocalDate.now(clock))) =>
        true
      case _ =>
        false
    }
  }
}