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

import config.FrontendAppConfig
import models.requests.IdentifierRequest
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc._
import services.AuditService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.domain.Vrn
import utils.FutureSyntax._

import scala.concurrent.{ExecutionContext, Future}

class FakeIdentifierAction extends IdentifierAction(
  mock[AuthConnector],
  mock[AuditService],
  mock[FrontendAppConfig]
)(ExecutionContext.Implicits.global) {

  override def refine[A](request: Request[A]): Future[Either[Result, IdentifierRequest[A]]] =
    Right(IdentifierRequest(
      request,
      Credentials("12345-credId", "GGW"),
      Vrn("123456789")
    )).toFuture
}
