/*
 * Copyright 2023 HM Revenue & Customs
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

import controllers.actions._
import forms.SoldGoodsFromNiFormProvider
import models.{Mode, Period, StandardPeriod, UserAnswers}
import pages.SoldGoodsFromNiPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PartialReturnPeriodService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SoldGoodsFromNiView

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SoldGoodsFromNiController @Inject()(
                                           cc: AuthenticatedControllerComponents,
                                           formProvider: SoldGoodsFromNiFormProvider,
                                           view: SoldGoodsFromNiView,
                                           partialReturnPeriodService: PartialReturnPeriodService,
                                           clock: Clock
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetOptionalData(period).async {
    implicit request =>

      val preparedForm = request.userAnswers.getOrElse(UserAnswers(request.userId, StandardPeriod.fromPeriod(period))).get(SoldGoodsFromNiPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      partialReturnPeriodService.getPartialReturnPeriod(request.registration, period).map { maybePartialReturnPeriod =>
        Ok(view(preparedForm, mode, maybePartialReturnPeriod.getOrElse(period)))
      }

  }

  def onSubmit(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetOptionalData(period).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, period))),

        value => {

          for {
            maybePartialReturnPeriod <- partialReturnPeriodService.getPartialReturnPeriod(request.registration, period)
            settingPeriod = maybePartialReturnPeriod.getOrElse(period)
            updatedAnswers <- Future.fromTry(request.userAnswers
              .getOrElse(UserAnswers(
                userId = request.userId,
                period = settingPeriod,
                lastUpdated = Instant.now(clock))
              ).set(SoldGoodsFromNiPage, value))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(SoldGoodsFromNiPage.navigate(mode, updatedAnswers))
        }
      )
  }
}
