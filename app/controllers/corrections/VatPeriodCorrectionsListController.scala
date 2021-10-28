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

import controllers.routes.IndexController
import controllers.actions._
import forms.corrections.VatPeriodCorrectionsListFormProvider
import models.Quarter._
import models.{Mode, NormalMode, Period}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.corrections.VatPeriodCorrectionsListView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatPeriodCorrectionsListController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: VatPeriodCorrectionsListFormProvider,
                                       view: VatPeriodCorrectionsListView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period) {
    implicit request =>

      // TODO: Get list of corrections / periods

      val periodList = Seq(
        Period(2021, Q1),
        Period(2021, Q2),
        Period(2021, Q3),
        Period(2021, Q4)
      )

      Ok(view(mode, period, periodList))
  }

  def onSubmit(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period) {
    implicit request => Redirect(IndexController.onPageLoad())
  }
}
