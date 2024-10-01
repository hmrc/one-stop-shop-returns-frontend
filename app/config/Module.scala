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

import com.google.inject.AbstractModule
import controllers.actions._
import play.api.{Configuration, Environment}

import java.time.{Clock, ZoneOffset}

class Module extends AbstractModule {

  private val defaultConfig = Configuration.load(Environment.simple())

  override def configure(): Unit = {
    bind(classOf[DataRequiredAction]).to(classOf[DataRequiredActionImpl]).asEagerSingleton()
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone.withZone(ZoneOffset.UTC))
    bind(classOf[AuthenticatedControllerComponents]).to(classOf[DefaultAuthenticatedControllerComponents]).asEagerSingleton()

    if (defaultConfig.get[Boolean]("create-internal-auth-token-on-start")) {
      bind(classOf[InternalAuthTokenInitialiser]).to(classOf[InternalAuthTokenInitialiserImpl]).asEagerSingleton()
    } else {
      bind(classOf[InternalAuthTokenInitialiser]).to(classOf[NoOpInternalAuthTokenInitialiser]).asEagerSingleton()
    }
  }
}
