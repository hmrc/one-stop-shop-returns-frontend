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

package generators

import models._
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.arbitrary
import pages._
import play.api.libs.json.{JsValue, Json}

trait UserAnswersEntryGenerators extends PageGenerators with ModelGenerators {

  implicit lazy val arbitraryVatRatesFromNiUserAnswersEntry: Arbitrary[(VatRatesFromNiPage, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[VatRatesFromNiPage]
        value <- Gen.nonEmptyListOf(arbitrary[VatRate]).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryVatOnSalesFromNiUserAnswersEntry: Arbitrary[(VatOnSalesFromNiPage, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[VatOnSalesFromNiPage]
        value <- arbitrary[Int].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitrarySoldGoodsFromNiUserAnswersEntry: Arbitrary[(SoldGoodsFromNiPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[SoldGoodsFromNiPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryNetValueOfSalesFromNiUserAnswersEntry: Arbitrary[(NetValueOfSalesFromNiPage, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[NetValueOfSalesFromNiPage]
        value <- arbitrary[Int].map(Json.toJson(_))
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
