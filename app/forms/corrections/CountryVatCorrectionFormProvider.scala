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

import config.Constants.{maxCurrencyAmount, minCurrencyAmount}
import forms.mappings.Mappings
import play.api.data.Form

import javax.inject.Inject

class CountryVatCorrectionFormProvider @Inject() extends Mappings {

  def apply(country:String, minimiumCorrection:BigDecimal, undeclaredCountry: Boolean): Form[BigDecimal] =
    Form(
      "value" -> currency(
        "countryVatCorrection.error.required",
        "countryVatCorrection.error.wholeNumber",
        "countryVatCorrection.error.nonNumeric",
        args = Seq(country))
          .verifying(inRange[BigDecimal](
            minCurrencyAmount, maxCurrencyAmount,
            if(undeclaredCountry) "countryVatCorrection.error.outOfRange.undeclared" else "countryVatCorrection.error.outOfRange")
          )
          .verifying("countryVatCorrection.error.nonZero", input => input != 0)
          .verifying(minimumValue(minimiumCorrection, "countryVatCorrection.error.negative"))
    )
}
