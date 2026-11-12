#!/bin/bash

echo ""
echo "Applying migration NoSavedReturns"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /:period/noSavedReturns                       controllers.NoSavedReturnsController.onPageLoad(period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "noSavedReturns.title = noSavedReturns" >> ../conf/messages.en
echo "noSavedReturns.heading = noSavedReturns" >> ../conf/messages.en

echo "Migration NoSavedReturns completed"
