/*
 * Copyright 2022 HM Revenue & Customs
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
import forms.VatRatesFromEuFormProvider
import models.requests.DataRequest
import models.{Index, Mode, Period, UserAnswers, VatRate, VatRateAndSales}
import pages.VatRatesFromEuPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.AllEuVatRateAndSalesQuery
import services.VatRateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax._
import views.html.VatRatesFromEuView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatRatesFromEuController @Inject()(
                                          cc: AuthenticatedControllerComponents,
                                          formProvider: VatRatesFromEuFormProvider,
                                          vatRateService: VatRateService,
                                          view: VatRatesFromEuView
                                        )(implicit ec: ExecutionContext)
  extends FrontendBaseController with VatRateBaseController with SalesFromEuBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, countryFromIndex: Index, countryToIndex: Index): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>
      getCountriesAsync(countryFromIndex, countryToIndex) {
        case (countryFrom, countryTo) =>

          val vatRates = vatRateService.vatRates(period, countryTo)
          val form = formProvider(vatRates)
          val currentValue = request.userAnswers.get(VatRatesFromEuPage(countryFromIndex, countryToIndex))

          val preparedForm = currentValue match {
            case None => form
            case Some(value) => form.fill(value)
          }

          vatRates.size match {
            case 1 =>
              currentValue match {
                case Some(_) =>
                  Redirect(VatRatesFromEuPage(countryFromIndex, countryToIndex).navigate(mode, request.userAnswers)).toFuture
                case _ =>
                  updateAndContinue(mode, countryFromIndex, countryToIndex, request, vatRates.toList)
              }
            case _ =>
              Ok(view(preparedForm, mode, period, countryFromIndex, countryToIndex, countryFrom, countryTo, checkboxItems(vatRates))).toFuture
          }
      }
  }

  def onSubmit(mode: Mode, period: Period, countryFromIndex: Index, countryToIndex: Index): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>
      getCountriesAsync(countryFromIndex, countryToIndex) {
        case (countryFrom, countryTo) =>

          val vatRates = vatRateService.vatRates(period, countryTo)
          val form = formProvider(vatRates)

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, mode, period, countryFromIndex, countryToIndex, countryFrom, countryTo, checkboxItems(vatRates))).toFuture,

            value => {
              val existingAnswers = request.userAnswers.get(VatRatesFromEuPage(countryFromIndex, countryToIndex)).getOrElse(List.empty)
              val existingAnswersWithRemovals = existingAnswers.filter(rate => value.contains(rate))
              val updated = existingAnswersWithRemovals ++ value.filterNot(rate => existingAnswersWithRemovals.contains(rate))
              updateAndContinue(mode, countryFromIndex, countryToIndex, request, updated)
            }
          )
      }
  }

  def getUpdatedAnswers(countryFromIndex: Index, countryToIndex: Index, existingUserAnswers: UserAnswers, vatRates: List[VatRate]): Seq[VatRateAndSales] = {
    val existingSales = existingUserAnswers.get(AllEuVatRateAndSalesQuery(countryFromIndex, countryToIndex)).getOrElse(Seq())
    existingSales.filter(sales => vatRates.exists(_.rate == sales.rate)) ++ vatRates.filterNot(rate => existingSales.exists(_.rate == rate.rate))
      .map(VatRateAndSales.convert)
  }

  private def updateAndContinue(mode: Mode, countryFromIndex: Index, countryToIndex: Index, request: DataRequest[AnyContent], value: List[VatRate]) = {
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(
        AllEuVatRateAndSalesQuery(countryFromIndex, countryToIndex),
        getUpdatedAnswers(countryFromIndex, countryToIndex, request.userAnswers, value)))
      _ <- cc.sessionRepository.set(updatedAnswers)
    } yield {
      Redirect(VatRatesFromEuPage(countryFromIndex, countryToIndex).navigate(mode, updatedAnswers))
    }
  }
}
