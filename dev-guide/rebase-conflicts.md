# Guia de Resolução de Conflitos em Rebase

## 🔹 Objetivo

Orientar o desenvolvedor a resolver conflitos durante um `git rebase`.

---

## 🚀 Passo a passo

1. **Inicie o rebase**:

   ```bash
   git rebase develop
   ```

2. **Identifique os conflitos**:
   - O Git pausa o rebase e mostra os arquivos em conflito.
   - Edite os arquivos e resolva os conflitos manualmente.

3. **Marque como resolvido**:

   ```bash
   git add <arquivo>
   ```

4. **Continue o rebase**:

   ```bash
   git rebase --continue
   ```

5. **Se necessário, aborte**:

   ```bash
   git rebase --abort
   ```

   - Volta ao estado anterior ao rebase.

6. **Pular um commit problemático**:

   ```bash
   git rebase --skip
   ```

## ✅ Resultado

- Conflitos resolvidos.
- Histórico linear mantido.
- Branch atualizada com segurança.

## 📂 Conclusão

O rebase é poderoso, mas exige atenção. Resolva conflitos com calma e use `--abort` ou `--skip` quando necessário.
