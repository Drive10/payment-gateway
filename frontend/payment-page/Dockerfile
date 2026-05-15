FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build:docker

FROM nginx:1.25-alpine
RUN rm -rf /usr/share/nginx/html/*
COPY --from=build /app/dist /usr/share/nginx/html
COPY --from=build /app/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/public/env.template.js /usr/share/nginx/html/env.template.js

RUN echo '#!/bin/sh' > /docker-entrypoint.sh && \
    echo 'if [ -n "$API_BASE_URL" ]; then' >> /docker-entrypoint.sh && \
    echo '  sed "s|\${API_BASE_URL}|$API_BASE_URL|g" /usr/share/nginx/html/env.template.js > /usr/share/nginx/html/env.js' >> /docker-entrypoint.sh && \
    echo 'fi' >> /docker-entrypoint.sh && \
    echo 'exec nginx -g "daemon off;"' >> /docker-entrypoint.sh && \
    chmod +x /docker-entrypoint.sh

EXPOSE 80
ENTRYPOINT ["/docker-entrypoint.sh"]
