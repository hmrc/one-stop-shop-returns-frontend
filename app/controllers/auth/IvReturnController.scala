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

package controllers.auth

import config.FrontendAppConfig
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, OnlyRelative, RedirectUrl}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.iv._

import javax.inject.Inject

class IvReturnController @Inject()(
                                    override val messagesApi: MessagesApi,
                                    val controllerComponents: MessagesControllerComponents,
                                    errorView: IvErrorView,
                                    incompleteView: IvIncompleteView,
                                    insufficientEvidenceView: InsufficientEvidenceView,
                                    ivLockedOutView: IvLockedOutView,
                                    ivPreconditionFailedView: IvPreconditionFailedView,
                                    ivTechnicalIssueView: IvTechnicalIssueView,
                                    ivTimeoutView: IvTimeoutView,
                                    ivUserAbortedView: IvUserAbortedView,
                                    frontendAppConfig: FrontendAppConfig,
                                    ivNotEnoughEvidenceView: IvNotEnoughEvidenceView,
                                    failedMatchingView: IvFailedMatchingView,
                                    ivFailedView: IvFailedView
                                  ) extends FrontendBaseController with I18nSupport {

  private val redirectPolicy = OnlyRelative | AbsoluteWithHostnameFromAllowlist(frontendAppConfig.allowedRedirectUrls: _*)

  def error(continueUrl: RedirectUrl): Action[AnyContent] = Action {
    implicit request =>
      Ok(errorView(continueUrl.get(redirectPolicy).url))
  }

  def failedMatching(continueUrl: RedirectUrl): Action[AnyContent] = Action {
    implicit request =>
      Ok(failedMatchingView(continueUrl.get(redirectPolicy).url))
  }

  def failed(continueUrl: RedirectUrl): Action[AnyContent] = Action {
    implicit request =>
      Ok(ivFailedView(continueUrl.get(redirectPolicy).url))
  }

  def incomplete(continueUrl: RedirectUrl): Action[AnyContent] = Action {
    implicit request =>
      Ok(incompleteView(continueUrl.get(redirectPolicy).url))
  }

  def insufficientEvidence(continueUrl: RedirectUrl): Action[AnyContent] = Action {
    implicit request =>
      Ok(insufficientEvidenceView(continueUrl.get(redirectPolicy).url))
  }

  def lockedOut(continueUrl: RedirectUrl): Action[AnyContent] = Action {
    implicit request =>
      Ok(ivLockedOutView(continueUrl.get(redirectPolicy).url))
  }

  def notEnoughEvidenceSources(continueUrl: RedirectUrl): Action[AnyContent] = Action {
    implicit request =>
      Ok(ivNotEnoughEvidenceView(continueUrl.get(redirectPolicy).url))
  }

  def preconditionFailed(continueUrl: RedirectUrl): Action[AnyContent] = Action {
    implicit request =>
      Ok(ivPreconditionFailedView(continueUrl.get(redirectPolicy).url))
  }

  def technicalIssue(continueUrl: RedirectUrl): Action[AnyContent] = Action {
    implicit request =>
      Ok(ivTechnicalIssueView(continueUrl.get(redirectPolicy).url))
  }

  def timeout(continueUrl: RedirectUrl): Action[AnyContent] = Action {
    implicit request =>
      Ok(ivTimeoutView(continueUrl.get(redirectPolicy).url))
  }

  def userAborted(continueUrl: RedirectUrl): Action[AnyContent] = Action {
    implicit request =>
      Ok(ivUserAbortedView(continueUrl.get(redirectPolicy).url))
  }
}