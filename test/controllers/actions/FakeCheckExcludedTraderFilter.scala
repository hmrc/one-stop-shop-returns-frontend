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

package controllers.actions

import models.requests.OptionalDataRequest
import models.{Period, StandardPeriod}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}

class FakeCheckExcludedTraderFilter() extends CheckExcludedTraderFilterImpl(
  mock[StandardPeriod]
)(ExecutionContext.Implicits.global) {

  override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = {
    Future.successful(None)
  }
}

class FakeCheckExcludedTraderFilterProvider()
  extends CheckExcludedTraderFilterProvider()(ExecutionContext.Implicits.global) {

  override def apply(startReturnPeriod: Period): CheckExcludedTraderFilterImpl = new FakeCheckExcludedTraderFilter()
}
