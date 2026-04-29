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

package crypto

import config.FrontendAppConfig
import models.{EncryptedUserAnswers, UserAnswers}
import play.api.libs.json.{JsObject, Json}
import services.crypto.EncryptionService

import javax.inject.Inject

class UserAnswersEncryptor @Inject()(
                                      appConfig: FrontendAppConfig,
                                      encryptionService: EncryptionService
                                    ) {

  protected val key: String = appConfig.encryptionKey

  def encryptUserAnswers(userAnswers: UserAnswers): EncryptedUserAnswers = {
    def encryptValue(value: String): String = encryptionService.encryptField(value)

    EncryptedUserAnswers(
      userId = userAnswers.userId,
      period = userAnswers.period,
      data = encryptValue(userAnswers.data.toString),
      lastUpdated = userAnswers.lastUpdated
    )
  }

  def decryptUserAnswers(encryptedUserAnswers: EncryptedUserAnswers): UserAnswers = {
    def decryptValue(encryptedValue: String): String = encryptionService.decryptField(encryptedValue)

    UserAnswers(
      userId = encryptedUserAnswers.userId,
      period = encryptedUserAnswers.period,
      data = Json.parse(decryptValue(encryptedUserAnswers.data)).as[JsObject],
      lastUpdated = encryptedUserAnswers.lastUpdated
    )
  }

}

