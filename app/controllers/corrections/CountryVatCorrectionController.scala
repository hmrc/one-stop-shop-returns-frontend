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
import forms.corrections.CountryVatCorrectionFormProvider
import models.domain.{SalesToCountry, VatReturn}
import models.{CheckMode, Country, Index, Mode, NormalMode, Period}
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, CountryVatCorrectionPage}
import play.api.i18n.I18nSupport
import play.api.i18n.Lang.logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.VatReturnService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.corrections.CountryVatCorrectionView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CountryVatCorrectionController @Inject()(
                                        cc: AuthenticatedControllerComponents,
                                        formProvider: CountryVatCorrectionFormProvider,
                                        service: VatReturnService,
                                        view: CountryVatCorrectionView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period).async {
    implicit request =>
      val correctionPeriod = request.userAnswers.get(CorrectionReturnPeriodPage(periodIndex))
      val selectedCountry = request.userAnswers.get(CorrectionCountryPage(periodIndex, countryIndex))
      (correctionPeriod, selectedCountry) match {
        case (Some(correctionPeriod), Some(country)) =>
          for{
            vatOwedToCountryOnPrevReturn <- service.getVatOwedToCountryOnReturn(country, correctionPeriod)
          }yield{
            val form = formProvider(country.name)

            val preparedForm = request.userAnswers.get(CountryVatCorrectionPage(periodIndex, countryIndex)) match {
              case None => form
              case Some(value) => form.fill(value)
            }
            Ok(view(preparedForm, mode, period, country, correctionPeriod, periodIndex, countryIndex, vatOwedToCountryOnPrevReturn))
          }
        case _ => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  def onSubmit(mode: Mode, period: Period, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period).async {
    implicit request =>
      val selectedCountry = request.userAnswers.get(CorrectionCountryPage(periodIndex, countryIndex))
      val correctionPeriod = request.userAnswers.get(CorrectionReturnPeriodPage(periodIndex))
      (correctionPeriod, selectedCountry) match {
        case (Some(correctionPeriod), Some(country)) =>
          service.getVatOwedToCountryOnReturn(country, correctionPeriod).flatMap { vatOwedToCountryOnPrevReturn =>
              val form = formProvider(country.name)
              form.bindFromRequest().fold(
                formWithErrors =>
                  Future.successful(BadRequest(view(formWithErrors, mode, period, country, correctionPeriod, periodIndex, countryIndex, vatOwedToCountryOnPrevReturn))),

                value =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(CountryVatCorrectionPage(periodIndex, countryIndex), value))
                    _              <- cc.sessionRepository.set(updatedAnswers)
                  } yield Redirect(CountryVatCorrectionPage(periodIndex, countryIndex).navigate(mode, updatedAnswers))
              )

          }
        case _ => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

      }
  }


}
