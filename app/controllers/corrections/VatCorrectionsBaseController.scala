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

package controllers.corrections

import controllers.JourneyRecoverySyntax._
import models.requests.DataRequest
import models.{Index, Period}
import pages.corrections.CorrectionReturnPeriodPage
import play.api.mvc.{AnyContent, Result}
import queries.corrections.DeriveNumberOfCorrections

trait VatCorrectionsBaseController {

  protected def getNumberOfCorrections(periodIndex: Index)
                                      (block: (Int, Period) => Result)
                                      (implicit request: DataRequest[AnyContent]): Result =
    (for {
      numberOfCorrections <- request.userAnswers.get(DeriveNumberOfCorrections(periodIndex))
      correctionPeriod <- request.userAnswers.get(CorrectionReturnPeriodPage(periodIndex))
    } yield block(numberOfCorrections, correctionPeriod))
      .orRecoverJourney

}
