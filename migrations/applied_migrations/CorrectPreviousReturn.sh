#!/bin/bash

echo ""
echo "Applying migration CorrectPreviousReturn"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/correctPreviousReturn                        controllers.CorrectPreviousReturnController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/correctPreviousReturn                        controllers.CorrectPreviousReturnController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeCorrectPreviousReturn                  controllers.CorrectPreviousReturnController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeCorrectPreviousReturn                  controllers.CorrectPreviousReturnController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "correctPreviousReturn.title = correctPreviousReturn" >> ../conf/messages.en
echo "correctPreviousReturn.heading = correctPreviousReturn" >> ../conf/messages.en
echo "correctPreviousReturn.checkYourAnswersLabel = correctPreviousReturn" >> ../conf/messages.en
echo "correctPreviousReturn.error.required = Select yes if correctPreviousReturn" >> ../conf/messages.en
echo "correctPreviousReturn.change.hidden = CorrectPreviousReturn" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCorrectPreviousReturnUserAnswersEntry: Arbitrary[(CorrectPreviousReturnPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[CorrectPreviousReturnPage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCorrectPreviousReturnPage: Arbitrary[CorrectPreviousReturnPage.type] =";\
    print "    Arbitrary(CorrectPreviousReturnPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(CorrectPreviousReturnPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration CorrectPreviousReturn completed"
