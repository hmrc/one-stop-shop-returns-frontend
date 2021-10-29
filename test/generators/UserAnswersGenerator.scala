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

import models.Quarter.Q3
import models.UserAnswers
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.TryValues
import pages._
import pages.corrections.{CorrectPreviousReturnPage, CorrectionCountryPage, CorrectionReturnPeriodPage, CorrectionReturnSinglePeriodPage, CountryVatCorrectionPage, RemoveCountryCorrectionPage, RemovePeriodCorrectionPage, UndeclaredCountryCorrectionPage, VatCorrectionsListPage, VatPeriodCorrectionsListPage}
import play.api.libs.json.{JsValue, Json}

trait UserAnswersGenerator extends TryValues {
  self: Generators =>

  val generators: Seq[Gen[(QuestionPage[_], JsValue)]] =
    arbitrary[(CorrectionReturnSinglePeriodPage.type, JsValue)] ::
    arbitrary[(VatPeriodCorrectionsListPage.type, JsValue)] ::
    arbitrary[(VatCorrectionsListPage.type, JsValue)] ::
    arbitrary[(UndeclaredCountryCorrectionPage, JsValue)] ::
    arbitrary[(RemovePeriodCorrectionPage.type, JsValue)] ::
    arbitrary[(RemoveCountryCorrectionPage.type, JsValue)] ::
    arbitrary[(CountryVatCorrectionPage.type, JsValue)] ::
    arbitrary[(CorrectionCountryPage, JsValue)] ::
    arbitrary[(CorrectPreviousReturnPage.type, JsValue)] ::
    arbitrary[(VatRatesFromEuPage, JsValue)] ::
    arbitrary[(SoldGoodsFromEuPage.type, JsValue)] ::
    arbitrary[(CountryOfSaleFromEuPage, JsValue)] ::
    arbitrary[(CountryOfConsumptionFromEuPage, JsValue)] ::
    arbitrary[(VatRatesFromNiPage, JsValue)] ::
    arbitrary[(SoldGoodsFromNiPage.type, JsValue)] ::
    arbitrary[(DeleteSalesFromNiPage, JsValue)] ::
    arbitrary[(CountryOfConsumptionFromNiPage, JsValue)] ::
    arbitrary[(StartReturnPage.type, JsValue)] ::
    Nil

  implicit lazy val arbitraryUserData: Arbitrary[UserAnswers] = {

    import models._

    Arbitrary {
      for {
        id      <- nonEmptyString
        data    <- generators match {
          case Nil => Gen.const(Map[QuestionPage[_], JsValue]())
          case _   => Gen.mapOf(oneOf(generators))
        }
      } yield UserAnswers (
        userId           = id,
        period       = Period(2021, Q3),
        data         = data.foldLeft(Json.obj()) {
          case (obj, (path, value)) =>
            obj.setObject(path.path, value).get
        }
      )
    }
  }
}
