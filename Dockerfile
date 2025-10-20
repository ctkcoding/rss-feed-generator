# syntax=docker/dockerfile:1

ARG NODE_VERSION=22.18.0

FROM node:${NODE_VERSION}-alpine as development
ENV NODE_ENV development
ENV NODE_ENV=${NODE_ENV}


WORKDIR /usr/src/app
COPY package*.json ./
COPY tsconfig.json ./

RUN npm install
COPY . ./

RUN npm run build

FROM node:${NODE_VERSION}-alpine as production
ENV NODE_ENV production
ENV NODE_ENV=${NODE_ENV}

RUN apk add ffmpeg

WORKDIR /usr/src/app
COPY package*.json .
RUN npm ci --only=production
COPY --from=development /usr/src/app/dist ./dist

EXPOSE 3000

CMD ["node", "dist/server.js"]
