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

package connectors.financialdata

import logging.Logging
import models.financialdata.{Charge, PeriodWithOutstandingAmount}
import models.responses._
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object FinancialDataHttpParser extends Logging {

  type ChargeResponse = Either[ErrorResponse, Option[Charge]]

  implicit object ChargeReads extends HttpReads[ChargeResponse] {
    override def read(method: String, url: String, response: HttpResponse): ChargeResponse = {
      response.status match {
        case OK =>
          response.json.validateOpt[Charge] match {
            case JsSuccess(chargeOption, _) => Right(chargeOption)
            case JsError(errors) =>
              logger.warn(s"Failed trying to parse JSON $errors. Json was ${response.json}", errors)
              Left(InvalidJson)
          }
        case _ =>
          logger.warn("Failed to retrieve charge data")
          Left(UnexpectedResponseStatus(response.status, response.body))
      }
    }
  }

  type OutstandingPaymentsResponse = Either[ErrorResponse, Seq[PeriodWithOutstandingAmount]]

  implicit object PeriodWithOutstandingAmountReads extends HttpReads[OutstandingPaymentsResponse] {
    override def read(method: String, url: String, response: HttpResponse): OutstandingPaymentsResponse = {
      response.status match {
        case OK =>
          response.json.validate[Seq[PeriodWithOutstandingAmount]] match {
            case JsSuccess(periodsWithOutstandingAmounts, _) => Right(periodsWithOutstandingAmounts)
            case JsError(errors) =>
              logger.warn(s"Failed trying to parse JSON $errors. Json was ${response.json}", errors)
              Left(InvalidJson)
          }
        case _ =>
          logger.warn("Failed to retrieve outstanding amount data")
          Left(UnexpectedResponseStatus(response.status, response.body))
      }
    }
  }
}
