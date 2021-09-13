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

package controllers.actions

import connectors.VatReturnConnector
import controllers.routes
import models.Period
import models.Quarter.Q3
import models.domain.VatReturn
import models.requests.OptionalDataRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckReturnsFilterImpl @Inject()(connector: VatReturnConnector)(implicit val executionContext: ExecutionContext)

  extends CheckReturnsFilter {
  
  override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    connector.get(Period(2021, Q3)) map {
      case Right(_) => Some(Redirect(routes.PreviousReturnController.onPageLoad(Period(2021, Q3))))
      case _    => None
    }
  }
}

trait CheckReturnsFilter extends ActionFilter[OptionalDataRequest]