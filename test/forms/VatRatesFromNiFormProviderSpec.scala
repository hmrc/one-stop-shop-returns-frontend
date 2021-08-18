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

package forms

import forms.behaviours.CheckboxFieldBehaviours
import generators.Generators
import models.VatRate
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.FormError

class VatRatesFromNiFormProviderSpec extends CheckboxFieldBehaviours with ScalaCheckPropertyChecks with Generators {

  private val vatRates = Gen.listOfN(2, arbitrary[VatRate]).sample.value
  private val formProvider = new VatRatesFromNiFormProvider()
  private val form = formProvider(vatRates)

  ".value" - {

    val fieldName = "value"
    val requiredKey = "vatRatesFromNi.error.required"

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

      forAll(arbitrary[VatRate]) {
        vatRate =>

          whenever(!vatRates.contains(vatRate)) {
            val data = Map(s"$fieldName[0]" -> vatRate.rate.toString)

            form.bind(data).errors must contain(FormError(fieldName, "vatRatesFromNi.error.invalid"))
          }
      }
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
