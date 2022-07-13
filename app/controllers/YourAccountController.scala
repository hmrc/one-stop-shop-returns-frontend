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

import connectors.financialdata.FinancialDataConnector
import connectors.{ReturnStatusConnector, SaveForLaterConnector}
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.financialdata.{CurrentPayments, PaymentStatus}
import models.requests.RegistrationRequest
import models.{Period, UserAnswers}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.UserAnswersRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.yourAccount._
import views.html.IndexView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YourAccountController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       returnStatusConnector: ReturnStatusConnector,
                                       financialDataConnector: FinancialDataConnector,
                                       saveForLaterConnector: SaveForLaterConnector,
                                       view: IndexView,
                                       sessionRepository: UserAnswersRepository
                                     )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>
      val results = getCurrentReturnsAndFinancialDataAndUserAnswers()
      results.flatMap {
        case (Right(availablePeriodsWithStatus), Right(vatReturnsWithFinancialData), answers) =>
          prepareViewWithFinancialData(availablePeriodsWithStatus, vatReturnsWithFinancialData, answers.map(_.period))
        case (Right(availablePeriodsWithStatus), Left(error), answers) =>
          logger.warn(s"There was an error with getting payment information $error")
          prepareViewWithNoFinancialData(availablePeriodsWithStatus, answers.map(_.period))
        case (Left(error), Left(error2), _) =>
          logger.error(s"there was an error with period with status $error and getting periods with outstanding amounts $error2")
          throw new Exception(error.toString)
        case (Left(error), _, _) =>
          logger.error(s"there was an error during period with status $error")
          throw new Exception(error.toString)
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

  private def prepareViewWithFinancialData(returnsViewModel: Seq[Return],
                                           currentPayments: CurrentPayments,
                                           periodInProgress: Option[Period])(implicit request: RegistrationRequest[AnyContent]) = {

    Future.successful(Ok(view(
      request.registration.registeredCompanyName,
      request.vrn.vrn,
      ReturnsViewModel(
        returnsViewModel.map(currentReturn => if (periodInProgress.contains(currentReturn.period)) {
          currentReturn.copy(inProgress = true)
        } else {
          currentReturn
        })),
      PaymentsViewModel(currentPayments.duePayments, currentPayments.overduePayments),
      (currentPayments.overduePayments ++ currentPayments.duePayments).exists(_.paymentStatus == PaymentStatus.Unknown)
    )))
  }

  private def prepareViewWithNoFinancialData(returnsViewModel: Seq[Return], periodInProgress: Option[Period])
                                            (implicit request: RegistrationRequest[AnyContent]) = {
    Future.successful(Ok(view(
      request.registration.registeredCompanyName,
      request.vrn.vrn,
      ReturnsViewModel(
        returnsViewModel.map(currentReturn => if (periodInProgress.contains(currentReturn.period)) {
          currentReturn.copy(inProgress = true)
        } else {
          currentReturn
        })),
      PaymentsViewModel(Seq.empty, Seq.empty),
      paymentError = true
    )))
  }

}
