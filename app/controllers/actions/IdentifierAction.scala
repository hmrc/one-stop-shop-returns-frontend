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

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.routes
import logging.Logging
import models.audit.LoginAuditModel
import models.requests.IdentifierRequest
import play.api.http.Status.NO_CONTENT
import play.api.mvc.Results._
import play.api.mvc._
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax._

import scala.concurrent.{ExecutionContext, Future}

class IdentifierAction @Inject()(
                                  override val authConnector: AuthConnector,
                                  auditService: AuditService,
                                  registrationConnector: RegistrationConnector,
                                  config: FrontendAppConfig
                                )
                                (implicit val executionContext: ExecutionContext)
  extends ActionRefiner[Request, IdentifierRequest]
    with AuthorisedFunctions with Logging {

  //noinspection ScalaStyle
  override def refine[A](request: Request[A]): Future[Either[Result, IdentifierRequest[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(
      AuthProviders(AuthProvider.GovernmentGateway) and
        (AffinityGroup.Individual or AffinityGroup.Organisation) and
        CredentialStrength(CredentialStrength.strong)
    ).retrieve( Retrievals.credentials and
                Retrievals.allEnrolments and
                Retrievals.affinityGroup and
                Retrievals.groupIdentifier and
                Retrievals.confidenceLevel and
                Retrievals.credentialRole ) {

      case Some(credentials) ~ enrolments ~ Some(Organisation) ~ Some(groupId) ~ _ ~ Some(credentialRole) if credentialRole == User =>
        (findVrnFromEnrolments(enrolments), ossEnrolmentEnabled(), hasOssEnrolment(enrolments)) match {
          case (Some(vrn), true, true) =>
            getSuccessfulResponse(request, credentials, vrn, groupId)
          case (Some(vrn), false, _) =>
            getSuccessfulResponse(request, credentials, vrn, groupId)
          case (Some(vrn), true, false) =>
            enrolUserIfRegistrationExists(request, credentials, vrn, groupId)
          case _ => throw InsufficientEnrolments()
        }

      case _ ~ _ ~ Some(Organisation) ~ _ ~ _ ~ Some(credentialRole) if credentialRole == Assistant =>
        throw UnsupportedCredentialRole()

      case Some(credentials) ~ enrolments ~ Some(Individual) ~ Some(groupId) ~ confidence ~ _ =>
        (findVrnFromEnrolments(enrolments), ossEnrolmentEnabled(), hasOssEnrolment(enrolments)) match {
          case (Some(vrn), true, true) =>
            checkConfidenceAndGetResponse(request, credentials, vrn, groupId, confidence)
          case (Some(vrn), false, _) =>
            checkConfidenceAndGetResponse(request, credentials, vrn, groupId, confidence)
          case (Some(vrn), true, false) =>
            if (confidence >= ConfidenceLevel.L200) {
              enrolUserIfRegistrationExists(request, credentials, vrn, groupId)
            } else {
              throw InsufficientConfidenceLevel()
            }
          case _ =>
            throw InsufficientEnrolments()
        }

      case _ =>
        throw new UnauthorizedException("Unable to retrieve authorisation data")

    } recoverWith {
      case _: NoActiveSession =>
        Left(Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))).toFuture
      case _: AuthorisationException =>
        Left(Redirect(routes.NotRegisteredController.onPageLoad())).toFuture
    }
  }

  private def enrolUserIfRegistrationExists[A](request: Request[A], credentials: Credentials, vrn: Vrn, groupId: String)
                                              (implicit hc: HeaderCarrier): Future[Either[Result, IdentifierRequest[A]]] = {

    registrationConnector.get().flatMap {
      case Some(_) =>
        logger.info(s"Registration found for user ${vrn.vrn} but no enrolment, attempting enrolment")
        registrationConnector.enrolUser().flatMap { response =>
          response.status match {
            case NO_CONTENT =>
              logger.info(s"Successfully retrospectively enrolled user ${vrn.vrn}")
              getSuccessfulResponse(request, credentials, vrn, groupId)
            case status =>
              logger.error(s"Failure enrolling an existing user, got status $status from registration service")
              throw new IllegalStateException("Existing user didn't have enrolment and was unable to enrol user")
          }
        }
      case _ => throw InsufficientEnrolments()
    }
  }


  private def getSuccessfulResponse[A](request: Request[A], credentials: Credentials, vrn: Vrn, groupId: String)
                                      (implicit hc: HeaderCarrier): Future[Either[Result, IdentifierRequest[A]]] = {
    val identifierRequest = IdentifierRequest(request, credentials, vrn)
    auditLogin(groupId, identifierRequest)
    Right(identifierRequest).toFuture
  }

  private def checkConfidenceAndGetResponse[A](request: Request[A], credentials: Credentials, vrn: Vrn, groupId: String, confidence: ConfidenceLevel)
                                              (implicit hc: HeaderCarrier): Future[Either[Result, IdentifierRequest[A]]] = {
    if (confidence >= ConfidenceLevel.L200) {
      getSuccessfulResponse(request, credentials, vrn, groupId)
    } else {
      throw InsufficientConfidenceLevel()
    }
  }

  private def ossEnrolmentEnabled(): Boolean = {
    config.ossEnrolmentEnabled
  }

  private def hasOssEnrolment(enrolments: Enrolments): Boolean = {
     enrolments.enrolments.exists(_.key == config.ossEnrolment)
  }

  private def findVrnFromEnrolments(enrolments: Enrolments): Option[Vrn] =
    enrolments.enrolments.find(_.key == "HMRC-MTD-VAT")
      .flatMap { enrolment => enrolment.identifiers.find(_.key == "VRN").map(e => Vrn(e.value))
      } orElse enrolments.enrolments.find(_.key == "HMCE-VATDEC-ORG")
      .flatMap { enrolment => enrolment.identifiers.find(_.key == "VATRegNo").map(e => Vrn(e.value)) }

  private def auditLogin(groupId: String, request: IdentifierRequest[_])(implicit hc: HeaderCarrier): Unit = {
    if(config.auditLogins) {
      val loginAuditModel = LoginAuditModel.build(groupId, request)
      auditService.audit(loginAuditModel)(hc, request)
    }

  }
}
