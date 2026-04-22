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

package controllers.actions

import controllers.routes
import models.requests.OptionalDataRequest
import models.Period
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import services.ObligationsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckReturnsFilterImpl(
                              period: Period,
                              obligationsService: ObligationsService
                            )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[OptionalDataRequest] {

  override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    
    obligationsService.getFulfilledObligations(request.vrn).flatMap { obligations =>
      val hasSubmittedReturn = obligations.exists {
        obligationDetails =>
          val fulfilledPeriod = Period.fromEtmpPeriodKey(obligationDetails.periodKey)
          fulfilledPeriod == period
      }

      if (hasSubmittedReturn) {
        Some(Redirect(routes.PreviousReturnController.onPageLoad(period))).toFuture
      } else {
        None.toFuture
      }
    }
  }
  
}

class CheckReturnsFilterProvider @Inject()(
                                            obligationsService: ObligationsService
                                          )(implicit ec: ExecutionContext) {

  def apply(period: Period): CheckReturnsFilterImpl =
    new CheckReturnsFilterImpl(period, obligationsService)
}