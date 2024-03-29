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

package generators

import models.Index
import org.scalacheck.Arbitrary
import pages._
import pages.corrections._

trait PageGenerators {

  implicit lazy val arbitraryVatPeriodCorrectionsListPage: Arbitrary[VatPeriodCorrectionsListPage.type] =
    Arbitrary(VatPeriodCorrectionsListPage)

  implicit lazy val arbitraryVatCorrectionsListPage: Arbitrary[VatCorrectionsListPage.type] =
    Arbitrary(VatCorrectionsListPage)

  implicit lazy val arbitraryUndeclaredCountryCorrectionPage: Arbitrary[UndeclaredCountryCorrectionPage] =
    Arbitrary(UndeclaredCountryCorrectionPage(Index(0), Index(0)))

  implicit lazy val arbitraryRemovePeriodCorrectionPage: Arbitrary[RemovePeriodCorrectionPage.type] =
    Arbitrary(RemovePeriodCorrectionPage)

  implicit lazy val arbitraryRemoveCountryCorrectionPage: Arbitrary[RemoveCountryCorrectionPage.type] =
    Arbitrary(RemoveCountryCorrectionPage)

  implicit lazy val arbitraryCountryVatCorrectionPage: Arbitrary[CountryVatCorrectionPage] =
    Arbitrary(CountryVatCorrectionPage(Index(0), Index(0)))

  implicit lazy val arbitraryCorrectionReturnPeriodPage: Arbitrary[CorrectionReturnPeriodPage.type] =
    Arbitrary(CorrectionReturnPeriodPage)

  implicit lazy val arbitraryCorrectionCountryPage: Arbitrary[CorrectionCountryPage] =
    Arbitrary(CorrectionCountryPage(Index(0), Index(0)))

  implicit lazy val arbitraryCorrectPreviousReturnPage: Arbitrary[CorrectPreviousReturnPage.type] =
    Arbitrary(CorrectPreviousReturnPage)

  implicit lazy val arbitraryVatOnSalesFromEuPage: Arbitrary[VatOnSalesFromEuPage] =
    Arbitrary(VatOnSalesFromEuPage(Index(0), Index(0), Index(0)))

  implicit lazy val arbitraryNetValueOfSalesFromEuPage: Arbitrary[NetValueOfSalesFromEuPage] =
    Arbitrary(NetValueOfSalesFromEuPage(Index(0), Index(0), Index(0)))

  implicit lazy val arbitraryVatOnSalesFromNiPage: Arbitrary[VatOnSalesFromNiPage] =
    Arbitrary(VatOnSalesFromNiPage(Index(0), Index(0)))

  implicit lazy val arbitraryNetValueOfSalesFromNiPage: Arbitrary[NetValueOfSalesFromNiPage] =
    Arbitrary(NetValueOfSalesFromNiPage(Index(0), Index(0)))

  implicit lazy val arbitraryVatRatesFromEuPage: Arbitrary[VatRatesFromEuPage] =
    Arbitrary(VatRatesFromEuPage(Index(0), Index(0)))

  implicit lazy val arbitrarySoldGoodsFromEuPage: Arbitrary[SoldGoodsFromEuPage.type] =
    Arbitrary(SoldGoodsFromEuPage)

  implicit lazy val arbitraryCountryOfSaleFromEuPage: Arbitrary[CountryOfSaleFromEuPage] =
    Arbitrary(CountryOfSaleFromEuPage(Index(0)))

  implicit lazy val arbitraryCountryOfConsumptionFromEuPage: Arbitrary[CountryOfConsumptionFromEuPage] =
    Arbitrary(CountryOfConsumptionFromEuPage(Index(0), Index(0)))

  implicit lazy val arbitraryVatRatesFromNiPage: Arbitrary[VatRatesFromNiPage] =
    Arbitrary(VatRatesFromNiPage(Index(0)))

  implicit lazy val arbitrarySoldGoodsFromNiPage: Arbitrary[SoldGoodsFromNiPage.type] =
    Arbitrary(SoldGoodsFromNiPage)

  implicit lazy val arbitraryDeleteSalesFromNiPage: Arbitrary[DeleteSalesFromNiPage] =
    Arbitrary(DeleteSalesFromNiPage(Index(0)))

  implicit lazy val arbitraryCountryOfConsumptionFromNiPage: Arbitrary[CountryOfConsumptionFromNiPage] =
    Arbitrary(CountryOfConsumptionFromNiPage(Index(0)))

  implicit lazy val arbitraryStartReturnPage: Arbitrary[StartReturnPage.type] =
    Arbitrary(StartReturnPage)
}
