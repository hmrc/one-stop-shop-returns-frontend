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

package queries.corrections

import models.StandardPeriod
import models.corrections.PeriodWithCorrections
import pages.PageConstants
import play.api.libs.json.{JsObject, JsPath}
import queries.Derivable

case object DeriveCompletedCorrectionPeriods extends Derivable[List[JsObject], List[StandardPeriod]] {

  override val derive: List[JsObject] => List[StandardPeriod] = _.flatMap(_.asOpt[PeriodWithCorrections]).filter(_.correctionsToCountry.isDefined).map(_.correctionReturnPeriod)

  override def path: JsPath = JsPath \ PageConstants.corrections
}
