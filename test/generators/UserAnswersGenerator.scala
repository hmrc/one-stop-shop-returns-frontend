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

import models.UserAnswers
import models.registration.{ContactDetails, Registration, UkAddress, VatDetailSource, VatDetails}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.TryValues
import pages._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Vrn

import java.time.LocalDate

trait UserAnswersGenerator extends TryValues {
  self: Generators =>

  val generators: Seq[Gen[(QuestionPage[_], JsValue)]] =
    arbitrary[(VatRatesFromNiPage.type, JsValue)] ::
    arbitrary[(VatOnSalesFromNiPage.type, JsValue)] ::
    arbitrary[(SoldGoodsFromNiPage.type, JsValue)] ::
    arbitrary[(NetValueOfSalesFromNiPage.type, JsValue)] ::
    arbitrary[(DeleteSalesFromNiPage.type, JsValue)] ::
    arbitrary[(CountryOfConsumptionFromNiPage.type, JsValue)] ::
    arbitrary[(StartReturnPage.type, JsValue)] ::
    Nil

  implicit lazy val arbitraryUserData: Arbitrary[UserAnswers] = {

    import models._

    val address = UkAddress("line 1", None, "town", None, "AA11 1AA")
    val registration = Registration(
      vrn                   = Vrn("123456789"),
      registeredCompanyName = "name",
      vatDetails            = VatDetails(LocalDate.of(2000, 1, 1), address, false, VatDetailSource.Mixed),
      euRegistrations       = Nil,
      contactDetails        = ContactDetails("name", "0123 456789", "email@example.com"),
      commencementDate      = LocalDate.now
    )

    Arbitrary {
      for {
        id      <- nonEmptyString
        data    <- generators match {
          case Nil => Gen.const(Map[QuestionPage[_], JsValue]())
          case _   => Gen.mapOf(oneOf(generators))
        }
      } yield UserAnswers (
        id           = id,
        registration = registration,
        data         = data.foldLeft(Json.obj()) {
          case (obj, (path, value)) =>
            obj.setObject(path.path, value).get
        }
      )
    }
  }
}
