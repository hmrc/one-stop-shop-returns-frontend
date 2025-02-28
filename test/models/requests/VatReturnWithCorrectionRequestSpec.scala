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
import models.{Country, StandardPeriod, VatOnSales,  VatOnSalesChoice}
import models.domain.{SalesDetails, SalesFromEuCountry, SalesToCountry, VatRate}
import models.corrections.PeriodWithCorrections
import models.requests.corrections.CorrectionRequest
import play.api.libs.json.{JsSuccess, JsValue, Json}

import uk.gov.hmrc.domain.Vrn


class VatReturnWithCorrectionRequestSpec extends SpecBase {

  "VatReturnWithCorrectionRequest" - {
    "must serialise and deserialise correctly" in {

      val vrn:Vrn = Vrn("vrn")
      val period:StandardPeriod = StandardPeriod(2021,Q4)

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
      val corrections:List[PeriodWithCorrections] = List(PeriodWithCorrections(period,None))

      val vatReturnRequest: VatReturnRequest = VatReturnRequest(vrn,period,None,None, salesFromNi, salesFromEu)
      val correctionRequest: CorrectionRequest = CorrectionRequest(vrn,period,corrections)

      val json = Json.obj(
        "vatReturnRequest" -> Json.obj(
          "vrn" -> "vrn",
          "period" -> Json.obj(
            "year" -> 2021,
            "quarter" -> "Q4"
          ),
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
        ),
        "correctionRequest" -> Json.obj(
          "vrn" -> "vrn",
          "period" -> Json.obj(
            "year" -> 2021,
            "quarter" -> "Q4"
          ),
          "corrections" -> Json.arr(
            Json.obj(
              "correctionReturnPeriod" -> Json.obj(
                "year" -> 2021,
                "quarter" -> "Q4"
              )
            )
          )
        )
      )


      val expectedResult = VatReturnWithCorrectionRequest(vatReturnRequest, correctionRequest)

      Json.toJson(expectedResult) mustBe json
      json.validate[VatReturnWithCorrectionRequest] mustBe JsSuccess(expectedResult)
    }
  }
  

}
