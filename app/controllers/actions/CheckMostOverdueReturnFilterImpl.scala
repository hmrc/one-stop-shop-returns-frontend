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

package controllers.actions

import connectors.ReturnStatusConnector
import controllers.routes
import models.requests.OptionalDataRequest
import models.{Period, SubmissionStatus}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckMostOverdueReturnFilterImpl(period: Period, connector: ReturnStatusConnector)
                            (implicit val executionContext: ExecutionContext)
  extends ActionFilter[OptionalDataRequest] {
  
  override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

    connector.listStatuses(request.registration.commencementDate) flatMap {
      case Right(previousPeriods) =>
         val dueReturns = previousPeriods.filter(p => p.status != SubmissionStatus.Complete).sortBy(_.period.firstDay)
        if(dueReturns.nonEmpty){
          if(dueReturns.head.period == period) {
          Future.successful(None)
          } else {
            Future(Some(Redirect(routes.CannotStartReturnController.onPageLoad())))
          }
        } else {
            Future(Some(Redirect(routes.NoOtherPeriodsAvailableController.onPageLoad())))
        }
      case _    => Future.successful(Some(Redirect(routes.JourneyRecoveryController.onPageLoad())))
    }
  }
}

class CheckMostOverdueReturnFilterProvider @Inject()(connector: ReturnStatusConnector)
                                          (implicit ec: ExecutionContext) {

 def apply(period: Period): CheckMostOverdueReturnFilterImpl =
   new CheckMostOverdueReturnFilterImpl(period, connector)
}