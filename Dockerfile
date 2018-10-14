FROM clojure:lein-alpine AS frontend-builder

WORKDIR /app
COPY ./frontend/project.clj /app
RUN lein deps
COPY ./frontend /app
RUN lein cljsbuild once min
RUN find target

FROM clojure:lein-alpine AS backend-builder

ARG DATOMIC_USER
ARG DATOMIC_PASS

WORKDIR /app
COPY ./backend/project.clj /app
RUN lein deps
COPY ./backend /app
COPY --from=frontend-builder /app/target/cljsbuild/public/js/app.js /app/resources/public
COPY --from=frontend-builder /app/resources/public/* /app/resources/public/
RUN sed -i -e 's/cljs-out\/dev-main.js/\/app.js/g' /app/resources/public/index.html
RUN lein uberjar

FROM openjdk:alpine
WORKDIR /app
COPY --from=backend-builder /app/target/language-lessons.jar /app
CMD java -jar /app/language-lessons.jar
EXPOSE 3000
