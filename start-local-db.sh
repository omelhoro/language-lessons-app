#!/usr/bin/env bash

docker-compose -f docker-compose.yml -f docker-compose.local.yml up datomic-peer-ll datomic-transactor-ll
