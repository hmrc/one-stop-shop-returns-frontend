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

import forms.behaviours.StringFieldBehaviours
import models.{Country, Index}
import org.scalacheck.Arbitrary.arbitrary
import play.api.data.FormError

class CountryOfConsumptionFromEuFormProviderSpec extends StringFieldBehaviours {

  private val requiredKey = "countryOfConsumptionFromEu.error.required"
  private val countryFrom = arbitrary[Country].sample.value
  val index = Index(0)
  val emptyExistingAnswers = Seq.empty[Country]

  val form = new CountryOfConsumptionFromEuFormProvider()(index, emptyExistingAnswers, countryFrom, false)

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      arbitrary[Country].suchThat(_ != countryFrom).map(_.code)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey, Seq(countryFrom.name))
    )

    "bind country that is the same as countryFrom when onlineMarketplace is true" in {
      val form = new CountryOfConsumptionFromEuFormProvider()(index, emptyExistingAnswers, countryFrom, true)
      val result = form.bind(Map(fieldName -> countryFrom.code)).apply(fieldName)
      result.value.value mustBe countryFrom.code
      result.errors mustBe empty
    }

    "not bind country that is the same as countryFrom when onlineMarketplace is false" in {
      val form = new CountryOfConsumptionFromEuFormProvider()(index, emptyExistingAnswers, countryFrom, false)
      val result = form.bind(Map("value" -> countryFrom.code)).apply(fieldName)
      result.errors must contain only FormError(fieldName, requiredKey, Seq(countryFrom.name))
    }

    "must not bind any values other than valid country codes" in {

      val invalidAnswers = arbitrary[String] suchThat (x => !Country.euCountries.map(_.code).contains(x))

      forAll(invalidAnswers) {
        answer =>
          val result = form.bind(Map("value" -> answer)).apply(fieldName)
          result.errors must contain only FormError(fieldName, requiredKey, Seq(countryFrom.name))
      }
    }

    "must fail to bind when given a duplicate value" in {
      val answer = Country.euCountries.tail.head
      val existingAnswers = Seq(Country.euCountries.head, Country.euCountries.tail.head)

      val duplicateForm =
        new CountryOfConsumptionFromEuFormProvider()(index, existingAnswers, Country.euCountries.reverse.head, false)

      val result = duplicateForm.bind(Map(fieldName ->  answer.code)).apply(fieldName)
      result.errors must contain only FormError(fieldName, "countryOfConsumptionFromEu.error.duplicate")
    }
  }
}
