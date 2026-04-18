# 🎬 Tmill Bot

Bot para Telegram desenvolvido em Java com Spring Boot que permite:

* 🔎 Buscar informações de filmes via TMDB
* 🎙️ Transcrever áudios em português usando IA (Whisper)
* ✨ Refinar texto automaticamente com LLM

---

## 🚀 Tecnologias

* Java 21
* Spring Boot 4
* Telegram Bots API
* TMDB API
* Groq API (Whisper + Llama)
* FFmpeg
* Virtual Threads (Project Loom)

---

## 🧠 Arquitetura

O projeto segue uma arquitetura em camadas:

```
Controller → Service → Client → API externa
```

Além disso, possui um pipeline assíncrono para áudio:

```
Áudio → Conversão → Transcrição → Refinamento
```

---

## ⚙️ Configuração

Crie as variáveis de ambiente:

```
TELEGRAM_BOT_TOKEN=seu_token
TMDB_READ_TOKEN=seu_token
GROQ_API_KEY=seu_token
```

---

## ▶️ Executando o projeto

```bash
./gradlew bootRun
```

---

## 💬 Como usar

No Telegram:

### Buscar filme

```
t1000 buscar matrix
```

### Enviar áudio

* Envie um áudio em português
* O bot irá transcrever e refinar automaticamente

---

## 🧪 Status do projeto

Versão: **1.0.0**

✔ Funcional
✔ Pipeline de áudio completo
✔ Integração com APIs externas

---

## ⚠️ Limitações atuais

* Sem cache de respostas
* Sem rate limiting
* Processamento de áudio sem fila
* Testes automatizados mínimos

---

## 🚀 Próximos passos

* Cache (Redis ou Caffeine)
* Rate limiting
* Fila de processamento de áudio
* Observabilidade (metrics + logs)
* Clean Architecture

---

## 📚 Aprendizados

Este projeto explora:

* Integração com APIs externas
* Processamento assíncrono
* Virtual Threads
* Pipeline de dados com IA
* Arquitetura em camadas

---

## 📍 Roadmap

- [ ] v1.1.0 - Stability
- [ ] v1.2.0 - Performance
- [ ] v1.3.0 - UX
- [ ] v1.4.0 - DevOps
- [ ] v2.0.0 - Scalability

---

## 👨‍💻 Autor

Andre Nascimento
