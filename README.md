# Tracker backend

This is a backend for [our Lightweight Tracker](https://github.com/litvinov-tabor2022/lightweight-gps-tracker).

## Local development

This is just an SBT Scala app, so everything as usual.

## Deployment

It's supposed to be deployed as Docker image. Create it by:

```bash
sbt -Dversion="0.1.1" docker:publishLocal
```

### Configuration

The app needs to be configured before running. This is an example `application.conf`:

```hocon
allowed-origins = ["https://tracker.com"]

database {
  url = "jdbc:mysql://my.mysql.com/tracker"
  username = "tracker"
  password = "userPassword"
}

rabbitmq {
  connection {
    hosts = ["rabbitmq.com:5671"]
    virtualHost = "/"

    name = "TrackerBackendConnection"

    credentials {
      username = "tracker"
      password = "--thePassword--"
    }
  }

  consumer {
    name = "TrackerBackendConsumer"
    consumerTag = "TrackerBackendConsumer"
    queueName = "tracker-backend_coordinates"
  }
}
```

## API endpoints

`GET /status`  
Basic status endpoint.

`GET /tracks-list`  
List of available tracks.

`GET /trackers-list`  
List of available trackers.

`GET /track-create/${trackerId}/${name}`  
Create new track with ${name}, available for tracker ${trackerId}.

`GET /track-assign/${trackerId}/${trackId}`  
Assign ${trackId} to ${trackerId} as a current track.

`GET /list/gpx/${trackId}`  
Download track ${trackId} in GPX format.

`GET /list/json/${trackId}`  
Download track ${trackId} in JSON format (array of coordinates).

`GET /analyze/${trackId}`  
Analyze gaps in the track.

`GET /subscribe`  
WS endpoint for receiving updates. After calling this, client has to subscribe for particular stuff by sending a command.

Currently supported:

- `coordinates/${trackId}` - receive all future coordinates for track ${trackId}
