FROM node:22-alpine

WORKDIR /app

COPY package.json ./
COPY docs ./docs
COPY server ./server
COPY scripts ./scripts

ENV API_MOCK_PORT=8081
ENV QUOTE_INTERVAL_MS=1000
ENV MOCK_JWT=mock-access-token

EXPOSE 8081

CMD ["npm", "start"]
