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

import models._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import pages._
import pages.corrections._
import play.api.libs.json.{JsValue, Json}

trait UserAnswersEntryGenerators extends PageGenerators with ModelGenerators {

  implicit lazy val arbitraryVatPeriodCorrectionsListUserAnswersEntry: Arbitrary[(VatPeriodCorrectionsListPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[VatPeriodCorrectionsListPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryVatCorrectionsListUserAnswersEntry: Arbitrary[(VatCorrectionsListPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[VatCorrectionsListPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryUndeclaredCountryCorrectionUserAnswersEntry: Arbitrary[(UndeclaredCountryCorrectionPage, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[UndeclaredCountryCorrectionPage]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryCountryVatCorrectionUserAnswersEntry: Arbitrary[(CountryVatCorrectionPage, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[CountryVatCorrectionPage]
        value <- arbitrary[Int].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryCorrectionCountryUserAnswersEntry: Arbitrary[(CorrectionCountryPage, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[CorrectionCountryPage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryCorrectPreviousReturnUserAnswersEntry: Arbitrary[(CorrectPreviousReturnPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[CorrectPreviousReturnPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryVatOnSalesFromEuUserAnswersEntry: Arbitrary[(VatOnSalesFromEuPage, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[VatOnSalesFromEuPage]
        value <- arbitrary[Int].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryNetValueOfSalesFromEuUserAnswersEntry: Arbitrary[(NetValueOfSalesFromEuPage, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[NetValueOfSalesFromEuPage]
        value <- arbitrary[Int].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryVatOnSalesFromNiUserAnswersEntry: Arbitrary[(VatOnSalesFromNiPage, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[VatOnSalesFromNiPage]
        value <- arbitrary[Int].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryNetValueOfSalesFromNiUserAnswersEntry: Arbitrary[(NetValueOfSalesFromNiPage, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[NetValueOfSalesFromNiPage]
        value <- arbitrary[Int].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryVatRatesFromEuUserAnswersEntry: Arbitrary[(VatRatesFromEuPage, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[VatRatesFromEuPage]
        value <- Gen.nonEmptyListOf(arbitrary[VatRate]).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitrarySoldGoodsFromEuUserAnswersEntry: Arbitrary[(SoldGoodsFromEuPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[SoldGoodsFromEuPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryCountryOfSaleFromEuUserAnswersEntry: Arbitrary[(CountryOfSaleFromEuPage, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[CountryOfSaleFromEuPage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryCountryOfConsumptionFromEuUserAnswersEntry: Arbitrary[(CountryOfConsumptionFromEuPage, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[CountryOfConsumptionFromEuPage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryVatRatesFromNiUserAnswersEntry: Arbitrary[(VatRatesFromNiPage, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[VatRatesFromNiPage]
        value <- Gen.nonEmptyListOf(arbitrary[VatRate]).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitrarySoldGoodsFromNiUserAnswersEntry: Arbitrary[(SoldGoodsFromNiPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[SoldGoodsFromNiPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryDeleteSalesFromNiUserAnswersEntry: Arbitrary[(DeleteSalesFromNiPage, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[DeleteSalesFromNiPage]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryCountryOfConsumptionFromNiUserAnswersEntry: Arbitrary[(CountryOfConsumptionFromNiPage, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[CountryOfConsumptionFromNiPage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryStartReturnUserAnswersEntry: Arbitrary[(StartReturnPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[StartReturnPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }
}
