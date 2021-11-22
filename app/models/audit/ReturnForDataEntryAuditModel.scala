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

package models.audit

import models.{PaymentReference, ReturnReference}
import models.requests.VatReturnRequest
import models.requests.corrections.CorrectionRequest
import play.api.libs.json.{JsObject, JsValue, Json}

case class ReturnForDataEntryAuditModel(
                                         vatReturnRequest: VatReturnRequest,
                                         correctionRequest: Option[CorrectionRequest],
                                         reference: ReturnReference,
                                         paymentReference: PaymentReference
                                       ) extends JsonAuditModel {

  override val auditType: String       = "ReturnForDataEntry"
  override val transactionName: String = "return-for-data-entry"

  private val salesFromNi: List[JsObject] =
    vatReturnRequest.salesFromNi.flatMap {
      salesFromCountry =>
        salesFromCountry.amounts.map {
          salesDetails =>
            Json.obj(
              "countryOfConsumption" -> Json.toJson(salesFromCountry.countryOfConsumption.name),
              "vatRate"              -> salesDetails.vatRate.rate,
              "vatRateType"          -> salesDetails.vatRate.rateType,
              "netValueOfSales"      -> salesDetails.netValueOfSales,
              "vatOnSales"           -> salesDetails.vatOnSales.amount
            )
        }
    }

  private val salesFromEu: List[JsObject] = {
    vatReturnRequest.salesFromEu.flatMap {
      salesFromEuCountry =>
        salesFromEuCountry.sales.flatMap {
          salesToCountry =>
            salesToCountry.amounts.map {
              salesDetails =>

                val taxIdentifierObj =
                  salesFromEuCountry.taxIdentifier.map {
                    id =>
                      Json.obj(
                        "taxIdentifierType" -> id.identifierType.toString,
                        "taxIdentifier"     -> id.value
                      )
                  }.getOrElse(Json.obj())

                Json.obj(
                  "countryOfSale"        -> Json.toJson(salesFromEuCountry.countryOfSale.name),
                  "countryOfConsumption" -> Json.toJson(salesToCountry.countryOfConsumption.name),
                  "vatRate"              -> salesDetails.vatRate.rate,
                  "vatRateType"          -> salesDetails.vatRate.rateType,
                  "netValueOfSales"      -> salesDetails.netValueOfSales,
                  "vatOnSales"           -> salesDetails.vatOnSales.amount
                ) ++ taxIdentifierObj
            }
        }
    }
  }

  private def periodsWithCorrections(correctionRequest: CorrectionRequest): List[JsObject] = {
    correctionRequest.corrections.map {
      correctionsToCountry =>

        val correctionToCountry = correctionsToCountry.correctionsToCountry.map {
          correctionCountry =>
            Json.obj(
              "correctionCountry" -> Json.toJson(correctionCountry.correctionCountry.name),
              "countryVatCorrectionAmount" -> correctionCountry.countryVatCorrection
            )
        }

        Json.obj(
          "correctionPeriod" -> correctionsToCountry.correctionReturnPeriod,
          "correctionToCountry" -> correctionToCountry
        )
    }
  }

  private val corrections: JsObject = {
    correctionRequest match {
      case Some(corrRequest) =>
        Json.obj(
          "corrections" -> periodsWithCorrections(corrRequest)
        )
      case _ => Json.obj()
    }

  }

  override val detail: JsValue = Json.obj(
    "vatRegistrationNumber" -> vatReturnRequest.vrn.vrn,
    "period"                -> Json.toJson(vatReturnRequest.period),
    "returnReference"       -> Json.toJson(reference),
    "paymentReference"      -> Json.toJson(paymentReference),
    "salesFromNi"           -> salesFromNi,
    "salesFromEu"           -> salesFromEu
  ) ++ corrections

}
