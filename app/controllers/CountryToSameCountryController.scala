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
import models.requests.DataRequest

import javax.inject.Inject
import models.{Country, Index, NormalMode, Period}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.{AllSalesFromEuQueryWithOptionalVatQuery, SalesFromEuQuery}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.CountryToSameCountryView

import scala.concurrent.{ExecutionContext, Future}

class CountryToSameCountryController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       view: CountryToSameCountryView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>

      Ok(view(period))
  }

  def onSubmit(period: Period): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      val maybeEuSaleWithIndex = request.userAnswers.get(AllSalesFromEuQueryWithOptionalVatQuery).getOrElse(List.empty).zipWithIndex
        .find {
          case (salesFromEuCountryWithOptionalVat, _) => Country.euCountries.exists(_.code == salesFromEuCountryWithOptionalVat.countryOfSale.code)

        }

      maybeEuSaleWithIndex match {
        case Some(_ @ (_, index)) =>
          for {
            updatedReturn <- Future.fromTry(request.userAnswers.remove(SalesFromEuQuery(Index(index))))
            _ <- cc.sessionRepository.set(updatedReturn)
          } yield {
            navigate(period)
          }
        case _ =>
          Future.successful(navigate(period))
      }
  }

  private def navigate(period: Period)(implicit request: DataRequest[AnyContent]): Result = {
    request.userAnswers
      .get(AllSalesFromEuQueryWithOptionalVatQuery) match {
      case Some(n) if n.nonEmpty =>
        Redirect(controllers.routes.SalesFromEuListController.onPageLoad(NormalMode, period).url)
      case _ =>
        Redirect(controllers.routes.SoldGoodsFromEuController.onPageLoad(NormalMode, period).url)
    }

  }

}
