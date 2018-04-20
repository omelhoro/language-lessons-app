FROM clojure:lein-alpine AS builder

ARG DATOMIC_USER
ARG DATOMIC_PASS

WORKDIR /app
COPY ./project.clj /app
RUN lein deps
COPY ./ /app
RUN lein uberjar

FROM openjdk:alpine
WORKDIR /app
COPY --from=builder /app/target/language-lessons.jar /app
CMD java -jar /app/language-lessons.jar
EXPOSE 3000
