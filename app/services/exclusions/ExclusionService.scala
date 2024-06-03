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

package services.exclusions

import config.FrontendAppConfig
import connectors.VatReturnConnector
import logging.Logging
import models.Period
import models.exclusions.ExclusionViewType._
import models.exclusions.{ExcludedTrader, ExclusionLinkView, ExclusionReason, ExclusionViewType}
import models.registration.Registration
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExclusionService @Inject()(
                                  connector: VatReturnConnector,
                                  frontendAppConfig: FrontendAppConfig,
                                  clock: Clock
                                ) extends Logging {

  def hasSubmittedFinalReturn(registration: Registration)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    registration.excludedTrader match {
      case Some(excludedTrader) =>
        connector.get(excludedTrader.finalReturnPeriod).map {
          case Right(_) => true
          case _ => false
        }
      case _ => Future.successful(false)
    }
  }

  def currentReturnIsFinal(registration: Registration, currentPeriod: Period)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {

    hasSubmittedFinalReturn(registration).flatMap {
      case true =>
        Future.successful(false)
      case _ =>
        registration.excludedTrader match {
          case Some(excludedTrader)
            if currentPeriod == excludedTrader.finalReturnPeriod => Future.successful(true)
          case _ =>
            Future.successful(false)
        }
    }
  }

  def calculateExclusionViewType(
                                  excludedTrader: Option[ExcludedTrader],
                                  canCancel: Boolean,
                                  hasSubmittedFinalReturn: Boolean
                                ): ExclusionViewType = {

    val isExcluded: Boolean = excludedTrader.exists(_.exclusionReason != ExclusionReason.Reversal)
    val isQuarantined: Boolean = excludedTrader.exists(_.quarantined)

    val today: LocalDate = LocalDate.now(clock)
    val isQuarantinedStillActive = isQuarantined && excludedTrader.exists(et => today.isAfter(et.rejoinDate) || today.isEqual(et.rejoinDate))

    (isExcluded, isQuarantinedStillActive, canCancel, hasSubmittedFinalReturn) match {
      case (true, true, _, _) => ExclusionViewType.Quarantined
      case (true, false, true, _) => ExclusionViewType.ReversalEligible
      case (true, false, false, true) => ExclusionViewType.RejoinEligible
      case (true, false, false, false) => ExclusionViewType.ExcludedFinalReturnPending
      case _ => ExclusionViewType.Default
    }
  }

  def getLink(exclusionViewType: ExclusionViewType)(implicit messages: Messages): Option[ExclusionLinkView] = {
    exclusionViewType match {
      case Quarantined | ExcludedFinalReturnPending => None
      case RejoinEligible => Some(
        ExclusionLinkView(
          displayText = messages("index.details.rejoinService"),
          id = "rejoin-this-service",
          href = s"#" // TODO add rejoin link
        )
      )
      case ReversalEligible => Some(
        ExclusionLinkView(
          displayText = messages("index.details.cancelRequestToLeave"),
          id = "cancel-request-to-leave",
          href = s"${frontendAppConfig.leaveOneStopShopUrl}/cancel-leave-scheme"
        )
      )
      case Default =>
        Some(
          ExclusionLinkView(
            displayText = messages("index.details.leaveThisService"),
            id = "leave-this-service",
            href = frontendAppConfig.leaveOneStopShopUrl
          )
        )
    }
  }
}
