FROM node:18-alpine AS build

WORKDIR /app

COPY package.json package-lock.json* ./

# Use cache mount for npm and install dependencies
# Note: switched from 'ci' to 'install' because lockfile is out of sync
RUN --mount=type=cache,target=/root/.npm \
    npm install --no-audit

COPY . .

ARG REACT_APP_API_URL
ENV REACT_APP_API_URL=$REACT_APP_API_URL

RUN npm run build

FROM nginx:alpine

ENV TZ=Europe/Moscow
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone

COPY --from=build /app/build /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf

RUN chmod -R 755 /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

