/*
 * Copyright 2023 HM Revenue & Customs
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

package services.exclusions

import connectors.VatReturnConnector
import logging.Logging
import models.exclusions.ExcludedTrader
import models.requests.RegistrationRequest
import play.api.mvc.AnyContent
import services.PeriodService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExclusionService @Inject()(connector: VatReturnConnector, periodService: PeriodService) extends Logging {

  def hasSubmittedFinalReturn()(implicit hc: HeaderCarrier, ec: ExecutionContext, request: RegistrationRequest[AnyContent]): Future[Boolean] = {
    request.registration.excludedTrader match {
      case Some(ExcludedTrader(_, _, _, effectivePeriod)) =>
        connector.get(effectivePeriod).map {
          case Right(_) => true
          case _ => false
        }
      case _ => Future.successful(false)
    }
  }

  /* TODO:
       when we come to implementing API this could be a spot for performance improvement - instead of doing multiple connector calls, we could do:
       * A single call with multiple periods
       * Refactor to use the initial "returns seq" in the controller to work out if the current period
       the user is submitting compares to the exclusion effective date period (IE final return)
   */
  def currentReturnIsFinal()(implicit hc: HeaderCarrier, ec: ExecutionContext, request: RegistrationRequest[AnyContent]): Future[Boolean] = {

    hasSubmittedFinalReturn().flatMap {
      case true =>
        Future.successful(false)
      case _ =>
        request.registration.excludedTrader match {
          case Some(ExcludedTrader(_, _, _, effectivePeriod)) =>
            val previousPeriod = periodService.getPreviousPeriod(effectivePeriod)

            connector.get(previousPeriod).map {
              case Right(_) => true

              case _ => false
            }
          case _ =>
            Future.successful(false)
        }
    }
  }
}
