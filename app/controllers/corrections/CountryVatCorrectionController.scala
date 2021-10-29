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
import forms.corrections.CountryVatCorrectionFormProvider
import models.{Index, Mode, Period}
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, CountryVatCorrectionPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.corrections.CountryVatCorrectionView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CountryVatCorrectionController @Inject()(
                                        cc: AuthenticatedControllerComponents,
                                        formProvider: CountryVatCorrectionFormProvider,
                                        view: CountryVatCorrectionView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period) {
    implicit request =>
      val correctionPeriod = request.userAnswers.get(CorrectionReturnPeriodPage(periodIndex))
      val selectedCountry = request.userAnswers.get(CorrectionCountryPage(periodIndex, countryIndex))
      (correctionPeriod, selectedCountry) match {
        case (Some(correctionPeriod), Some(country)) =>
            val form = formProvider(country.name)

            val preparedForm = request.userAnswers.get(CountryVatCorrectionPage) match {
              case None => form
              case Some(value) => form.fill(value)
            }
            Ok(view(preparedForm, mode, period, country, correctionPeriod, periodIndex, countryIndex))

      }

  }

  def onSubmit(mode: Mode, period: Period, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period).async {
    implicit request =>
      val selectedCountry = request.userAnswers.get(CorrectionCountryPage(periodIndex, countryIndex))
      val correctionPeriod = request.userAnswers.get(CorrectionReturnPeriodPage(periodIndex))

      (correctionPeriod, selectedCountry) match {
        case (Some(correctionPeriod), Some(country)) =>
          val form = formProvider(country.name)
          form.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, mode, period, country, correctionPeriod, periodIndex, countryIndex))),

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(CountryVatCorrectionPage, value))
                _              <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(CountryVatCorrectionPage.navigate(mode, updatedAnswers))
          )
      }
  }
}
