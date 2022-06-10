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

package services.external

import logging.Logging
import models.{Period, SessionData}
import models.external.{ContinueReturn, ExternalRequest, ExternalResponse, NoMoreWelsh, Payment, ReturnsHistory, StartReturn, YourAccount}
import models.responses.{ErrorResponse, NotFound}
import queries.external.ExternalReturnUrlQuery
import repositories.SessionRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExternalService @Inject()(sessionRepository: SessionRepository)(implicit executionContext: ExecutionContext) extends Logging {



  def getExternalResponse(externalRequest: ExternalRequest,
                          userId: String,
                          page: String,
                          period: Option[Period] = None,
                          language: Option[String] = None,
                          amountInPence: Option[Long] = None): Either[ErrorResponse, ExternalResponse] = {

    val response = (page, period, amountInPence) match {
      case (YourAccount.name, None, None) =>
        saveReturnUrl(userId, externalRequest)
        Right(ExternalResponse(YourAccount.url))
      case (ReturnsHistory.name, None, None) =>
        saveReturnUrl(userId, externalRequest)
        Right(ExternalResponse(ReturnsHistory.url))
      case (StartReturn.name, Some(returnPeriod), None) =>
        saveReturnUrl(userId, externalRequest)
        Right(ExternalResponse(StartReturn.url(returnPeriod)))
      case (ContinueReturn.name, Some(returnPeriod), None) =>
        saveReturnUrl(userId, externalRequest)
        Right(ExternalResponse(ContinueReturn.url(returnPeriod)))
      case (Payment.name, Some(returnPeriod), Some(amount)) =>
        saveReturnUrl(userId, externalRequest)
        Right(ExternalResponse(Payment.url(returnPeriod, amount)))
      case _ => Left(NotFound)
    }

    (response, language) match {
      case (Right(externalResponse), Some("cy")) => Right(wrapLanguageWarning(externalResponse))
      case _ => response
    }
  }

  private def wrapLanguageWarning(response: ExternalResponse): ExternalResponse = {
     ExternalResponse(NoMoreWelsh.url(response.redirectUrl))
  }

  private def saveReturnUrl(userId: String, externalRequest: ExternalRequest): Future[Boolean] = {
    for {
      sessionData <- sessionRepository.get(userId)
      updatedData <- Future.fromTry(sessionData.headOption.getOrElse(SessionData(userId)).set(ExternalReturnUrlQuery.path, externalRequest.returnUrl))
      savedData <- sessionRepository.set(updatedData)
    } yield {
      savedData
    }
  }.recover{
    case e: Exception =>
      logger.error(s"An error occurred while saving the external returnUrl in the session, ${e.getMessage}")
      false
  }
}
