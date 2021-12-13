
# one-stop-shop-returns-frontend

This is the repository for One Stop Shop Returns Frontend

Backend: https://github.com/hmrc/one-stop-shop-returns

Stub: https://github.com/hmrc/one-stop-shop-returns-stub

Requirements
------------

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.

## Run the application

To update from Nexus and start all services from the RELEASE version instead of snapshot
```
sm --start ONE_STOP_SHOP_ALL -r
```

### To run the application locally execute the following:
```
sm --stop ONE_STOP_SHOP_FRONTEND
```
and 
```
sbt 'run 10204'
```

### Running correct version of mongo
We have introduced a transaction to the call to be able to ensure that both the vatreturn and correction get submitted to mongo.
Your local mongo is unlikely to be running a latest enough version and probably not in a replica set.
To do this, you'll need to stop your current mongo instance (docker ps to get the name of your mongo docker then docker stop <name> to stop)
Run at least 4.0 with a replica set:
```  
docker run --restart unless-stopped -d -p 27017-27019:27017-27019 --name mongo4 mongo:4.0 --replSet rs0
```
Connect to said replica set:
```
docker exec -it mongo4 mongo
```
When that console is there:
```
rs.initiate()
```
You then should be running 4.0 with a replica set. You may have to re-run the rs.initiate() after you've restarted


### Using the application
To log in using the Authority Wizard provide "continue url", "affinity group" and "enrolments" as follows:

![image](https://user-images.githubusercontent.com/48218839/145842535-6209b43e-483b-4874-b53d-364c9b121f14.png)

![image](https://user-images.githubusercontent.com/48218839/145842926-c318cb10-70c3-4186-a839-b1928c8e2625.png)


To be able to use the application you need to be registered for the service.



### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
