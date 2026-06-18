# -------- BUILD STAGE --------
FROM node:24-alpine AS builder

WORKDIR /app

# install deps
COPY package*.json ./
RUN npm ci --legacy-peer-deps

# build-time variables (e.g. Vite)
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# copy and build
COPY . .
RUN npm run build


# -------- PRODUCTION STAGE --------
FROM nginx:alpine AS production

WORKDIR /usr/share/nginx/html

# copy static build from builder
COPY --from=builder /app/dist /usr/share/nginx/html

# Replace default nginx config with SPA-friendly config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]