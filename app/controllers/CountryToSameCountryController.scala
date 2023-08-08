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
import logging.Logging
import models.requests.DataRequest

import javax.inject.Inject
import models.{Index, NormalMode, Period, SalesFromEuWithOptionalVat, UserAnswers}
import play.api.i18n.I18nSupport
import play.api.mvc._
import queries.{AllSalesFromEuQueryWithOptionalVatQuery, SalesFromEuQuery}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.CountryToSameCountryView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CountryToSameCountryController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       view: CountryToSameCountryView,
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging{

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>

      Ok(view(period))
  }

  def onSubmit(period: Period): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      for {
            updatedReturn <- Future.fromTry(deleteEuToSameEuCountry(request.userAnswers, period))
            _ <- cc.sessionRepository.set(updatedReturn)
      } yield {
        navigate(period)
      }

  }

  private def deleteEuToSameEuCountry(userAnswers: UserAnswers, period: Period): Try[UserAnswers] = {

    val allEuSaleWithIndex = userAnswers.get(AllSalesFromEuQueryWithOptionalVatQuery).getOrElse(List.empty).zipWithIndex

    def recursivelyRemoveSalesFromEu(currentUserAnswers: UserAnswers,  remainingEuSales: List[(SalesFromEuWithOptionalVat, Int)]): Try[UserAnswers] = {

      remainingEuSales match {
        case Nil => Try(currentUserAnswers)
        case (salesFromEuCountry, index) :: Nil
          if salesFromEuCountry.salesFromCountry.exists(_.exists(_.countryOfConsumption.code == salesFromEuCountry.countryOfSale.code)) =>
          removeUserAnswer(currentUserAnswers, index)
        case _ :: Nil => Try(currentUserAnswers)
        case (salesFromEuCountry, index) :: otherEuSales =>
          if (salesFromEuCountry.salesFromCountry.exists(_.exists(_.countryOfConsumption.code == salesFromEuCountry.countryOfSale.code))) {
            removeUserAnswer(currentUserAnswers, index).flatMap { cleanedUserAnswers =>
              recursivelyRemoveSalesFromEu(cleanedUserAnswers, otherEuSales)
            }
          } else {
            recursivelyRemoveSalesFromEu(currentUserAnswers, otherEuSales)
          }
      }
    }
    recursivelyRemoveSalesFromEu(userAnswers, allEuSaleWithIndex)

  }

  private def removeUserAnswer(currentUserAnswers: UserAnswers, index: Int): Try[UserAnswers] = {
    currentUserAnswers.remove(SalesFromEuQuery(Index(index)))
  }

  private def navigate(period: Period)(implicit request: DataRequest[AnyContent]): Result = {

    val allSalesFromEuQuery = request.userAnswers.get(AllSalesFromEuQueryWithOptionalVatQuery)

    allSalesFromEuQuery match {
      case Some(n) if n.nonEmpty =>
        Redirect(controllers.routes.CheckYourAnswersController.onPageLoad(period).url)
      case _ =>
        Redirect(controllers.routes.SalesFromEuListController.onPageLoad(NormalMode, period).url)
    }

  }

}
