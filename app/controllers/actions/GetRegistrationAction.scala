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

import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.routes
import models.registration.Registration
import models.requests.{IdentifierRequest, RegistrationRequest}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import repositories.RegistrationRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetRegistrationAction @Inject()(
                                       val registrationRepository: RegistrationRepository,
                                       val registrationConnector: RegistrationConnector,
                                       appConfig: FrontendAppConfig
                                     )(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[IdentifierRequest, RegistrationRequest] {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, RegistrationRequest[A]]] =
    getCachedRegistrationIfEnabled(request) flatMap {
      case Some(registration) =>
        Right(RegistrationRequest(request.request, request.credentials, request.vrn, registration)).toFuture
      case None =>
        val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request.request, request.request.session)
        registrationConnector.get()(hc) flatMap {
          case Some(registration) =>
            setCachedRegistrationIfEnabled(request, registration).map {
              _ =>
                Right(RegistrationRequest(request.request, request.credentials, request.vrn, registration))
            }
          case None =>
            Left(Redirect(routes.NotRegisteredController.onPageLoad())).toFuture
        }
    }

  private def getCachedRegistrationIfEnabled[A](request: IdentifierRequest[A]): Future[Option[Registration]] = {
    if(appConfig.cacheRegistrations) {
      registrationRepository.get(request.userId)
    } else {
      Future.successful(None)
    }
  }

  private def setCachedRegistrationIfEnabled[A](request: IdentifierRequest[A], registration: Registration): Future[Boolean] = {
    if(appConfig.cacheRegistrations) {
      registrationRepository.set(request.userId, registration)
    } else {
      Future.successful(true)
    }

  }
}