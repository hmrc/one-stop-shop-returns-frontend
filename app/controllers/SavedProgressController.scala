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

import config.FrontendAppConfig
import connectors.{SaveForLaterConnector, SavedUserAnswers}
import controllers.actions._
import logging.Logging
import models.Period
import models.requests.SaveForLaterRequest
import models.responses.ConflictFound
import pages.SavedProgressPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.external.ExternalReturnUrlQuery
import repositories.SessionRepository
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
                                         connector: SaveForLaterConnector,
                                         appConfig: FrontendAppConfig,
                                         sessionRepository: SessionRepository
                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period, continueUrl: String): Action[AnyContent] = cc.authAndGetDataSimple(period).async {
    implicit request =>
      val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
      val answersExpiry = request.userAnswers.lastUpdated.plus(appConfig.saveForLaterTtl, ChronoUnit.DAYS)
        .atZone(ZoneId.systemDefault()).toLocalDate.format(dateTimeFormatter)
      Future.fromTry(request.userAnswers.set(SavedProgressPage, continueUrl)).flatMap {
        updatedAnswers =>
          val s4LRequest = SaveForLaterRequest(updatedAnswers, request.vrn, period)
          (for{
            sessionData <- sessionRepository.get(request.userId)
            s4laterResult <- connector.submit(s4LRequest)
          } yield {
            val externalUrl = sessionData.headOption.flatMap(_.get[String](ExternalReturnUrlQuery.path))
            (s4laterResult, externalUrl)
          }).flatMap {
            case (Right(Some(_: SavedUserAnswers)), externalUrl) =>
              for {
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield {
                Ok(view(period, answersExpiry, continueUrl, externalUrl))
              }
            case (Left(ConflictFound), externalUrl)=>
              Future.successful(Redirect(externalUrl.getOrElse(routes.YourAccountController.onPageLoad().url)))
            case (Left(e), _) =>
              logger.error(s"Unexpected result on submit: ${e.toString}")
              Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
            case (Right(None), _) =>
              logger.error(s"Unexpected result on submit")
              Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          }
      }
  }
}
