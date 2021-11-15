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
import forms.corrections.RemoveCountryCorrectionFormProvider
import models.{Index, Mode, Period}
import pages.corrections.RemoveCountryCorrectionPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.corrections._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.corrections.RemoveCountryCorrectionView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveCountryCorrectionController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: RemoveCountryCorrectionFormProvider,
                                       view: RemoveCountryCorrectionView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period) {
    implicit request =>

      val preparedForm = request.userAnswers.get(RemoveCountryCorrectionPage(periodIndex)) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, period, periodIndex, countryIndex))
  }

  def onSubmit(mode: Mode, period: Period, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, period, periodIndex, countryIndex))),

        value =>
          if (value) {
            Future.fromTry(request.userAnswers.remove(CorrectionToCountryQuery(periodIndex, countryIndex))).flatMap {
              updatedAnswers =>
                if (updatedAnswers.get(DeriveNumberOfCorrections(periodIndex)).getOrElse(0) == 0) {
                  Future.fromTry(updatedAnswers.remove(CorrectionPeriodQuery(periodIndex))).flatMap{
                    answersWithRemovedPeriod =>
                      if (answersWithRemovedPeriod.get(DeriveNumberOfCorrectionPeriods).getOrElse(0) == 0) {
                        for {
                          answersWithRemovedCorrections <- Future.fromTry(answersWithRemovedPeriod.remove(AllCorrectionPeriodsQuery))
                          _ <- cc.sessionRepository.set(answersWithRemovedCorrections)
                        } yield {
                          Redirect(RemoveCountryCorrectionPage(periodIndex).navigate(mode, answersWithRemovedCorrections))
                        }
                      } else {
                        for {
                          _ <- cc.sessionRepository.set(answersWithRemovedPeriod)
                        } yield {
                          Redirect(RemoveCountryCorrectionPage(periodIndex).navigate(mode, answersWithRemovedPeriod))
                        }
                      }
                  }
                } else {
                  for {
                    _ <- cc.sessionRepository.set(updatedAnswers)
                  } yield {
                    Redirect(RemoveCountryCorrectionPage(periodIndex).navigate(mode, updatedAnswers))
                  }
                }
            }
          } else {
              Future.successful( Redirect(RemoveCountryCorrectionPage(periodIndex).navigate(mode, request.userAnswers)))
            }
      )
  }
}
