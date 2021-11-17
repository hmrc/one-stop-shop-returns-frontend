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
import forms.VatPayableForCountryFormProvider
import models.{CheckMode, Index, Mode, NormalMode, Period}
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, CountryVatCorrectionPage, VatPayableForCountryPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.VatReturnService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.corrections.VatPayableForCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatPayableForCountryController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: VatPayableForCountryFormProvider,
                                       vatReturnService: VatReturnService,
                                       view: VatPayableForCountryView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period).async {
    implicit request =>

      val correctionPeriod = request.userAnswers.get(CorrectionReturnPeriodPage(periodIndex))
      val selectedCountry = request.userAnswers.get(CorrectionCountryPage(periodIndex, countryIndex))
      val correctionAmount = request.userAnswers.get(CountryVatCorrectionPage(periodIndex, countryIndex))

      (correctionPeriod, selectedCountry, correctionAmount) match {
        case (Some(correctionPeriod), Some(country), Some(amount)) =>
          for {
            vatOwedToCountryOnPrevReturn <- vatReturnService.getVatOwedToCountryOnReturn(country, correctionPeriod)
          } yield {
            val preparedForm = request.userAnswers.get(VatPayableForCountryPage(periodIndex, countryIndex)) match {
              case None => form
              case Some(value) => form.fill(value)
            }

            val newAmount = vatOwedToCountryOnPrevReturn + amount

            Ok(view(preparedForm, mode, period, periodIndex, countryIndex, country, correctionPeriod, newAmount))
          }
        case _ => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  def onSubmit(mode: Mode, period: Period, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      val correctionPeriod = request.userAnswers.get(CorrectionReturnPeriodPage(periodIndex))
      val selectedCountry = request.userAnswers.get(CorrectionCountryPage(periodIndex, countryIndex))
      val correctionAmount = request.userAnswers.get(CountryVatCorrectionPage(periodIndex, countryIndex))
      (correctionPeriod, selectedCountry, correctionAmount) match {
        case (Some(correctionPeriod), Some(country), Some(amount)) =>
            vatReturnService.getVatOwedToCountryOnReturn(country, correctionPeriod).flatMap {
              vatOwedToCountryOnPrevReturn =>
              val newAmount = vatOwedToCountryOnPrevReturn + amount

              form.bindFromRequest().fold(
                formWithErrors =>
                  Future.successful(BadRequest(view(formWithErrors, mode, period, periodIndex, countryIndex, country, correctionPeriod, newAmount))),

                value =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(VatPayableForCountryPage(periodIndex, countryIndex), value))
                  } yield Redirect(VatPayableForCountryPage(periodIndex, countryIndex).navigate(mode, updatedAnswers))
              )
            }
        case _ => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
      }
}
