# Guia de Reversão Segura na Branch Develop

## 📋 Objetivo

Evitar commits acidentais na branch `develop` e garantir que o fluxo de trabalho permaneça seguro.

---

## 🔹 Cenário 1: Descartar todas as mudanças

Se você não precisa das alterações:

```bash
git reset --hard
git clean -fd
```

- `reset --hard`: volta ao último commit da branch.
- `clean -fd`: remove arquivos não rastreados.

## 🔹 Cenário 2: Apenas desestagiar os arquivos

Se você quer manter as mudanças no diretório mas não no stage:

```bash
git reset

```

- Remove os arquivos da área de stage.
- Mantém as alterações para revisão.

## 🔹 Cenário 3: Salvar alterações em outra branch

Se você quer preservar o trabalho mas não na `develop`:

```bash
git checkout -b chore/retry-fix
git checkout develop
git reset --hard origin/develop
```

- Cria uma branch temporária com as mudanças.
- Volta a develop limpa.

## ✅ Resultado esperado

- Cenário 1: Branch limpa, sem alterações.
- Cenário 2: Alterações mantidas, mas não preparadas para commit.
- Cenário 3: Alterações salvas em branch separada, `develop` protegida.

## 📂 Conclusão

Seguindo este guia, você evita commits acidentais em `develop` e mantém o fluxo de trabalho seguro.
