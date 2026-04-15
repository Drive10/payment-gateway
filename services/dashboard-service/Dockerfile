FROM node:20-alpine AS base

RUN apk add --no-cache dumb-init

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --only=production

COPY dist ./dist

USER node

EXPOSE 3001

ENV NODE_ENV=production \
    PORT=3001

ENTRYPOINT ["dumb-init", "--"]

CMD ["node", "dist/main.js"]