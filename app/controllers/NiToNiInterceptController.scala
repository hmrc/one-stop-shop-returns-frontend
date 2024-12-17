/*
 * Copyright 2024 HM Revenue & Customs
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

import javax.inject.Inject
import models.{Country, Index, NormalMode, Period, UserAnswers}
import pages.{SoldGoodsFromEuPage, SoldGoodsFromNiPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.{AllSalesFromEuQueryWithOptionalVatQuery, AllSalesFromNiWithOptionalVatQuery, SalesFromNiQuery}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.NiToNiInterceptView

import scala.concurrent.{ExecutionContext, Future}

class NiToNiInterceptController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       view: NiToNiInterceptView,
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>

      Ok(view(request.userAnswers.period))
  }

  def onSubmit(period: Period): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      val maybeNiSaleWithIndex = request.userAnswers.get(AllSalesFromNiWithOptionalVatQuery).getOrElse(List.empty).zipWithIndex
        .find {
          case (salesFromCountryWithOptionalVat, _) => salesFromCountryWithOptionalVat.countryOfConsumption.code == Country.northernIreland.code
        }

      maybeNiSaleWithIndex match {
        case Some(_ @ (_, index)) =>
          for {
            updatedReturn <- Future.fromTry(request.userAnswers.remove(SalesFromNiQuery(Index(index))))
            _ <- cc.sessionRepository.set(updatedReturn)
          } yield {
            val updatedUserAnswers = updatedReturn
            navigate(period, updatedUserAnswers)
          }
        case _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  private def navigate(period: Period, userAnswers: UserAnswers): Result = {
    val soldGoodsToNi = userAnswers.get(SoldGoodsFromNiPage)
    val numberOfSalesFromNi = userAnswers.get(AllSalesFromNiWithOptionalVatQuery).getOrElse(List.empty)
    val soldGoodsToEu = userAnswers.get(SoldGoodsFromEuPage)
    val numberOfSalesFromEu = userAnswers.get(AllSalesFromEuQueryWithOptionalVatQuery).getOrElse(List.empty)

    (soldGoodsToNi,numberOfSalesFromNi,soldGoodsToEu, numberOfSalesFromEu) match {
      case (Some(true), Nil, Some(true), Nil) =>
        Redirect(controllers.routes.SoldGoodsFromNiController.onPageLoad(NormalMode, period).url)
      case (Some(true), Nil, Some(true), _) =>
        Redirect(controllers.routes.CountryToSameCountryController.onPageLoad(period).url)
      case (Some(true), Nil, _, _) =>
        Redirect(controllers.routes.SoldGoodsFromNiController.onPageLoad(NormalMode, period).url)
      case (Some(true), _, Some(true), Nil) =>
        Redirect(controllers.routes.SoldGoodsFromNiController.onPageLoad(NormalMode, period).url)
      case (Some(true), _, Some(true), _) =>
        Redirect(controllers.routes.CountryToSameCountryController.onPageLoad(period).url)
      case _ =>
        Redirect(controllers.routes.CheckYourAnswersController.onPageLoad(period).url)
    }
  }

}
