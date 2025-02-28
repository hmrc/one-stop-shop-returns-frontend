/*
 * Copyright 2025 HM Revenue & Customs
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

package models.requests

import base.SpecBase
import models.Quarter.Q4
import models.domain.VatRateType.{Reduced, Standard}
import models.domain.{SalesDetails, SalesFromEuCountry, SalesToCountry, VatRate}
import models.{Country, SalesToEu, StandardPeriod, VatOnSales, VatOnSalesChoice}
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.domain.Vrn

import java.time.LocalDate

class VatReturnRequestSpec extends SpecBase {

  "SaveForLaterRequest" - {
    "must serialise and deserialise correctly" in {

      val vrn: Vrn = Vrn("vrn")
      val period: StandardPeriod = StandardPeriod(2021, Q4)
      val startDate = Some(LocalDate.of(2021, 12, 8))
      val endDate = Some(LocalDate.of(2021, 12, 9))
      val salesFromNi = List(
        SalesToCountry(
          Country("AT", "Austria"), List(SalesDetails(VatRate(BigDecimal(100.10), Standard), BigDecimal(200.20), VatOnSales(VatOnSalesChoice.Standard, 300.30)))
        )
      )
      val salesFromEu = List(
        SalesFromEuCountry(Country("AT", "Austria"), None,
          List(SalesToCountry(Country("AT", "Austria"),
            List(SalesDetails(VatRate(BigDecimal(100.90), Reduced), BigDecimal(10.90), VatOnSales(VatOnSalesChoice.Standard, BigDecimal(100.10)))))
          )
        )
      )

      val json = Json.obj(
        "salesFromNi" -> Json.arr(
          Json.obj(
            "countryOfConsumption" -> Json.obj(
              "code" -> "AT",
              "name" -> "Austria"
            ),
            "amounts" -> Json.arr(
              Json.obj(
                "vatRate" -> Json.obj(
                  "rate" -> 100.1,
                  "rateType" -> "STANDARD"
                ),
                "netValueOfSales" -> 200.2,
                "vatOnSales" -> Json.obj(
                  "choice" -> "standard",
                  "amount" -> 300.3
                )
              )
            )
          )
        ),
        "vrn" -> "vrn",
        "startDate" -> "2021-12-08",
        "period" -> Json.obj(
          "year" -> 2021,
          "quarter" -> "Q4"
        ),
        "endDate" -> "2021-12-09",
        "salesFromEu" -> Json.arr(
          Json.obj(
            "countryOfSale" -> Json.obj(
              "code" -> "AT",
              "name" -> "Austria"
            ),
            "sales" -> Json.arr(
              Json.obj(
                "countryOfConsumption" -> Json.obj(
                  "code" -> "AT",
                  "name" -> "Austria"
                ),
                "amounts" -> Json.arr(
                  Json.obj(
                    "vatRate" -> Json.obj(
                      "rate" -> 100.9,
                      "rateType" -> "REDUCED"
                    ),
                    "netValueOfSales" -> 10.9,
                    "vatOnSales" -> Json.obj(
                      "choice" -> "standard",
                      "amount" -> 100.1
                    )
                  )
                )
              )
            )
          )
        )
      )


      val expectedResult = VatReturnRequest(vrn, period, startDate, endDate, salesFromNi, salesFromEu)

      Json.toJson(expectedResult) mustBe json
      json.validate[VatReturnRequest] mustBe JsSuccess(expectedResult)
    }
  }

}
