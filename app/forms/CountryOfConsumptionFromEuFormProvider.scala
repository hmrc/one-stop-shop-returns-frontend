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

import javax.inject.Inject
import forms.mappings.Mappings
import models.Country
import play.api.data.Form
import play.api.data.validation.{Constraint, Invalid, Valid}

class CountryOfConsumptionFromEuFormProvider @Inject() extends Mappings {

  def apply(countryFrom: Country): Form[Country] =
    Form(
      "value" -> text("countryOfConsumptionFromEu.error.required", args = Seq(countryFrom.name))
        .verifying(validCountry(countryFrom))
        .transform[Country](value => Country.euCountries.find(_.code == value).get, _.code)
    )

  private def validCountry(countryFrom: Country): Constraint[String] =
    Constraint {
      case value if Country.euCountries.filterNot(_ == countryFrom).exists(_.code == value) =>
        Valid
      case _ =>
        Invalid("countryOfConsumptionFromEu.error.required", countryFrom.name)
    }
}