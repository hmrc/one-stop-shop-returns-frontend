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

package services.crypto

import play.api.Configuration
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainText, SymmetricCryptoFactory}

import javax.inject.{Inject, Singleton}

@Singleton
class EncryptionService @Inject()(configuration: Configuration) {

  protected lazy val crypto: Encrypter with Decrypter = SymmetricCryptoFactory.aesCryptoFromConfig(
    baseConfigKey = "mongodb.encryption",
    config = configuration.underlying
  )

  def encryptField(rawValue: String): String = {
   crypto.encrypt(PlainText(rawValue)).value
  }

  def decryptField(decryptedValue: String): String = {
    crypto.decrypt(Crypted(decryptedValue)).value
  }

}