#!/bin/bash

echo ""
echo "Applying migration DeleteReturn"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/deleteReturn                        controllers.DeleteReturnController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/deleteReturn                        controllers.DeleteReturnController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeDeleteReturn                  controllers.DeleteReturnController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeDeleteReturn                  controllers.DeleteReturnController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "deleteReturn.title = deleteReturn" >> ../conf/messages.en
echo "deleteReturn.heading = deleteReturn" >> ../conf/messages.en
echo "deleteReturn.checkYourAnswersLabel = deleteReturn" >> ../conf/messages.en
echo "deleteReturn.error.required = Select yes if deleteReturn" >> ../conf/messages.en
echo "deleteReturn.change.hidden = DeleteReturn" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryDeleteReturnUserAnswersEntry: Arbitrary[(DeleteReturnPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[DeleteReturnPage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryDeleteReturnPage: Arbitrary[DeleteReturnPage.type] =";\
    print "    Arbitrary(DeleteReturnPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(DeleteReturnPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration DeleteReturn completed"
