# Guia de Proteção da Branch Develop

## 📋 Objetivo

Garantir que a branch `develop` esteja protegida contra commits diretos e mantenha a qualidade do código.

---

## 🔹 Configuração no GitHub

1. Vá até **Settings > Branches** no repositório.
2. Em **Branch protection rules**, adicione uma regra para `develop`.
3. Configure as opções:
   - [x] Exigir PR antes de merge
   - [x] Exigir aprovação de revisores
   - [x] Exigir que o branch esteja atualizado com `main`
   - [x] Bloquear commits diretos
   - [x] Exigir status checks (CI/CD) antes de merge

---

## 🚀 Boas práticas

- Nunca faça commit direto em `develop`.
- Sempre crie uma branch a partir de `develop` para novas features ou correções.
- Abra PRs para mergear de volta em `develop`.
- Use revisores obrigatórios para garantir qualidade.

---

## ⚙️ Automação

- Configure pipelines de CI/CD para rodar testes automaticamente em PRs.
- Adicione ferramentas de lint e formatadores (ex.: Spotless, ESLint).
- Use hooks de pre-commit para evitar código mal formatado.

---

## ✅ Resultado esperado

- Branch `develop` sempre estável.
- Fluxo de trabalho disciplinado.
- Qualidade e consistência garantidas.

---

## 📂 Conclusão

Proteger a branch `develop` é essencial para evitar problemas de integração e manter o repositório saudável.
