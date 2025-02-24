package models

import base.SpecBase
import play.api.libs.json.{JsSuccess, JsValue, Json}

import java.time.LocalDate

class SalesFromEuSpec extends SpecBase {

  "SalesFromEu" - {
    "must serialise and deserialise correctly" in {

      val countryOfSale = Country("AT", "Austria")
      val vatRateAndSales = VatRateAndSales(BigDecimal(101.11),VatRateType.Reduced,LocalDate.of(2022,10,1),None,None)
      val salesFromCountry = List(SalesFromCountry(Country("AT", "Austria"),List(vatRateAndSales)))

      val json = Json.obj(
        "countryOfSale" -> Json.obj(
          "code" -> "AT",
          "name" -> "Austria"
        ),
        "salesFromCountry" -> Json.arr(
          Json.obj(
            "countryOfConsumption" -> Json.obj(
              "code" -> "AT",
              "name" -> "Austria"
            ),
            "vatRates" -> Json.arr(
              Json.obj(
                "rate" -> "101.11",
                "rateType" -> "REDUCED",
                "validFrom" -> "2022-10-01"
              )
            )
          )
        )
      )

      val expectedResult = SalesFromEu(countryOfSale,salesFromCountry)

      Json.toJson(expectedResult) mustBe json
      json.validate[SalesFromEu] mustBe JsSuccess(expectedResult)
    }
  }
}
