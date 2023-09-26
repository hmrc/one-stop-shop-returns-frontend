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

package services

import base.SpecBase
import models.{Country, SalesFromCountryWithOptionalVat, SalesFromEuWithOptionalVat}
import queries.AllSalesFromEuQueryWithOptionalVatQuery

class RemoveSameEuToEuServiceSpec extends SpecBase{

  val mockSalesData: List[SalesFromEuWithOptionalVat] = List(
    SalesFromEuWithOptionalVat(Country("AT", "Austria"), Some(List(
        SalesFromCountryWithOptionalVat(Country("AT", "Austria"), None),
        SalesFromCountryWithOptionalVat(Country("BG", "Bulgaria"), None),
      ))
    ),
    SalesFromEuWithOptionalVat(
      Country("BG", "Bulgaria"),
      Some(List(
        SalesFromCountryWithOptionalVat(Country("AT", "Austria"), None),
        SalesFromCountryWithOptionalVat(Country("BG", "Bulgaria"), None),
      ))
    ),
    SalesFromEuWithOptionalVat(
      Country("HR", "Croatia"),
      Some(List(
        SalesFromCountryWithOptionalVat(Country("AT", "Austria"), None),
      ))
    )
  )

  val mockExpectedSalesData: List[SalesFromEuWithOptionalVat] = List(
    SalesFromEuWithOptionalVat(Country("AT", "Austria"), Some(List(SalesFromCountryWithOptionalVat(Country("BG", "Bulgaria"), None)))),
    SalesFromEuWithOptionalVat(Country("BG", "Bulgaria"), Some(List(SalesFromCountryWithOptionalVat(Country("AT", "Austria"), None)))),
    SalesFromEuWithOptionalVat(Country("HR", "Croatia"), Some(List(SalesFromCountryWithOptionalVat(Country("AT", "Austria"), None))))
  )

  "RemoveSameEuToEuService.deleteEuToSameEuCountry" - {
    "must remove sales from the same EU Country" in {
      val userAnswers = emptyUserAnswers.set(AllSalesFromEuQueryWithOptionalVatQuery, mockSalesData).get

      val service = new RemoveSameEuToEuService()

      val result = service.deleteEuToSameEuCountry(userAnswers)

      val updateUserAnswers = result.get

      val expectedUserAnswers = emptyUserAnswers.set(AllSalesFromEuQueryWithOptionalVatQuery, mockExpectedSalesData).get

      updateUserAnswers mustBe expectedUserAnswers
    }

  }
}
