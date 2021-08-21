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

package controllers

import models.Index
import models.requests.DataRequest
import play.api.mvc.{AnyContent, Result}
import play.api.mvc.Results.Redirect
import queries.{DeriveNumberOfSalesFromEu, DeriveNumberOfSalesToEu}

trait SalesFromEuBaseController {

  protected def getNumberOfSalesFromEu(block: Int => Result)
                                      (implicit request: DataRequest[AnyContent]): Result =
    request.userAnswers
      .get(DeriveNumberOfSalesFromEu)
      .map(block(_))
      .getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))

  protected def getNumberOfSalesToEu(index: Index)
                                    (block: Int => Result)
                                    (implicit request: DataRequest[AnyContent]): Result =
    request.userAnswers
      .get(DeriveNumberOfSalesToEu(index))
      .map(block(_))
      .getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
}
