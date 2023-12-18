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

package models.external

import models.Period

sealed trait ExternalTargetPage {
  val name: String
}

sealed trait ParameterlessUrl {
 val url: String
}

sealed trait UrlWithPeriod {
  def url(period: Period) : String
}

sealed trait UrlWithPeriodAndAmount {
  def url(period: Period, amountInPence: Long) : String
}

case object YourAccount extends ExternalTargetPage with ParameterlessUrl {
  override val name: String = "your-account"
  override val url: String = controllers.routes.YourAccountController.onPageLoad().url
}

case object ReturnsHistory extends ExternalTargetPage with ParameterlessUrl {
  override val name: String = "returns-history"
  override val url: String = controllers.routes.SubmittedReturnsHistoryController.onPageLoad().url
}

case object StartReturn extends ExternalTargetPage with UrlWithPeriod {
  override val name: String = "start-your-return"
  override def url(period: Period): String = controllers.routes.StartReturnController.onPageLoad(period).url
}

case object ContinueReturn extends ExternalTargetPage with UrlWithPeriod {
  override val name: String = "continue-your-return"
  override def url(period: Period): String = controllers.routes.ContinueReturnController.onPageLoad(period).url
}

case object NoMoreWelsh extends ExternalTargetPage {
  override val name: String = "no-more-welsh"
  def url(targetUrl: String): String = controllers.external.routes.NoMoreWelshController.onPageLoad(Some(targetUrl)).url
}

case object Payment extends ExternalTargetPage with ParameterlessUrl {
  override val name: String = "make-payment"
  override val url: String = controllers.routes.WhichVatPeriodToPayController.onPageLoad().url
}