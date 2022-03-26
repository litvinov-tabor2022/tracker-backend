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
database {
  url = "jdbc:mysql://my.mysql.com/tracker"
  username = "tracker"
  password = "userPassword"
}

mqtt {
  host = "my.mqqt.server.com"

  user = "myUser"
  pass = "myPassword"

  subscriber-name = "TrackerSubscriber"
  topic = "gps-tracker-test"
}

```
