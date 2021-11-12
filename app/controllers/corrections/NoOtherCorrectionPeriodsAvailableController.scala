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

package controllers.corrections

import controllers.actions._
import controllers.{routes => baseRoutes}
import logging.Logging
import models.Period
import pages.corrections.CorrectPreviousReturnPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.corrections.DeriveCompletedCorrectionPeriods
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.corrections.NoOtherCorrectionPeriodsAvailableView

import javax.inject.Inject

class NoOtherCorrectionPeriodsAvailableController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       view: NoOtherCorrectionPeriodsAvailableView
                                     ) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period) {
    implicit request =>
      Ok(view(period))
  }

  def onSubmit(period: Period): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period) {
    implicit request =>
      val completedCorrectionPeriods: List[Period] = request.userAnswers.get(DeriveCompletedCorrectionPeriods).getOrElse(List.empty)

      if(completedCorrectionPeriods.isEmpty) {
        val cleanUp = request.userAnswers.remove(CorrectPreviousReturnPage)
        if (cleanUp.isFailure) {
          logger.error("Could not remove answer for CorrectPreviousReturnPage")
        }
      }

      Redirect(baseRoutes.CheckYourAnswersController.onPageLoad(period))
  }
}
