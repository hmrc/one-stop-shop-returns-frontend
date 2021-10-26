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

package forms.corrections

import config.Constants.maxCurrencyAmount
import forms.mappings.Mappings
import play.api.data.Form

import javax.inject.Inject

class CountryVatCorrectionFormProvider @Inject() extends Mappings {

  def apply(country:String): Form[BigDecimal] =
    Form(
      "value" -> currency(
        "countryVatCorrection.error.required",
        "countryVatCorrection.error.wholeNumber",
        "countryVatCorrection.error.nonNumeric",
        args = Seq(country))
          .verifying(inRange[BigDecimal](-1000000000, maxCurrencyAmount, "countryVatCorrection.error.outOfRange"))
    )
}
