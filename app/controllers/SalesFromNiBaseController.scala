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

import controllers.JourneyRecoverySyntax._
import models.requests.DataRequest
import models.{Country, Index, VatRate}
import pages.{CountryOfConsumptionFromNiPage, NetValueOfSalesFromNiPage}
import play.api.mvc.{AnyContent, Result}
import queries.{DeriveNumberOfSalesFromNi, VatRateFromNiQuery}

import scala.concurrent.Future

trait SalesFromNiBaseController {

  protected def getCountry(index: Index)
                          (block: Country => Result)
                          (implicit request: DataRequest[AnyContent]): Result =
    request.userAnswers
      .get(CountryOfConsumptionFromNiPage(index))
      .map(block(_))
      .orRecoverJourney


  protected def getCountryAsync(index: Index)
                               (block: Country => Future[Result])
                               (implicit request: DataRequest[AnyContent]): Future[Result] =
    request.userAnswers
      .get(CountryOfConsumptionFromNiPage(index))
      .map(block(_))
      .orRecoverJourney

  protected def getCountryAndVatRate(countryIndex: Index, vatRateIndex: Index)
                                    (block: (Country, VatRate) => Result)
                                    (implicit request: DataRequest[AnyContent]): Result =
    (for {
      country  <- request.userAnswers.get(CountryOfConsumptionFromNiPage(countryIndex))
      vatRate  <- request.userAnswers.get(VatRateFromNiQuery(countryIndex, vatRateIndex))
    } yield block(country, vatRate))
      .orRecoverJourney

  protected def getCountryAndVatRateAsync(countryIndex: Index, vatRateIndex: Index)
                                         (block: (Country, VatRate) => Future[Result])
                                         (implicit request: DataRequest[AnyContent]): Future[Result] =
    (for {
      country  <- request.userAnswers.get(CountryOfConsumptionFromNiPage(countryIndex))
      vatRate  <- request.userAnswers.get(VatRateFromNiQuery(countryIndex, vatRateIndex))
    } yield block(country, vatRate))
      .orRecoverJourney

  protected def getCountryVatRateAndNetSales(countryIndex: Index, vatRateIndex: Index)
                                            (block: (Country, VatRate, BigDecimal) => Result)
                                            (implicit request: DataRequest[AnyContent]): Result =
    (for {
      country  <- request.userAnswers.get(CountryOfConsumptionFromNiPage(countryIndex))
      vatRate  <- request.userAnswers.get(VatRateFromNiQuery(countryIndex, vatRateIndex))
      netSales <- request.userAnswers.get(NetValueOfSalesFromNiPage(countryIndex, vatRateIndex))
    } yield block(country, vatRate, netSales))
      .orRecoverJourney

  protected def getCountryVatRateAndNetSalesAsync(countryIndex: Index, vatRateIndex: Index)
                                                 (block: (Country, VatRate, BigDecimal) => Future[Result])
                                                 (implicit request: DataRequest[AnyContent]): Future[Result] =
    (for {
      country  <- request.userAnswers.get(CountryOfConsumptionFromNiPage(countryIndex))
      vatRate  <- request.userAnswers.get(VatRateFromNiQuery(countryIndex, vatRateIndex))
      netSales <- request.userAnswers.get(NetValueOfSalesFromNiPage(countryIndex, vatRateIndex))
    } yield block(country, vatRate, netSales))
      .orRecoverJourney

  protected def getNumberOfSalesFromNi(block: Int => Result)
                                      (implicit request: DataRequest[AnyContent]): Result =
    request.userAnswers
      .get(DeriveNumberOfSalesFromNi)
      .map(block(_))
      .orRecoverJourney
}
