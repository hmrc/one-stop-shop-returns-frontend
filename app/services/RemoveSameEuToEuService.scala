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

package services

import models.{SalesFromEuWithOptionalVat, UserAnswers}
import queries.AllSalesFromEuQueryWithOptionalVatQuery

import scala.annotation.tailrec
import scala.util.Try

class RemoveSameEuToEuService {


  def deleteEuToSameEuCountry(userAnswers: UserAnswers): Try[UserAnswers] = {

    val allEuSaleWithIndex = userAnswers.get(AllSalesFromEuQueryWithOptionalVatQuery).getOrElse(List.empty).zipWithIndex

    @tailrec
    def recursivelyRemoveSalesFromEu(currentUserAnswers: UserAnswers, remainingEuSales: List[(SalesFromEuWithOptionalVat, Int)]): Try[UserAnswers] = {

      remainingEuSales match {
        case Nil => Try(currentUserAnswers)
        case (salesFromEuCountry, index) :: otherEuSales =>
          val updatedSalesFromCountry = salesFromEuCountry.salesFromCountry
            .map(_.filterNot(_.countryOfConsumption.code == salesFromEuCountry.countryOfSale.code))
            .filterNot(_.isEmpty)

          if (updatedSalesFromCountry.nonEmpty) {
            val updatedSalesFromEu = salesFromEuCountry.copy(salesFromCountry = updatedSalesFromCountry)

            val updatedAllSales = currentUserAnswers.get(AllSalesFromEuQueryWithOptionalVatQuery).getOrElse(List.empty)
              .updated(index, updatedSalesFromEu)

            val updatedUserAnswers = currentUserAnswers.set(AllSalesFromEuQueryWithOptionalVatQuery, updatedAllSales)

            recursivelyRemoveSalesFromEu(updatedUserAnswers.get, otherEuSales)

          } else {
            val updatedAllSales = currentUserAnswers.get(AllSalesFromEuQueryWithOptionalVatQuery).getOrElse(List.empty)
              .patch(index, Nil, 1)
            val updatedUserAnswers = currentUserAnswers.set(AllSalesFromEuQueryWithOptionalVatQuery, updatedAllSales)

            recursivelyRemoveSalesFromEu(updatedUserAnswers.get, otherEuSales)
          }
      }
    }

    recursivelyRemoveSalesFromEu(userAnswers, allEuSaleWithIndex)

  }
}
