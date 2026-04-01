# ---------- BUILD STAGE ----------
FROM node:22-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .

RUN npm run build


# ---------- OUTPUT ----------
# Static files mounted by reverse-proxy
FROM scratch AS output
COPY --from=builder /app/dist/index.html /index.html
COPY --from=builder /app/dist/assets /assets