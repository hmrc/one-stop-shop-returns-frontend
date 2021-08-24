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

import forms.behaviours.StringFieldBehaviours
import models.{Country, Index}
import org.scalacheck.Arbitrary.arbitrary
import play.api.data.FormError

class CountryOfConsumptionFromNiFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "countryOfConsumptionFromNi.error.required"
  val lengthKey = "countryOfConsumptionFromNi.error.length"
  val maxLength = 100
  val index = Index(0)
  val emptyExistingAnswers = Seq.empty[Country]

  val form = new CountryOfConsumptionFromNiFormProvider()(index, emptyExistingAnswers)

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      arbitrary[Country].map(_.code)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must not bind any values other than valid country codes" in {

      val invalidAnswers = arbitrary[String] suchThat (x => !Country.euCountries.map(_.code).contains(x))

      forAll(invalidAnswers) {
        answer =>
          val result = form.bind(Map("value" -> answer)).apply(fieldName)
          result.errors must contain only FormError(fieldName, requiredKey)
      }
    }

    "must fail to bind when given a duplicate value" in {
      val answer = Country.euCountries.tail.head
      val existingAnswers = Seq(Country.euCountries.head, Country.euCountries.tail.head)

      val duplicateForm = new CountryOfConsumptionFromNiFormProvider()(index, existingAnswers)

      val result = duplicateForm.bind(Map(fieldName ->  answer.code)).apply(fieldName)
      result.errors must contain only FormError(fieldName, "countryOfConsumptionFromNi.error.duplicate")
    }
  }
}
