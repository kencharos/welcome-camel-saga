# camel saga pubsub sample

## Running the application in dev mode

runnint LRA coordinator and pubsub emulator by docker compose

```
docker comose up
```

run quarkus camel app

```shell script
./mvnw compile quarkus:dev
```

start saga transaction by PubSub service-a message

```

DATA=$(echo '{"title":"test"}' | base64)
curl -i -XPOST \
  -H "Content-Type:application/json" \
  -d "{\"messages\":[{\"data\": \"${DATA}\"}]}" \
  http://localhost:8681/v1/projects/pj-local/topics/service-a-topic:publish
```

