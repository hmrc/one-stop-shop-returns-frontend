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

package controllers

import models.requests.DataRequest
import models.{Index, VatRatesFromNi}
import pages.CountryOfConsumptionFromNiPage
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import queries.{DeriveNumberOfSalesFromNi, VatRateQuery}
import utils.FutureSyntax._

import scala.concurrent.Future

trait SalesFromNiBaseController {

  protected def getCountry(index: Index)
                          (block: String => Result)
                          (implicit request: DataRequest[AnyContent]): Result =
    request.userAnswers
      .get(CountryOfConsumptionFromNiPage(index))
      .map(block(_))
      .getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))


  protected def getCountryAsync(index: Index)
                               (block: String => Future[Result])
                               (implicit request: DataRequest[AnyContent]): Future[Result] =
    request.userAnswers
      .get(CountryOfConsumptionFromNiPage(index))
      .map(block(_))
      .getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture)

  protected def getCountryAndVatRate(countryIndex: Index, vatRateIndex: Index)
                                    (block: (String, VatRatesFromNi) => Result)
                                    (implicit request: DataRequest[AnyContent]): Result =
    (for {
      country  <- request.userAnswers.get(CountryOfConsumptionFromNiPage(countryIndex))
      vatRate  <- request.userAnswers.get(VatRateQuery(countryIndex, vatRateIndex))
    } yield block(country, vatRate))
      .getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))

  protected def getCountryAndVatRateAsync(countryIndex: Index, vatRateIndex: Index)
                                         (block: (String, VatRatesFromNi) => Future[Result])
                                         (implicit request: DataRequest[AnyContent]): Future[Result] =
    (for {
      country  <- request.userAnswers.get(CountryOfConsumptionFromNiPage(countryIndex))
      vatRate  <- request.userAnswers.get(VatRateQuery(countryIndex, vatRateIndex))
    } yield block(country, vatRate))
      .getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture)

  protected def getNumberOfSalesFromNi(block: Int => Result)
                                      (implicit request: DataRequest[AnyContent]): Result =
    request.userAnswers
      .get(DeriveNumberOfSalesFromNi)
      .map(block(_))
      .getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
}
