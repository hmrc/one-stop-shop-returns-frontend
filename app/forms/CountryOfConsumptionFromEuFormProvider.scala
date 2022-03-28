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

package forms

import forms.mappings.Mappings
import models.Country.euCountriesWithNI
import models.{Country, Index}
import play.api.data.Form
import play.api.data.validation.{Constraint, Invalid, Valid}

import javax.inject.Inject

class CountryOfConsumptionFromEuFormProvider @Inject() extends Mappings {

  def apply(
    index: Index,
    existingAnswers: Seq[Country],
    countryFrom: Country,
    isOnlineMarketplace: Boolean
  ): Form[Country] =
    Form(
      "value" -> text("countryOfConsumptionFromEu.error.required", args = Seq(countryFrom.name))
        .verifying(validIfEqualToCountryFrom(countryFrom, isOnlineMarketplace))
        .transform[Country](value => euCountriesWithNI.find(_.code == value).get, _.code)
        .verifying(notADuplicate(index, existingAnswers, "countryOfConsumptionFromEu.error.duplicate"))
    )

  private def validIfEqualToCountryFrom(countryFrom: Country, isOnlineMarketplace: Boolean): Constraint[String] =
    Constraint {
      case value if euCountriesWithNI.filterNot(_ == countryFrom).exists(_.code == value) =>
        Valid
      case _ =>
        if (isOnlineMarketplace) {
          Valid
        } else {
          Invalid("countryOfConsumptionFromEu.error.required", countryFrom.name)
        }
    }
}
