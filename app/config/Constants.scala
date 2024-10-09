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

package config

object Constants {

  val maxCurrencyAmount: BigDecimal = 1000000000
  val minCurrencyAmount: BigDecimal = -1000000000
  val returnsConfirmationTemplateId = "oss_returns_email_confirmation"
  val overdueReturnsConfirmationTemplateId = "oss_overdue_returns_email_confirmation"
  val returnsConfirmationNoVatOwedTemplateId = "oss_returns_email_confirmation_no_vat_owed"
  val exclusionCodeSixFollowingMonth: Int = 1
  val exclusionCodeSixTenthOfMonth: Int = 10
  val submittedReturnsPeriodsLimit: Int = 6

}
