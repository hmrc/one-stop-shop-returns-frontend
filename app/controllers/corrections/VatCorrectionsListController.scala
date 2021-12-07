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
import forms.corrections.VatCorrectionsListFormProvider
import models.{Country, Index, Mode, Period}
import pages.corrections.VatCorrectionsListPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.corrections.VatCorrectionsListSummary
import views.html.corrections.VatCorrectionsListView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class VatCorrectionsListController @Inject()(
                                              cc: AuthenticatedControllerComponents,
                                              formProvider: VatCorrectionsListFormProvider,
                                              view: VatCorrectionsListView
                                            )(implicit ec: ExecutionContext) extends FrontendBaseController with VatCorrectionsBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc
  private val form = formProvider()

  def onPageLoad(mode: Mode, period: Period, periodIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible(period) {
    implicit request =>
      getNumberOfCorrections(periodIndex) { (number, correctionPeriod) =>

        val canAddCountries = number < Country.euCountries.size
        val list = VatCorrectionsListSummary.addToListRows(request.userAnswers, mode, periodIndex)

        Ok(view(form, mode, list, period, correctionPeriod, periodIndex, canAddCountries))
      }

  }

  def onSubmit(mode: Mode, period: Period, periodIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible(period) {
    implicit request =>
      getNumberOfCorrections(periodIndex) { (number, correctionPeriod) =>

        val canAddCountries = number < Country.euCountries.size

        form.bindFromRequest().fold(
          formWithErrors => {
            val list = VatCorrectionsListSummary.addToListRows(request.userAnswers, mode, periodIndex)
            BadRequest(view(formWithErrors, mode, list, period, correctionPeriod, periodIndex, canAddCountries))
          },
          value =>
            Redirect(VatCorrectionsListPage(periodIndex).navigate(request.userAnswers, mode, value))
        )
      }
  }
}
