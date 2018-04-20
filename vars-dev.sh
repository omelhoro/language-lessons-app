#!/usr/bin/env bash
export PEER_HOST=localhost:8998
export DATABASE_AKEY=123
export DATABASE_PASS=mega-pass
export DATABASE_NAME=language-lessons
export CHROME_BIN=$(node -p -e "require('puppeteer').executablePath()")