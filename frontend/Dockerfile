# -------- BUILD STAGE --------
  FROM node:24-alpine AS builder
  
  WORKDIR /app
  
  COPY package*.json ./
  RUN npm ci --legacy-peer-deps
  
  ARG VITE_API_BASE_URL
  ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
  
  COPY . .
  RUN npm run build
  
  
  # -------- RUNTIME STAGE (NO NGINX CONFIG) --------
  FROM nginx:alpine
  
  # copy build only
  COPY --from=builder /app/dist /usr/share/nginx/html
  
  # optional: remove default config (safe)
  RUN rm -f /etc/nginx/conf.d/default.conf
  
  EXPOSE 80
  
  CMD ["nginx", "-g", "daemon off;"]