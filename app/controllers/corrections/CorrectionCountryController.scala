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

import connectors.VatReturnConnector
import controllers.actions._
import forms.corrections.CorrectionCountryFormProvider
import models.Quarter.Q3
import models.{Index, Mode, Period}
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.AllSalesFromEuQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.corrections.CorrectionCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CorrectionCountryController @Inject()(
                                        cc: AuthenticatedControllerComponents,
                                        formProvider: CorrectionCountryFormProvider,
                                        vatReturnConnector: VatReturnConnector,
                                        view: CorrectionCountryView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period) {
    implicit request =>
      val form = formProvider(index,
        request.userAnswers
          .get(CorrectionCountryPage).toSeq)

      val preparedForm = request.userAnswers.get(CorrectionCountryPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      request.userAnswers.get(CorrectionReturnPeriodPage(index)) match {
        case Some(correctionPeriod) => Ok(view(preparedForm, mode, period, index, correctionPeriod))
        case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad().url)
      }
  }

  def onSubmit(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period).async {
    implicit request =>
      val form = formProvider(index,
        request.userAnswers
          .get(CorrectionCountryPage).toSeq)

      form.bindFromRequest().fold(
        formWithErrors =>
          request.userAnswers.get(CorrectionReturnPeriodPage(index)) match {
            case Some(correctionPeriod) => Future.successful(BadRequest(view(formWithErrors, mode, period, index, correctionPeriod)))
            case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad().url))
          },

        value =>
          request.userAnswers.get(CorrectionReturnPeriodPage(index)) match{
            case Some(correctionPeriod) =>
              for {
                          updatedAnswers <- Future.fromTry(request.userAnswers.set(CorrectionCountryPage, value))
                          vatReturnResult <- vatReturnConnector.get(period)
                          _              <- cc.sessionRepository.set(updatedAnswers)
                        } yield {
                vatReturnResult match {
                  case Right(vatReturn) => {
                    val countriesFromNi = vatReturn.salesFromNi.map(sales => sales.countryOfConsumption)
                    Redirect(CorrectionCountryPage.navigate(mode, updatedAnswers, countriesFromNi))
                  }
                  case _ => ???
                }
                }
            case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad().url))
          }

      )
  }
}
