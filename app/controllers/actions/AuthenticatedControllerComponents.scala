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

import models.Period
import models.requests.{DataRequest, IdentifierRequest, OptionalDataRequest, RegistrationRequest}
import play.api.http.FileMimeTypes
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc._
import repositories.SessionRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext

trait AuthenticatedControllerComponents extends MessagesControllerComponents {

  def actionBuilder: DefaultActionBuilder
  def sessionRepository: SessionRepository
  def identify: AuthenticatedIdentifierAction
  def getRegistration: GetRegistrationAction
  def checkReturn: CheckReturnsFilter
  def getData: DataRetrievalActionProvider
  def requireData: DataRequiredAction

  def auth: ActionBuilder[IdentifierRequest, AnyContent] =
    actionBuilder andThen identify

  def authAndGetRegistration: ActionBuilder[RegistrationRequest, AnyContent] =
    auth andThen getRegistration

  def authAndGetOptionalData(period: Period): ActionBuilder[OptionalDataRequest, AnyContent] =
    auth andThen getRegistration andThen getData(period) andThen checkReturn

  def authAndGetData(period: Period): ActionBuilder[DataRequest, AnyContent] =
    authAndGetOptionalData(period) andThen requireData
}

case class DefaultAuthenticatedControllerComponents @Inject()(
                                                               messagesActionBuilder: MessagesActionBuilder,
                                                               actionBuilder: DefaultActionBuilder,
                                                               parsers: PlayBodyParsers,
                                                               messagesApi: MessagesApi,
                                                               langs: Langs,
                                                               fileMimeTypes: FileMimeTypes,
                                                               executionContext: ExecutionContext,
                                                               sessionRepository: SessionRepository,
                                                               identify: AuthenticatedIdentifierAction,
                                                               getRegistration: GetRegistrationAction,
                                                               checkReturn: CheckReturnsFilter,
                                                               getData: DataRetrievalActionProvider,
                                                               requireData: DataRequiredAction
                                                             ) extends AuthenticatedControllerComponents
