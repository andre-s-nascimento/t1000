# 🎬 Tmill Bot

Bot inteligente para Telegram que busca filmes no TMDB e transcreve áudios com IA

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen)
![Build](https://img.shields.io/badge/build-passing-success)
![Tests](https://img.shields.io/badge/tests-coming_soon-yellow)
![License](https://img.shields.io/badge/license-MIT-blue)
![Status](https://img.shields.io/badge/status-v1.0.0-blueviolet)
![CI](https://github.com/andre-s-nascimento/t1000/actions/workflows/ci.yml/badge.svg)

---

## 🚀 Sobre o Projeto

O **Tmill Bot** é um bot para Telegram que combina:

- 🎥 Busca de filmes via API do TMDB
- 🎙️ Transcrição de áudio com IA via Groq
- 🧠 Refinamento de texto com modelos LLM
- ⚙️ Backend moderno com Spring Boot + Java 21 (Virtual Threads)

---

## ✨ Funcionalidades

### 🔎 Busca de Filmes

- Buscar filmes por nome
- Exibir detalhes:
  - título
  - ano
  - sinopse
  - elenco
  - onde assistir

### 🎧 Transcrição de Áudio

- Recebe áudio via Telegram
- Converte com FFmpeg
- Transcreve com IA
- Refina o texto automaticamente

---

## 🧠 Arquitetura

Telegram → Controller → Services → Clients → APIs externas

📌 Documentação detalhada da arquitetura está disponível em:  
[🏗️ Arquitetura de Pacotes](./docs/dev-guide/arquitetura.md)

---

## 🏗️ Tecnologias

- Java 21
- Spring Boot 3
- Telegram Bots API
- TMDB API
- Groq (Whisper + LLM)
- FFmpeg
- JUnit 5

---

## ⚙️ Como rodar o projeto

### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/tmill-bot.git
cd tmill-bot
```

### 2. Configure variáveis de ambiente

```bash
TELEGRAM_BOT_TOKEN=seu_token
TMDB_API_KEY=sua_chave
GROQ_API_KEY=sua_chave
```

### 3. Execute

```bash
./gradlew bootRun
```

## 🗺️ Roadmap

- [ ] v1.1.0 — Stability & Hardening
- [ ] v1.2.0 — Performance & Testing
- [ ] v1.3.0 — UX Improvements
- [ ] v1.4.0 — DevOps
- [ ] v2.0.0 — Scalability

## 📦 Estrutura

```bash
src/main/java
├── cache
├── client
├── config
├── controller
├── dto
├── exception
├── model
├── service
└── telegram
    ├── core
    ├── exception
    └── util
```

## 📚 Documentação de Desenvolvimento

Toda a documentação técnica e guias estão centralizados em:  
[Dev Guide](./docs/dev-guide/README.md)

Inclui:

- Guia de Logging Estruturado
- Guia de Arquitetura de Pacotes
- Guia de Rebase, PRs e Proteção da Branch Develop
- Configuração do Spotless
- Reversão Segura na Branch Develop

## 📄 Licença

MIT

## 👨‍💻 Autor

Andre Nascimento
