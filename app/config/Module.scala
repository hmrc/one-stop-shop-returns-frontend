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

package config

import com.google.inject.AbstractModule
import controllers.actions._

import java.time.{Clock, ZoneId, ZoneOffset, ZonedDateTime}

class Module extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[DataRequiredAction]).to(classOf[DataRequiredActionImpl]).asEagerSingleton()
//    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone.withZone(ZoneOffset.UTC))
    bind(classOf[Clock]).toInstance(
      Clock.fixed(
        ZonedDateTime.of(
          2022, 4, 1, 1, 0, 0, 0, ZoneOffset.UTC
        ).toInstant,
        ZoneId.of("Australia/Melbourne")
      )
    )
    bind(classOf[AuthenticatedControllerComponents]).to(classOf[DefaultAuthenticatedControllerComponents]).asEagerSingleton()
  }
}
