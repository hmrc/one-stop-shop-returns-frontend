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

import controllers.JourneyRecoverySyntax._
import models.requests.DataRequest
import models.{Country, Index, VatRate}
import pages.{CountryOfConsumptionFromEuPage, CountryOfSaleFromEuPage}
import play.api.mvc.{AnyContent, Result}
import queries.{DeriveNumberOfSalesFromEu, DeriveNumberOfSalesToEu, VatRateFromEuQuery}

import scala.concurrent.Future

trait SalesFromEuBaseController {

  // TODO: CountryOfSaleFromEuPage will need to change to a query in all of these methods

  protected def getCountryFrom(index: Index)
                              (block: Country => Result)
                              (implicit request: DataRequest[AnyContent]): Result =
    request.userAnswers
      .get(CountryOfSaleFromEuPage(index))
      .map(block(_))
      .orRecoverJourney

  protected def getCountryFromAsync(index: Index)
                                   (block: Country => Future[Result])
                                   (implicit request: DataRequest[AnyContent]): Future[Result] =
    request.userAnswers
      .get(CountryOfSaleFromEuPage(index))
      .map(block(_))
      .orRecoverJourney

  protected def getCountries(countryFromIndex: Index, countryToIndex: Index)
                            (block: (Country, Country) => Result)
                            (implicit request: DataRequest[AnyContent]): Result =
    (for {
      countryFrom <- request.userAnswers.get(CountryOfSaleFromEuPage(countryFromIndex))
      countryTo   <- request.userAnswers.get(CountryOfConsumptionFromEuPage(countryFromIndex, countryToIndex))
    } yield block(countryFrom, countryTo))
      .orRecoverJourney

  protected def getCountriesAsync(countryFromIndex: Index, countryToIndex: Index)
                                 (block: (Country, Country) => Future[Result])
                                 (implicit request: DataRequest[AnyContent]): Future[Result] =
    (for {
      countryFrom <- request.userAnswers.get(CountryOfSaleFromEuPage(countryFromIndex))
      countryTo   <- request.userAnswers.get(CountryOfConsumptionFromEuPage(countryFromIndex, countryToIndex))
    } yield block(countryFrom, countryTo))
      .orRecoverJourney

  protected def getCountriesAndVatRate(countryFromIndex: Index, countryToIndex: Index, vatRateIndex: Index)
                                      (block: (Country, Country, VatRate) => Result)
                                      (implicit request: DataRequest[AnyContent]): Result =
    (for {
      countryFrom <- request.userAnswers.get(CountryOfSaleFromEuPage(countryFromIndex))
      countryTo   <- request.userAnswers.get(CountryOfConsumptionFromEuPage(countryFromIndex, countryToIndex))
      vatRate     <- request.userAnswers.get(VatRateFromEuQuery(countryFromIndex, countryToIndex, vatRateIndex))
    } yield block(countryFrom, countryTo, vatRate))
      .orRecoverJourney

  protected def getCountriesAndVatRateAsync(countryFromIndex: Index, countryToIndex: Index, vatRateIndex: Index)
                                           (block: (Country, Country, VatRate) => Future[Result])
                                           (implicit request: DataRequest[AnyContent]): Future[Result] =
    (for {
      countryFrom <- request.userAnswers.get(CountryOfSaleFromEuPage(countryFromIndex))
      countryTo   <- request.userAnswers.get(CountryOfConsumptionFromEuPage(countryFromIndex, countryToIndex))
      vatRate     <- request.userAnswers.get(VatRateFromEuQuery(countryFromIndex, countryToIndex, vatRateIndex))
    } yield block(countryFrom, countryTo, vatRate))
      .orRecoverJourney

  protected def getNumberOfSalesFromEu(block: Int => Result)
                                      (implicit request: DataRequest[AnyContent]): Result =
    request.userAnswers
      .get(DeriveNumberOfSalesFromEu)
      .map(block(_))
      .orRecoverJourney

  protected def getNumberOfSalesToEuAndCountry(index: Index)
                                              (block: (Int, Country) => Result)
                                              (implicit request: DataRequest[AnyContent]): Result =
    (for {
      numberOfSales <- request.userAnswers.get(DeriveNumberOfSalesToEu(index))
      countryFrom   <- request.userAnswers.get(CountryOfSaleFromEuPage(index))
    } yield block(numberOfSales, countryFrom))
      .orRecoverJourney
}
