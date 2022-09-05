#!/bin/bash

echo ""
echo "Applying migration ExcludedNotPermitted"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /:period/excludedNotPermitted                       controllers.ExcludedNotPermittedController.onPageLoad(period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "excludedNotPermitted.title = excludedNotPermitted" >> ../conf/messages.en
echo "excludedNotPermitted.heading = excludedNotPermitted" >> ../conf/messages.en

echo "Migration ExcludedNotPermitted completed"
