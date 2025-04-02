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

package models.etmp

import play.api.libs.json.{Json, OFormat}

import java.time.{LocalDate, LocalDateTime}

case class EtmpVatReturn(
                          returnReference: String,
                          returnVersion: LocalDateTime,
                          periodKey: String,
                          returnPeriodFrom: LocalDate,
                          returnPeriodTo: LocalDate,
                          goodsSupplied: Seq[EtmpVatReturnGoodsSupplied],
                          totalVATGoodsSuppliedGBP: BigDecimal,
                          goodsDispatched: Seq[EtmpVatReturnGoodsDispatched],
                          totalVatAmtDispatchedGBP: BigDecimal,
                          totalVATAmountPayable: BigDecimal,
                          totalVATAmountPayableAllSpplied: BigDecimal,
                          correctionPreviousVATReturn: Seq[EtmpVatReturnCorrection],
                          totalVATAmountFromCorrectionGBP: BigDecimal,
                          balanceOfVATDueForMS: Seq[EtmpVatReturnBalanceOfVatDue],
                          totalVATAmountDueForAllMSGBP: BigDecimal,
                          paymentReference: String
                        )

object EtmpVatReturn {

  implicit val format: OFormat[EtmpVatReturn] = Json.format[EtmpVatReturn]
}
