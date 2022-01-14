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

package queries

import models.{Index, VatRate}
import pages.PageConstants._
import play.api.libs.json.JsPath

case class VatRateFromEuQuery(countryFromIndex: Index, countryToIndex: Index, vatRateIndex: Index) extends Gettable[VatRate] {

  override def path: JsPath =
    JsPath \ salesFromEu \ countryFromIndex.position \ salesFromCountry \ countryToIndex.position \ vatRates \ vatRateIndex.position
}
