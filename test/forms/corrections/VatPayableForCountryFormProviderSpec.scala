/*
 * Copyright 2022 HM Revenue & Customs
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

package forms.corrections

import forms.VatPayableForCountryFormProvider
import forms.behaviours.BooleanFieldBehaviours
import models.Country
import org.scalacheck.{Arbitrary, Gen}
import play.api.data.FormError

class VatPayableForCountryFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "vatPayableForCountry.error.required"
  val invalidKey = "error.boolean"
  val form = new VatPayableForCountryFormProvider()(Country("DE", "Germany"), BigDecimal(10))
  val errorArgs = Seq("Germany", "&pound;10")

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey, errorArgs)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey, errorArgs)
    )
  }
}
