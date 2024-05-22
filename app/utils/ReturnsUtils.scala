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

package utils

import models.SubmissionStatus.{Complete, Excluded}
import viewmodels.yourAccount.Return

import java.time.LocalDate

object ReturnsUtils {

  def hasDueReturnThreeYearsOld(returns: Seq[Return]): Boolean = {
    returns.exists { `return` =>
      if(`return`.submissionStatus == Complete || `return`.submissionStatus == Excluded) {
        false
      } else {
        isThreeYearsOld(`return`.dueDate)
      }
    }
  }

  def isThreeYearsOld(dueDate: LocalDate): Boolean = {
    val threeYearsAgo = LocalDate.now().minusYears(3)
    dueDate.isBefore(threeYearsAgo) || dueDate.isEqual(threeYearsAgo)
  }

  def isLessThanThreeYearsOld(dueDate: LocalDate): Boolean = {
    val threeYearsAgo = LocalDate.now().minusYears(3)
    dueDate.isAfter(threeYearsAgo)
  }

  def hasDueReturnsLessThanThreeYearsOld(returns: Seq[Return]): Boolean = returns.count { `return` =>
    if (`return`.submissionStatus == Complete || `return`.submissionStatus == Excluded) {
      false
    } else {
      isLessThanThreeYearsOld(`return`.dueDate)
    }
  } >= 2
}
