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

import com.typesafe.play.cachecontrol.Seconds
import config.FrontendAppConfig
import controllers.actions._
import formats.Format.dateTimeFormatter

import javax.inject.Inject
import models.Period
import pages.SavedProgressPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SavedProgressView

import java.time.{LocalDate, ZoneId}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

class SavedProgressController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       view: SavedProgressView,
                                       appConfig: FrontendAppConfig
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period, continueUrl: String): Action[AnyContent] = cc.authAndGetDataSimple(period).async {
    implicit request =>
      val answersExpiry = request.userAnswers.lastUpdated.plus(appConfig.cacheTtl, ChronoUnit.SECONDS)
        .atZone(ZoneId.systemDefault()).toLocalDate.format(dateTimeFormatter)
      for {
        updatedAnswers <- Future.fromTry(request.userAnswers.set(SavedProgressPage, continueUrl))
        _              <- cc.sessionRepository.set(updatedAnswers)
      } yield{
        Ok(view(period, answersExpiry, continueUrl))
      }
  }
}
