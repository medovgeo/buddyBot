
#### Описание 
Бот читает чат в telegram и язвительно отвечает на упоминания, реплаи боту, и рандомные сообщения. 

История сообщений хранится в Mongo (кроме ответов бота, они не сохраняются)

Ответы бота генерируются в Gemini, куда передаётся история чата на каждый запрос.    


#### Локальный запуск
- задать переменные окружения в .env файле
```
# app.env
BOT_TOKEN=your_bot_token
BOT_NAME=your_bot_name
MONGO_URI=mongodb://user:password@mongo:27017 \
GEMINI_MODEL=gemini-1.5-pro
GEMINI_TOKEN=your_gemini_token
```

#### Запуск через dockerfile 
```bash
docker build -t buddy_bot .
```

```bash
docker run -d \
--name buddy-bot-app \
--network mynetwork \ 
-e BOT_TOKEN=your_bot_token \
-e BOT_NAME=your_bot_name \
-e MONGO_URI=mongodb://user:password@mongo:27017 \
-e GEMINI_MODEL=gemini-1.5-pro \
-e GEMINI_TOKEN=your_gemini_token \
buddy_bot
```

#### Git push в amvera
```bash
git push amvera main:master
```

