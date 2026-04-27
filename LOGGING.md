# Guia de Logging Estruturado

Este documento define as práticas recomendadas para logs no projeto **tmill**.

## 🎯 Princípios

1. **Preservar stacktrace**
   - Sempre usar `log.error("mensagem", ex)` em vez de apenas `log.error("mensagem")`.

2. **Adicionar contexto útil**
   - Incluir IDs e parâmetros relevantes (`fileId`, `movieId`, `chatId`, `blogId`, etc.).
   - Exemplo:

     ```java
     log.error("Erro ao processar áudio fileId={} chatId={}", fileId, chatId, ex);
     ```

3. **Nível de log adequado**
   - `info`: eventos normais (início de pipeline, requisição recebida).
   - `warn`: situações inesperadas mas não críticas (ex.: filme não encontrado).
   - `error`: falhas que interrompem o fluxo (ex.: exceção em integração externa).

4. **Remover prints soltos**
   - Nunca usar `System.out.println`. Substituir por logs estruturados.

5. **Mensagens consistentes**
   - Padronizar mensagens para facilitar busca e análise.
   - Exemplo: `"Erro ao publicar no Blogger blogId={} title={}"`.

6. **Logs de sucesso**
   - Além de erros, registrar eventos importantes de sucesso (`Post criado`, `Transcrição concluída`).

---

## ✅ Checklist de aplicação

- [ ] Nenhum `System.out.println` restante.
- [ ] Todos os `log.error` preservam stacktrace.
- [ ] Logs incluem contexto útil.
- [ ] Níveis de log usados corretamente.
- [ ] Mensagens padronizadas.
