#!/bin/bash

echo ""
echo "Applying migration RemoveCountryCorrection"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/removeCountryCorrection                        controllers.RemoveCountryCorrectionController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/removeCountryCorrection                        controllers.RemoveCountryCorrectionController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeRemoveCountryCorrection                  controllers.RemoveCountryCorrectionController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeRemoveCountryCorrection                  controllers.RemoveCountryCorrectionController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "removeCountryCorrection.title = removeCountryCorrection" >> ../conf/messages.en
echo "removeCountryCorrection.heading = removeCountryCorrection" >> ../conf/messages.en
echo "removeCountryCorrection.checkYourAnswersLabel = removeCountryCorrection" >> ../conf/messages.en
echo "removeCountryCorrection.error.required = Select yes if removeCountryCorrection" >> ../conf/messages.en
echo "removeCountryCorrection.change.hidden = RemoveCountryCorrection" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryRemoveCountryCorrectionUserAnswersEntry: Arbitrary[(RemoveCountryCorrectionPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[RemoveCountryCorrectionPage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryRemoveCountryCorrectionPage: Arbitrary[RemoveCountryCorrectionPage.type] =";\
    print "    Arbitrary(RemoveCountryCorrectionPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(RemoveCountryCorrectionPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration RemoveCountryCorrection completed"
