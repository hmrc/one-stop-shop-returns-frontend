#!/bin/bash

echo ""
echo "Applying migration NewPrevioousReturn"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /:period/newPrevioousReturn                       controllers.NewPrevioousReturnController.onPageLoad(period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "newPrevioousReturn.title = newPrevioousReturn" >> ../conf/messages.en
echo "newPrevioousReturn.heading = newPrevioousReturn" >> ../conf/messages.en

echo "Migration NewPrevioousReturn completed"
