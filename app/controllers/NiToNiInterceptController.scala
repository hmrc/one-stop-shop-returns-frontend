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

import javax.inject.Inject
import models.{Country, Index, NormalMode, Period}
import models.requests.DataRequest
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

      Ok(view(period))
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
            navigate(period)
          }
        case _ =>
          Future.successful(navigate(period))
      }
  }

  private def navigate(period: Period)(implicit request: DataRequest[AnyContent]): Result = {

    val allSalesFromNiQuery = request.userAnswers.get(AllSalesFromNiWithOptionalVatQuery)
    val allSalesFromEuQuery = request.userAnswers.get(AllSalesFromEuQueryWithOptionalVatQuery)

    if (allSalesFromEuQuery.exists(_.nonEmpty) && allSalesFromNiQuery.exists(_.nonEmpty)) {
      Redirect(controllers.routes.CountryToSameCountryController.onPageLoad(period).url) // redirects here if user has said yes to sales from NI & from EU
    } else if (allSalesFromNiQuery.exists(_.nonEmpty)) {
      Redirect(controllers.routes.CheckYourAnswersController.onPageLoad(period).url) // redirects here if user has says from NI Only
    } else {
      Redirect(controllers.routes.SalesFromNiListController.onPageLoad(NormalMode, period).url)
    }
  }

}
