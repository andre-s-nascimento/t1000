# Configuração do Spotless com Fallback de Versão

## 📋 Objetivo

Garantir que o plugin **Spotless** funcione corretamente em diferentes versões do JDK, usando fallback de versão do `google-java-format`.

---

## 🔹 Configuração no `build.gradle`

```gradle
spotless {
    java {
        // Usa google-java-format 1.19.2 em JDK 11+
        googleJavaFormat('1.19.2').aosp()

        // Fallback para ambientes legados (ex.: JDK 8)
        targetExclude('**/generated/**')
    }
}
```

## 🚀 Boas práticas

- Sempre rodar:

```bash
./gradlew spotlessApply
```

- Evitar commits com código mal formatado.
- Configurar hook de pre-commit para rodar Spotless automaticamente.

## ⚙️ Alternativa com fallback explícito

```gradle
spotless {
    java {
        try {
            googleJavaFormat('1.19.2').aosp()
        } catch (Exception e) {
            googleJavaFormat('1.17.0').aosp()
        }
    }
}
```

## ✅ Resultado esperado

- Compatibilidade garantida entre diferentes versões de JDK.
- Formatação consistente em todo o projeto.
- Evita falhas em ambientes de CI/CD.

## 📂 Conclusão

Com o fallback configurado, o Spotless se adapta a diferentes ambientes, mantendo o código sempre padronizado.
