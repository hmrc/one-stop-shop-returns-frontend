#!/bin/bash

echo ""
echo "Applying migration VatOnSalesFromNi"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/vatOnSalesFromNi                  controllers.VatOnSalesFromNiController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/vatOnSalesFromNi                  controllers.VatOnSalesFromNiController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeVatOnSalesFromNi                        controllers.VatOnSalesFromNiController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeVatOnSalesFromNi                        controllers.VatOnSalesFromNiController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "vatOnSalesFromNi.title = VatOnSalesFromNi" >> ../conf/messages.en
echo "vatOnSalesFromNi.heading = VatOnSalesFromNi" >> ../conf/messages.en
echo "vatOnSalesFromNi.checkYourAnswersLabel = VatOnSalesFromNi" >> ../conf/messages.en
echo "vatOnSalesFromNi.error.nonNumeric = Enter your vatOnSalesFromNi using numbers" >> ../conf/messages.en
echo "vatOnSalesFromNi.error.required = Enter your vatOnSalesFromNi" >> ../conf/messages.en
echo "vatOnSalesFromNi.error.wholeNumber = Enter your vatOnSalesFromNi using whole numbers" >> ../conf/messages.en
echo "vatOnSalesFromNi.error.outOfRange = VatOnSalesFromNi must be between {0} and {1}" >> ../conf/messages.en
echo "vatOnSalesFromNi.change.hidden = VatOnSalesFromNi" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryVatOnSalesFromNiUserAnswersEntry: Arbitrary[(VatOnSalesFromNiPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[VatOnSalesFromNiPage.type]";\
    print "        value <- arbitrary[Int].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryVatOnSalesFromNiPage: Arbitrary[VatOnSalesFromNiPage.type] =";\
    print "    Arbitrary(VatOnSalesFromNiPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(VatOnSalesFromNiPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration VatOnSalesFromNi completed"
