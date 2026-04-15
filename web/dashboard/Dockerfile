FROM node:20-alpine AS base

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

RUN npm run build

FROM base AS runner

WORKDIR /app

ENV NODE_ENV=production \
    NEXT_PUBLIC_API_URL=http://localhost:3001 \
    NEXT_PUBLIC_API_BASE_URL=http://localhost:8080

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

COPY --from=dist /app/.next ./.next
COPY --from=dist /app/public ./public
COPY --from=dist /app/package.json ./package.json

USER nextjs

EXPOSE 3000

ENV PORT=3000

CMD ["npm", "start"]