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

package controllers.actions

import connectors.ReturnStatusConnector
import controllers.routes
import models.SubmissionStatus
import models.requests.DataRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckSubmittedReturnsFilterImpl(connector: ReturnStatusConnector)
                            (implicit val executionContext: ExecutionContext)
  extends ActionFilter[DataRequest] {
  
  override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    connector.listStatuses(request.registration.commencementDate) flatMap {
      case Right(previousPeriods) => if(previousPeriods.exists(p => p.status == SubmissionStatus.Complete)) {
        Future.successful(None)
      } else {
        Future(Some(Redirect(routes.CheckYourAnswersController.onPageLoad(request.userAnswers.period))))
      }
      case _    => Future(Some(Redirect(routes.CheckYourAnswersController.onPageLoad(request.userAnswers.period))))
    }
  }
}

class CheckSubmittedReturnsFilterProvider @Inject()(connector: ReturnStatusConnector)
                                          (implicit ec: ExecutionContext) {

 def apply(): CheckSubmittedReturnsFilterImpl =
   new CheckSubmittedReturnsFilterImpl(connector)
}