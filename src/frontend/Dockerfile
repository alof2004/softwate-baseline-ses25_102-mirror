FROM node:20.19-alpine
WORKDIR /app

COPY --chown=node:node package*.json ./
RUN npm ci

COPY --chown=node:node . .

EXPOSE 5173
USER node
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "5173"]
