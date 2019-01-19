#!/usr/bin/env bash

docker-compose -f docker-compose.yml -f docker-compose.db.yml up datomic-peer-ll datomic-transactor-ll
