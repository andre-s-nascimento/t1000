# Proteção da Branch Develop com Rulesets

## 📋 Objetivo

Utilizar os **Rulesets do GitHub** para proteger a branch `develop` com políticas modernas e flexíveis.

---

## 🔹 Configuração de Rulesets

1. Vá até **Settings > Rulesets** no repositório.
2. Crie um novo ruleset para a branch `develop`.
3. Configure as regras:
   - [x] Exigir Pull Requests para merge
   - [x] Exigir revisores obrigatórios
   - [x] Exigir histórico linear (sem merges fora de ordem)
   - [x] Bloquear commits diretos
   - [x] Exigir commits assinados (GPG ou SSH)
   - [x] Exigir status checks (CI/CD) antes de merge
   - [x] Restringir quem pode aprovar ou mergear

---

## 🚀 Benefícios dos Rulesets

- Mais flexibilidade que branch protection rules tradicionais.
- Permite aplicar políticas diferentes para múltiplas branches.
- Suporte a condições avançadas (ex.: obrigar testes específicos).
- Melhor integração com pipelines e automações.

---

## ⚙️ Exemplos de uso

- Exigir que PRs tenham pelo menos 2 aprovações.
- Bloquear merges se cobertura de testes cair abaixo de 80%.
- Permitir merges apenas para usuários de um grupo específico.

---

## ✅ Resultado esperado

- Branch `develop` protegida com regras modernas.
- Fluxo de trabalho mais seguro e disciplinado.
- Maior confiabilidade nas entregas.

---

## 📂 Conclusão

Rulesets são a forma mais atual e robusta de proteger branches no GitHub. Configure-os para `develop` e garanta qualidade contínua.
