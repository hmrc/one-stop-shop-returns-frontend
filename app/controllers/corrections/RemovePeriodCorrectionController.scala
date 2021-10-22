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
import forms.corrections.RemovePeriodCorrectionFormProvider
import models.{Mode, Period}
import pages.corrections.RemovePeriodCorrectionPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.corrections.RemovePeriodCorrectionView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemovePeriodCorrectionController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: RemovePeriodCorrectionFormProvider,
                                       view: RemovePeriodCorrectionView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>

      val preparedForm = request.userAnswers.get(RemovePeriodCorrectionPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, period))
  }

  def onSubmit(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, period))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(RemovePeriodCorrectionPage, value))
            _              <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(RemovePeriodCorrectionPage.navigate(mode, updatedAnswers))
      )
  }
}
