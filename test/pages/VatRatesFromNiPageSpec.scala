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

package pages

import controllers.routes
import models.{CheckMode, Index, NormalMode, SalesAtVatRate, VatRate}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import pages.behaviours.PageBehaviours

class VatRatesFromNiPageSpec extends PageBehaviours {

  "VatRatesFromNiPage" - {

    beRetrievable[List[VatRate]](VatRatesFromNiPage(index))

    beSettable[List[VatRate]](VatRatesFromNiPage(index))

    beRemovable[List[VatRate]](VatRatesFromNiPage(index))

    "must navigate in Normal mode" - {

      "to Net Value of Sales" in {

        VatRatesFromNiPage(index).navigate(NormalMode, emptyUserAnswers)
          .mustEqual(routes.NetValueOfSalesFromNiController.onPageLoad(NormalMode, emptyUserAnswers.period, index, Index(0)))
      }
    }

    "must navigate in Check mode" - {

      "to Net Value of Sales" in {

        VatRatesFromNiPage(index).navigate(CheckMode, emptyUserAnswers)
          .mustEqual(routes.NetValueOfSalesFromNiController.onPageLoad(CheckMode, emptyUserAnswers.period, index, Index(0)))
      }
    }

    "cleanup" - {
      val vatRates = Gen.listOfN(2, arbitrary[VatRate]).sample.value

      "must remove values when answer changes" in {
        val newVatRates = Gen.listOfN(1, arbitrary[VatRate]).sample.value

        val answers = emptyUserAnswers
          .set(VatRatesFromNiPage(index), vatRates).success.value
          .set(SalesAtVatRateFromNiPage(index, index), SalesAtVatRate(BigDecimal(0), BigDecimal(0))).success.value
          .set(SalesAtVatRateFromNiPage(index, index + 1), SalesAtVatRate(BigDecimal(0), BigDecimal(0))).success.value
          .set(VatRatesFromNiPage(index), newVatRates).success.value


        val expected = emptyUserAnswers
          .set(VatRatesFromNiPage(index), newVatRates).success.value

        val actual = VatRatesFromNiPage(index).cleanup(Some(newVatRates), answers).success.value

        actual mustEqual expected
      }
    }
  }
}
