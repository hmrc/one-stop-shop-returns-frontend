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

package pages

import controllers.routes
import models.{CheckMode, Country, Index, NormalMode, VatOnSales, VatRate}
import org.scalacheck.Arbitrary.arbitrary
import pages.behaviours.PageBehaviours

class SoldGoodsFromNiPageSpec extends PageBehaviours {

  "SoldGoodsFromNiPage" - {

    beRetrievable[Boolean](SoldGoodsFromNiPage)

    beSettable[Boolean](SoldGoodsFromNiPage)

    beRemovable[Boolean](SoldGoodsFromNiPage)

    "must navigate in Normal mode" - {

      "to Country of Consumption when the answer is yes" in {

        val answers = emptyUserAnswers.set(SoldGoodsFromNiPage, true).success.value

        SoldGoodsFromNiPage.navigate(NormalMode, answers)
          .mustEqual(routes.CountryOfConsumptionFromNiController.onPageLoad(NormalMode, answers.period, Index(0)))
      }

      "to SoldGoodsFromEU when the answer is no" in {

        val answers = emptyUserAnswers.set(SoldGoodsFromNiPage, false).success.value

        SoldGoodsFromNiPage.navigate(NormalMode, answers)
          .mustEqual(routes.SoldGoodsFromEuController.onPageLoad(NormalMode, answers.period))
      }
    }

    "must navigate in Check mode" - {

      "when the answer is yes" - {

        "to Country of Consumption" in {

          val answers = emptyUserAnswers.set(SoldGoodsFromNiPage, true).success.value

          SoldGoodsFromNiPage.navigate(CheckMode, answers)
            .mustEqual(routes.CountryOfConsumptionFromNiController.onPageLoad(CheckMode, answers.period, index))
        }
      }

      "when the answer is no" - {

        "to Check Your Answers and clear previous set answers" in {

          val answers = emptyUserAnswers
            .set(SoldGoodsFromNiPage, false).success.value

          SoldGoodsFromNiPage.navigate(CheckMode, answers)
            .mustEqual(routes.CheckYourAnswersController.onPageLoad(answers.period))
        }
      }
    }

    "cleanup" - {
      val country: Country = arbitrary[Country].sample.value
      val vatRate: VatRate = arbitrary[VatRate].sample.value

      "must remove values when answer is no" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsFromNiPage, false).success.value
          .set(CountryOfConsumptionFromNiPage(index), country).success.value
          .set(VatRatesFromNiPage(index), List(vatRate)).success.value
          .set(NetValueOfSalesFromNiPage(index, index), arbitrary[BigDecimal].sample.value).success.value
          .set(VatOnSalesFromNiPage(index, index), arbitrary[VatOnSales].sample.value).success.value

        val expected = emptyUserAnswers
          .set(SoldGoodsFromNiPage, false).success.value

        val result = SoldGoodsFromNiPage.cleanup(Some(false), answers).success.value

        result mustBe expected
      }

      "must remove multiple values when answer is no" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsFromNiPage, false).success.value
          .set(CountryOfConsumptionFromNiPage(index), country).success.value
          .set(VatRatesFromNiPage(index), List(vatRate)).success.value
          .set(NetValueOfSalesFromNiPage(index, index), arbitrary[BigDecimal].sample.value).success.value
          .set(VatOnSalesFromNiPage(index, index), arbitrary[VatOnSales].sample.value).success.value
          .set(NetValueOfSalesFromNiPage(index, index + 1), arbitrary[BigDecimal].sample.value).success.value
          .set(VatOnSalesFromNiPage(index, index + 1), arbitrary[VatOnSales].sample.value).success.value
          .set(CountryOfConsumptionFromNiPage(index + 1), country).success.value
          .set(VatRatesFromNiPage(index + 1), List(vatRate)).success.value
          .set(NetValueOfSalesFromNiPage(index + 1, index), arbitrary[BigDecimal].sample.value).success.value
          .set(VatOnSalesFromNiPage(index + 1, index), arbitrary[VatOnSales].sample.value).success.value

        val expected = emptyUserAnswers
          .set(SoldGoodsFromNiPage, false).success.value

        val result = SoldGoodsFromNiPage.cleanup(Some(false), answers).success.value

        result mustBe expected
      }

      "must not remove values when answer is yes" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsFromNiPage, true).success.value
          .set(CountryOfConsumptionFromNiPage(index), country).success.value
          .set(VatRatesFromNiPage(index), List(vatRate)).success.value
          .set(NetValueOfSalesFromNiPage(index, index), arbitrary[BigDecimal].sample.value).success.value
          .set(VatOnSalesFromNiPage(index, index), arbitrary[VatOnSales].sample.value).success.value

        val result = SoldGoodsFromNiPage.cleanup(Some(true), answers).success.value

        result mustBe answers
      }
    }
  }
}
