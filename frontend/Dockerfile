# -------- BUILD STAGE --------
  FROM node:24-alpine AS builder
  
  WORKDIR /app
  
  # install deps
  COPY package*.json ./
  RUN npm ci --legacy-peer-deps
  
  # build-time variables (Vite)
  ARG VITE_API_BASE_URL
  ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
  
  # copy source and build
  COPY . .
  RUN npm run build
  
  
  # -------- PRODUCTION STAGE --------
  FROM nginx:alpine
  
  # clear default nginx html
  RUN rm -rf /usr/share/nginx/html/*
  
  # copy build output
  COPY --from=builder /app/dist /usr/share/nginx/html
  
  # SPA config
  COPY nginx.conf /etc/nginx/conf.d/default.conf
  
  EXPOSE 80
  
  CMD ["nginx", "-g", "daemon off;"]