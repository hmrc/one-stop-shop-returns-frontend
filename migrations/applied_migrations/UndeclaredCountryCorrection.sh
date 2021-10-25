#!/bin/bash

echo ""
echo "Applying migration UndeclaredCountryCorrection"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/undeclaredCountryCorrection                        controllers.UndeclaredCountryCorrectionController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/undeclaredCountryCorrection                        controllers.UndeclaredCountryCorrectionController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeUndeclaredCountryCorrection                  controllers.UndeclaredCountryCorrectionController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeUndeclaredCountryCorrection                  controllers.UndeclaredCountryCorrectionController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "undeclaredCountryCorrection.title = undeclaredCountryCorrection" >> ../conf/messages.en
echo "undeclaredCountryCorrection.heading = undeclaredCountryCorrection" >> ../conf/messages.en
echo "undeclaredCountryCorrection.checkYourAnswersLabel = undeclaredCountryCorrection" >> ../conf/messages.en
echo "undeclaredCountryCorrection.error.required = Select yes if undeclaredCountryCorrection" >> ../conf/messages.en
echo "undeclaredCountryCorrection.change.hidden = UndeclaredCountryCorrection" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryUndeclaredCountryCorrectionUserAnswersEntry: Arbitrary[(UndeclaredCountryCorrectionPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[UndeclaredCountryCorrectionPage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryUndeclaredCountryCorrectionPage: Arbitrary[UndeclaredCountryCorrectionPage.type] =";\
    print "    Arbitrary(UndeclaredCountryCorrectionPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(UndeclaredCountryCorrectionPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration UndeclaredCountryCorrection completed"
