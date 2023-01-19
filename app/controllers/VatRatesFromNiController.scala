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
import forms.VatRatesFromNiFormProvider
import models.requests.DataRequest
import models.{Index, Mode, Period, UserAnswers, VatRate, VatRateAndSalesWithOptionalVat}
import pages.VatRatesFromNiPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.AllNiVatRateAndSalesWithOptionalVatQuery
import services.VatRateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax._
import views.html.VatRatesFromNiView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatRatesFromNiController @Inject()(
                                          cc: AuthenticatedControllerComponents,
                                          formProvider: VatRatesFromNiFormProvider,
                                          view: VatRatesFromNiView,
                                          vatRateService: VatRateService
                                        )(implicit ec: ExecutionContext)
  extends FrontendBaseController with SalesFromNiBaseController with VatRateBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>
      getCountryAsync(index) {
        country =>

          val vatRates = vatRateService.vatRates(period, country)
          val form = formProvider(vatRates)
          val currentValue = request.userAnswers.get(VatRatesFromNiPage(index))

          val preparedForm = currentValue match {
            case None => form
            case Some(value) => form.fill(value)
          }

          vatRates.size match {
            case 1 =>
              currentValue match {
                case Some(_) =>
                  Redirect(VatRatesFromNiPage(index).navigate(mode, request.userAnswers)).toFuture
                case _ =>
                  updateAndContinue(mode, index, request, vatRates.toList)
              }
            case _ =>
              Ok(view(preparedForm, mode, period, index, country, checkboxItems(vatRates))).toFuture
          }
      }
  }

  def onSubmit(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>
      getCountryAsync(index) {
        country =>

          val vatRates = vatRateService.vatRates(period, country)
          val form = formProvider(vatRates)

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, mode, period, index, country, checkboxItems(vatRates))).toFuture,

            value => {
              val existingAnswers = request.userAnswers.get(VatRatesFromNiPage(index)).getOrElse(List.empty)
              val existingAnswersWithRemovals = existingAnswers.filter(rate => value.contains(rate))
              val updated = existingAnswersWithRemovals ++ value.filterNot(rate => existingAnswersWithRemovals.contains(rate))
              updateAndContinue(mode, index, request, updated)
            }
          )
      }
  }

  def getUpdatedAnswers(countryIndex: Index, existingUserAnswers: UserAnswers, vatRates: List[VatRate]): Seq[VatRateAndSalesWithOptionalVat] = {
    val existingSales = existingUserAnswers.get(AllNiVatRateAndSalesWithOptionalVatQuery(countryIndex)).getOrElse(Seq())
    existingSales.filter(sales => vatRates.exists(_.rate == sales.rate)) ++ vatRates.filterNot(rate => existingSales.exists(_.rate == rate.rate))
      .map(VatRateAndSalesWithOptionalVat.convert)
  }

  private def updateAndContinue(mode: Mode, index: Index, request: DataRequest[AnyContent], value: List[VatRate]) = {
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(
        AllNiVatRateAndSalesWithOptionalVatQuery(index),
        getUpdatedAnswers(index, request.userAnswers, value)))
      _ <- cc.sessionRepository.set(updatedAnswers)
    } yield {
      Redirect(VatRatesFromNiPage(index).navigate(mode, updatedAnswers))
    }
  }
}
