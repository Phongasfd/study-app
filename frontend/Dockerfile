# -------- BUILD STAGE --------
  FROM node:24-alpine AS builder
  
  WORKDIR /app
  
  COPY package*.json ./
  RUN npm ci --legacy-peer-deps
  
  ARG VITE_API_BASE_URL
  ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
  
  COPY . .
  RUN npm run build
  