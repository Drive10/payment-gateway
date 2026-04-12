#!/bin/bash
# update-dockerfiles.sh - Update Dockerfiles for independent builds

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

echo "Updating Dockerfiles for independent builds..."

# Services to update
SERVICES=(
    "api-gateway"
    "auth-service"
    "order-service"
    "payment-service"
    "notification-service"
    "analytics-service"
    "audit-service"
    "simulator-service"
)

for service in "${SERVICES[@]}"; do
    echo "Processing $service..."
    
    service_dir="$ROOT_DIR/services/$service"
    if [ ! -d "$service_dir" ]; then
        echo "Warning: $service_dir not found, skipping"
        continue
    fi
    
    # Check if Dockerfile exists
    if [ -f "$service_dir/Dockerfile" ]; then
        # Backup original
        cp "$service_dir/Dockerfile" "$service_dir/Dockerfile.monorepo"
    fi
    
    # Create new independent Dockerfile
    cat > "$service_dir/Dockerfile" << 'DOCKEREOF'
# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy only service files
COPY pom.xml .
COPY src ./src
COPY prisma ./prisma

# Build
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
DOCKEREOF
    
    echo "Updated Dockerfile for $service"
done

# Update payment-page Dockerfile
if [ -f "$ROOT_DIR/web/payment-page/Dockerfile" ]; then
    cp "$ROOT_DIR/web/payment-page/Dockerfile" "$ROOT_DIR/web/payment-page/Dockerfile.monorepo"
fi

# Update dashboard Dockerfile
if [ -f "$ROOT_DIR/web/dashboard/Dockerfile" ]; then
    cp "$ROOT_DIR/web/dashboard/Dockerfile" "$ROOT_DIR/web/dashboard/Dockerfile.monorepo"
    # Create standalone dashboard Dockerfile
    cat > "$ROOT_DIR/web/dashboard/Dockerfile" << 'DOCKEREOF'
FROM node:20-alpine AS build
WORKDIR /app

# Copy package files
COPY package.json ./
COPY . .

# Install dependencies
RUN npm ci

# Build
RUN npm run build

# Production stage
FROM node:20-alpine
WORKDIR /app

# Copy built files
COPY --from=build /app/.next ./.next
COPY --from=build /app/package.json ./
COPY --from=build /app/node_modules ./node_modules

ENV NODE_ENV=production
EXPOSE 3000
CMD ["npm", "start"]
DOCKEREOF
fi

echo "Done updating Dockerfiles"