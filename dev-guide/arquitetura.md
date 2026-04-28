# 🏗️ Arquitetura de Pacotes - Tmill

Este documento faz parte do **Dev Guide** e descreve a organização dos pacotes e dependências principais da aplicação **Tmill**.

---

## 📁 Estrutura Geral

```bash
src/main/java/net/ddns/adambravo79/tmill
├── BotRunner.java
├── TmillApplication.java
├── cache
│   └── TranscricaoCache.java
├── client
│   ├── BloggerClient.java
│   ├── GroqClient.java
│   └── TmdbClient.java
├── config
│   ├── AppConfig.java
│   ├── GlobalExceptionHandler.java
│   └── TelegramConfig.java
├── controller
│   └── TelegramController.java
├── dto
│   └── ErrorResponse.java
├── exception
│   ├── AudioProcessingException.java
│   ├── BloggerPublishException.java
│   ├── MovieNotFoundException.java
│   └── TelegramFileException.java
├── model
│   ├── CastRecord.java
│   ├── ChatCompletionResponse.java
│   ├── Choice.java
│   ├── CountryProviders.java
│   ├── CreditsResponse.java
│   ├── Message.java
│   ├── MovieOrchestrationResponse.java
│   ├── MovieRecord.java
│   ├── MovieResponse.java
│   ├── MovieSearchResponse.java
│   ├── Provider.java
│   ├── TranscriptionResponse.java
│   └── WatchProviderResponse.java
├── service
│   ├── AudioPipelineService.java
│   ├── AudioService.java
│   ├── MovieService.java
│   └── TelegramFileService.java
└── telegram
    ├── core
    │   ├── TelegramAction.java
    │   ├── TelegramFacade.java
    │   ├── TelegramSafeExecutor.java
    │   └── TelegramSender.java
    ├── exception
    │   ├── TelegramExceptionHandler.java
    │   └── TelegramFileException.java
    └── util
        ├── MetricsService.java
        └── RetryPolicy.java
```

## 🔄 Fluxo de Dependências

`controller → service → client/model → telegram`

- controller: recebe eventos do Telegram e delega para os serviços.
- service: contém a lógica de negócio (ex.: processamento de áudio, filmes).
- client: integra com APIs externas (TMDB, Groq, Blogger).
- model: define os objetos de domínio e respostas.
- telegram: abstrai a comunicação com a API do Telegram.

## 📦 Pacotes e Responsabilidades

- cache: armazenamento em memória de transcrições.
- client: integrações externas (APIs de terceiros).
- config: configuração da aplicação e beans globais.
- controller: camada de entrada, responsável por interações com o Telegram.
- dto: objetos de transporte específicos (ex.: respostas de erro).
- exception: exceções customizadas da aplicação.
- model: representações de dados e respostas de APIs externas.
- service: lógica de negócio e orquestração.
- telegram:
  - core: abstrações principais de envio/recebimento.
  - exception: tratamento de erros específicos do Telegram.
  - util: utilitários de suporte (métricas, retry).

## 🧪 Testes

A estrutura de testes em `src/test/java` espelha a hierarquia de main, garantindo cobertura consistente:

- Testes unitários para cada pacote.
- Testes de integração em `integration`.

## ✅ Conclusão

A organização atual está correta, modular e escalável, permitindo evolução futura sem comprometer a clareza.
