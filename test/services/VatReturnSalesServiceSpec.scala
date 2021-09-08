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

package services

import base.SpecBase
import models.Country
import models.domain.{EuTaxIdentifier, EuTaxIdentifierType, SalesDetails, SalesFromEuCountry, SalesToCountry, VatRate, VatRateType}

class VatReturnSalesServiceSpec extends SpecBase {

  val service = new VatReturnSalesService()

  "VatReturnSalesService" - {

    "getNiTotalVatOnSales" - {

      "must show correct vat total for multiple countries with vat rates" in {
        service.getTotalVatOnSalesToCountry(completeVatReturn.salesFromNi) mustBe BigDecimal(741806.8)
      }

    }

    "getNiTotalNetSales" - {

      "must show correct net total sales for one country with one vat rate" in {
        service.getTotalNetSalesToCountry(completeVatReturn.salesFromNi) mustBe BigDecimal(960197.21)
      }

    }

    "getEuTotalVatOnSales" - {

      "must show correct total vat from one country, to one country, with one vat rate" in {
        service.getEuTotalVatOnSales(completeVatReturn.salesFromEu) mustBe BigDecimal(1379807.43)
      }

    }

    "getEuTotalNetSales" - {

      "must show correct net total sales for one country from, one country to with one vat rate" in {
        service.getEuTotalNetSales(completeVatReturn.salesFromEu) mustBe BigDecimal(1022804.90)
      }

    }

    "getTotalVatOnSales" - {

      "must return correct total when NI and EU sales exist" in {

        service.getTotalVatOnSales(completeVatReturn) mustBe BigDecimal(2121614.23)
      }

      "must return zero when total NI and EU sales don't exist" in {

        service.getTotalVatOnSales(emptyVatReturn) mustBe BigDecimal(0)
      }

      "must return total when NI exists and EU sales don't exist" in {

        val salesFromNi = List(SalesToCountry(Country("LT",
          "Lithuania"),
          List(SalesDetails(VatRate(45.54,
            VatRateType.Reduced),
            306338.71,
            230899.32),
            SalesDetails(VatRate(98.54,
              VatRateType.Reduced),
              295985.50,
              319051.84))),
          SalesToCountry(Country("MT",
            "Malta"),
            List(SalesDetails(VatRate(80.28,
              VatRateType.Standard),
              357873.00,
              191855.64))))

        service.getTotalVatOnSales(emptyVatReturn.copy(salesFromNi = salesFromNi)) mustBe BigDecimal(741806.80)
      }

      "must return total when NI doesn't exist and EU does exist" in {

        val salesFromEu = List(SalesFromEuCountry(Country("DE", "Germany"),
          Some(EuTaxIdentifier(EuTaxIdentifierType.Vat, "-1")),
          List(SalesToCountry(Country("FI",
            "Finland"),
            List(SalesDetails(VatRate(56.02,
              VatRateType.Standard),
              543742.51,
              801143.05))))),
          SalesFromEuCountry(Country("IE",
            "Ireland"),
            Some(EuTaxIdentifier(EuTaxIdentifierType.Other, "-2147483648")),
            List(SalesToCountry(Country("CY",
              "Republic of Cyprus"),
              List(SalesDetails(VatRate(98.97,
                VatRateType.Reduced),
                356270.07,
                24080.60),
                SalesDetails(VatRate(98.92,
                  VatRateType.Reduced),
                  122792.32,
                  554583.78))))))

        service.getTotalVatOnSales(emptyVatReturn.copy(salesFromEu = salesFromEu)) mustBe BigDecimal(1379807.43)
      }
    }
  }
}
