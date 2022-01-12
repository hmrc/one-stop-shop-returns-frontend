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
import config.FrontendAppConfig
import connectors.{SaveForLaterConnector, SavedUserAnswers}
import controllers.actions._
import logging.Logging
import models.Period
import models.responses.ConflictFound
import pages.SavedProgressPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveForLaterService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SavedProgressView

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SavedProgressController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       view: SavedProgressView,
                                       service: SaveForLaterService,
                                       connector: SaveForLaterConnector,
                                       appConfig: FrontendAppConfig
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport  with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period, continueUrl: String): Action[AnyContent] = cc.authAndGetDataSimple(period).async {
    implicit request =>
      val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
      val answersExpiry = request.userAnswers.lastUpdated.plus(appConfig.saveForLaterTtl, ChronoUnit.DAYS)
        .atZone(ZoneId.systemDefault()).toLocalDate.format(dateTimeFormatter)
      Future.fromTry(request.userAnswers.set(SavedProgressPage, continueUrl)).flatMap {
        updatedAnswers =>
          val validatedS4LRequest = service.fromUserAnswers(updatedAnswers, request.vrn, period)
          validatedS4LRequest match {
            case Valid(s4LRequest) =>
              connector.submit(s4LRequest).flatMap {
                case Right(_ : SavedUserAnswers) =>
                  for {
                    _ <- cc.sessionRepository.clear(request.userId)
                  } yield {
                    Ok(view(period, answersExpiry, continueUrl))
                  }
                case Left(ConflictFound) =>
                  Future.successful(Redirect(routes.YourAccountController.onPageLoad()))
                case Left(e) =>
                  logger.error(s"Unexpected result on submit: ${e.toString}")
                  Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
              }
            case Invalid(errors) =>
              val errorList = errors.toChain.toList
              val errorMessages = errorList.map(_.errorMessage).mkString("\n")
              logger.error(s"Unable to save user answers: $errorMessages")
              Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          }
      }
  }
}
