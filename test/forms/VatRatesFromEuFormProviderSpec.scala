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

package forms

import forms.behaviours.CheckboxFieldBehaviours
import generators.Generators
import models.VatRate
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.FormError

class VatRatesFromEuFormProviderSpec extends CheckboxFieldBehaviours with ScalaCheckPropertyChecks with Generators {

  private val vatRate1 = arbitrary[VatRate].sample.value
  private val vatRate2 = arbitrary[VatRate].retryUntil(_ != vatRate1).sample.value
  private val vatRate3 = arbitrary[VatRate].retryUntil(v => !List(vatRate1,vatRate2).contains(v)).sample.value
  private val vatRates = List(vatRate1, vatRate2)
  private val formProvider = new VatRatesFromEuFormProvider()
  private val form = formProvider(vatRates)

  ".value" - {

    val fieldName = "value"
    val requiredKey = "vatRatesFromEu.error.required"

    "must bind all valid values" in {

      val data =
        Map(
          s"$fieldName[0]" -> vatRates.head.rate.toString,
          s"$fieldName[1]" -> vatRates.tail.head.rate.toString
        )

      val result = form.bind(data)
      result.get mustEqual vatRates
      result.errors mustBe empty
    }

    "must fail to bind invalid values" in {

      val data = Map(s"$fieldName[0]" -> vatRate3.rate.toString)
      form.bind(data).errors must contain(FormError(fieldName, "vatRatesFromEu.error.invalid"))
    }

    "must fail to bind when the key is not present" in {

      val data = Map.empty[String, String]
      form.bind(data).errors must contain theSameElementsAs Seq(FormError(fieldName, requiredKey))
    }

    "must fail to bind when no answer is selected" in {

      val data = Map(s"$fieldName[0]" -> "")
      form.bind(data).errors must contain theSameElementsAs Seq(FormError(s"$fieldName[0]", requiredKey))
    }
  }
}
