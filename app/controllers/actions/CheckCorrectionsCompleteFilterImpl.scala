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
import controllers.corrections.routes
import controllers.routes
import controllers.corrections.{routes => correctionsRoutes}
import models.{Index, Mode, Period}
import models.corrections.CorrectionToCountry
import models.requests.{DataRequest, OptionalDataRequest}
import pages.corrections.VatCorrectionsListPage
import play.api.data.FormError
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import queries.corrections.AllCorrectionCountriesQuery
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckCorrectionsCompleteFilterImpl(mode: Mode, period: Period, periodIndex: Index)
                            (implicit val executionContext: ExecutionContext)
  extends ActionFilter[DataRequest] {
  
  override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val incompleteCorrections: Option[List[CorrectionToCountry]] = request.userAnswers
      .get(AllCorrectionCountriesQuery(periodIndex))
      .map(_.filter(_.countryVatCorrection.isEmpty))

    incompleteCorrections match {
      case Some(corrections) if corrections.nonEmpty =>
        Future.successful(Some(Redirect(correctionsRoutes.VatCorrectionsListController.onPageLoad(mode, period, periodIndex))))
      case _ =>
        Future.successful(None)
    }
  }
}

class CheckCorrectionsCompleteFilterProvider @Inject()()
                                          (implicit ec: ExecutionContext) {

 def apply(mode: Mode, period: Period, periodIndex: Index): CheckCorrectionsCompleteFilterImpl =
   new CheckCorrectionsCompleteFilterImpl(mode, period, periodIndex)
}